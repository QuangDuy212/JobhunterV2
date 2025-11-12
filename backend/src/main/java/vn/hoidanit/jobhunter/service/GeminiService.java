package vn.hoidanit.jobhunter.service;

import java.util.List;
import java.util.Map;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper(); 
    
    // Định nghĩa câu hỏi rõ ràng cho AI để nhận phản hồi có thể phân tích được
    private static final String SAFETY_PROMPT_FORMAT = "Review this Job Description for professionalism, safety, and suitability for a public job board. Respond ONLY with either 'APPROVED' or 'REJECTED: [brief reason in Vietnamese]'.\n\nJob Description:\n%s";
    
    // Khởi tạo WebClient và truyền Key qua Header X-Goog-Api-Key
    public GeminiService(@Value("${gemini.api.key}") String geminiApiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("X-Goog-Api-Key", geminiApiKey) 
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 1. HÀM KIỂM TRA NỘI DUNG AN TOÀN (Sử dụng đồng bộ blocking)
     * PHƯƠNG PHÁP CẬP NHẬT: Gửi prompt yêu cầu phản hồi 'APPROVED' hoặc 'REJECTED' và phân tích chuỗi kết quả.
     * @param jobDescription Nội dung mô tả công việc cần kiểm duyệt.
     * @return true nếu AI quyết định an toàn (chuỗi bắt đầu bằng 'APPROVED'), false nếu không an toàn hoặc lỗi API.
     */
    public boolean checkContentSafety(String jobDescription) {
        String url = "/v1beta/models/gemini-2.5-flash:generateContent";
        
        // Tạo câu hỏi cụ thể cho AI, yêu cầu phản hồi có thể phân tích được
        String fullPrompt = String.format(SAFETY_PROMPT_FORMAT, jobDescription);
        
        // Cấu hình Request cho phản hồi văn bản thông thường
        Map<String, Object> bodyMap = Map.of(
            "contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(
                        Map.of("text", fullPrompt)
                    )
                )
            )
        );

        try {
            String jsonBody = mapper.writeValueAsString(bodyMap);

            // Chuyển Mono thành blocking call (sử dụng .block())
            String responseText = webClient.post()
                    .uri(url)
                    .bodyValue(jsonBody)
                    .retrieve()
                    // Xử lý lỗi API (4xx, 5xx)
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> 
                            clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("Google API Error Body (Safety Check): " + errorBody); 
                                    return Mono.error(new RuntimeException("Gemini API Error: " + clientResponse.statusCode() + " Body: " + errorBody));
                                })
                    )
                    // Lấy phản hồi thô của API, cần phải parse để lấy text
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .map(response -> {
                        // Trích xuất văn bản từ cấu trúc phản hồi của API
                        try {
                            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                            if (candidates == null || candidates.isEmpty()) {
                                return "ERROR: No candidates or content blocked.";
                            }
                            Map<String, Object> candidate = candidates.get(0);
                            
                            // Kiểm tra phản hồi cấp độ cao của Gemini (an toàn cứng)
                            if (candidate.containsKey("finishReason") && "SAFETY".equals(candidate.get("finishReason"))) {
                                return "REJECTED: Bị chặn bởi bộ lọc an toàn của Google."; // Chuyển thành chuỗi REJECTED để logic bên dưới xử lý
                            }

                            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                            return parts.get(0).get("text").trim().toUpperCase(); // Trả về text đã chuẩn hóa
                        } catch (Exception e) {
                            System.err.println("Lỗi Parse JSON phản hồi từ Gemini: " + e.getMessage());
                            return "ERROR: Internal parse failure.";
                        }
                    })
                    .block(java.time.Duration.ofSeconds(15)); // Đợi kết quả (block)

            // 2. PHÂN TÍCH KẾT QUẢ DẠNG CHUỖI TỪ AI
            
            // Xử lý null hoặc lỗi
            if (responseText == null || responseText.startsWith("ERROR")) {
                System.err.println("Lỗi Gemini API: Không nhận được phản hồi hoặc lỗi nội bộ. Chuyển sang FAIL-SAFE.");
                return false; // FAIL-SAFE
            }
            
            // Logic kiểm duyệt: Nếu phản hồi bắt đầu bằng "APPROVED", nội dung là an toàn.
            if (responseText.startsWith("APPROVED")) {
                System.out.println("--- JOB CONTENT APPROVED by AI Review ---");
                return true; 
            } else if (responseText.startsWith("REJECTED")) {
                // In ra lý do
                System.out.println("--- JOB CONTENT REJECTED by AI Review: " + responseText.substring(responseText.indexOf(":") + 1) + " ---");
                return false;
            } else {
                // Trường hợp AI trả lời không đúng định dạng "APPROVED" / "REJECTED"
                System.err.println("Phản hồi Gemini không đúng định dạng (APPROVED/REJECTED): " + responseText);
                return false; // FAIL-SAFE
            }

        } 
        catch (RuntimeException e) {
            System.err.println("Lỗi gọi Gemini API (Safety Check) - Runtime Exception: " + e.getMessage());
            // ⭐️ CHIẾN LƯỢC FAIL-SAFE: Nếu API gặp lỗi (timeout, 4xx, 5xx), mặc định là FALSE (KHÔNG an toàn)
            return false; 
        } catch (Exception e) {
            System.err.println("Lỗi nội bộ khi tạo request: " + e.getMessage());
            // Lỗi nội bộ: Mặc định là FALSE
            return false;
        }
    }

    /**
     * Phương thức cho chức năng Chatbot thông thường (Non-structured response) - Dùng Mono (reactive)
     * Logic này KHÔNG thay đổi so với phiên bản trước.
     * @param userMessage Tin nhắn từ người dùng
     * @return Mono<String> Phản hồi từ Gemini
     */
    public Mono<String> chatWithGemini(String userMessage) {
        
        String url = "/v1beta/models/gemini-2.5-flash:generateContent";

        try {
            // Cấu hình Request cho phản hồi văn bản thông thường
            Map<String, Object> bodyMap = Map.of(
                "contents", List.of(
                    Map.of(
                        "role", "user",
                        "parts", List.of(
                            Map.of("text", userMessage)
                        )
                    )
                )
            );
            
            String jsonBody = mapper.writeValueAsString(bodyMap);
            
            System.out.println("DEBUG JSON SENT (Chat): " + jsonBody);
            
            return webClient.post()
                    .uri(url)
                    .bodyValue(jsonBody) 
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> 
                         clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Google API Error Body (Chat): " + errorBody); 
                                return Mono.error(new RuntimeException("Gemini API Error: " + clientResponse.statusCode() + " Body: " + errorBody));
                            })
                    )
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .map(response -> {
                        // Logic parse response giữ nguyên
                        try {
                            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                            if (candidates == null || candidates.isEmpty()) {
                                if (response.containsKey("error")) {
                                    Map<?, ?> error = (Map<?, ?>) response.get("error");
                                    return "Lỗi từ Gemini: " + error.get("message");
                                }
                                return "Không nhận được phản hồi hoặc nội dung bị chặn.";
                            }
                            
                            Map<String, Object> candidate = candidates.get(0);
                            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
        
                            return parts.get(0).get("text"); 
                        } catch (Exception e) {
                            return "Lỗi khi đọc phản hồi từ Gemini!";
                        }
                    })
                    .onErrorResume(e -> {
                        System.err.println("Gemini API Error (Reactive): " + e.getMessage());
                        return Mono.just("Lỗi gọi API: " + e.getMessage());
                    });
            
        } catch (Exception e) {
            System.err.println("Lỗi nội bộ khi tạo request: " + e.getMessage());
            return Mono.just("Lỗi nội bộ khi tạo request!");
        }
    }
}