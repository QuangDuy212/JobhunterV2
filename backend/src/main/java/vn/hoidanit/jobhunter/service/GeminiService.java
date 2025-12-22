package vn.hoidanit.jobhunter.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vn.hoidanit.jobhunter.domain.response.job.SafetyCheckResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    // Prompt mới: Ép AI tìm từ phản cảm và trả về JSON
    private static final String SAFETY_PROMPT_FORMAT = "Bạn là một hệ thống kiểm duyệt nội dung tự động. Nhiệm vụ của bạn là phân tích mô tả công việc (Job Description) dưới đây.\n\n"
            +
            "QUY TẮC KIỂM DUYỆT:\n" +
            "1. Chỉ 'REJECTED' khi nội dung chứa: từ ngữ thô tục, lừa đảo tài chính, cờ bạc, nội dung khiêu dâm, hoặc yêu cầu đặt cọc tiền.\n"
            +
            "2. Các nội dung ngắn hoặc đơn giản như 'không có yêu cầu', 'liên hệ trực tiếp', 'tuyển gấp' được coi là HỢP LỆ ('APPROVED').\n"
            +
            "3. Nếu nội dung an toàn, 'violatedWords' PHẢI là mảng rỗng [].\n\n" +
            "ĐỊNH DẠNG TRẢ VỀ (JSON DUY NHẤT):\n" +
            "{\"status\": \"APPROVED/REJECTED\", \"reason\": \"lý do ngắn gọn\", \"violatedWords\": []}\n\n" +
            "Job Description:\n%s";

    public GeminiService(@Value("${gemini.api.key}") String geminiApiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("X-Goog-Api-Key", geminiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * HÀM KIỂM TRA NỘI DUNG (Đã sửa để trả về danh sách từ phản cảm)
     */
    public SafetyCheckResult checkContentSafety(String jobDescription) {
        // Sử dụng model 1.5-flash (bản 2.5 không tồn tại, có thể bạn gõ nhầm)
        String url = "/v1beta/models/gemini-2.5-flash:generateContent";
        String fullPrompt = String.format(SAFETY_PROMPT_FORMAT, jobDescription);

        Map<String, Object> bodyMap = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json"));

        try {
            String jsonBody = mapper.writeValueAsString(bodyMap);

            return webClient.post()
                    .uri(url)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).flatMap(
                                    errorBody -> Mono.error(new RuntimeException("Gemini API Error: " + errorBody))))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .map(response -> {
                        try {
                            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response
                                    .get("candidates");
                            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            String rawJson = (String) parts.get(0).get("text");

                            // Parse chuỗi JSON AI trả về thành Object SafetyCheckResult
                            return mapper.readValue(rawJson, SafetyCheckResult.class);
                        } catch (Exception e) {
                            return createFallbackResponse("Lỗi phân tích dữ liệu AI.");
                        }
                    })
                    .block(java.time.Duration.ofSeconds(15));
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống Gemini: " + e.getMessage());
            return createFallbackResponse("Không thể kết nối API kiểm duyệt.");
        }
    }

    /**
     * GIỮ NGUYÊN HÀM CHAT CỦA BẠN
     */
    public Mono<String> chatWithGemini(String userMessage) {
        String url = "/v1beta/models/gemini-2.5-flash:generateContent";
        try {
            Map<String, Object> bodyMap = Map.of(
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage)))));
            String jsonBody = mapper.writeValueAsString(bodyMap);

            return webClient.post()
                    .uri(url)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .map(response -> {
                        try {
                            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response
                                    .get("candidates");
                            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                            List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                            return parts.get(0).get("text");
                        } catch (Exception e) {
                            return "Lỗi khi đọc phản hồi từ Gemini!";
                        }
                    })
                    .onErrorResume(e -> Mono.just("Lỗi gọi API: " + e.getMessage()));
        } catch (Exception e) {
            return Mono.just("Lỗi nội bộ khi tạo request!");
        }
    }

    private SafetyCheckResult createFallbackResponse(String message) {
        return SafetyCheckResult.builder()
                .status("ERROR")
                .reason(message)
                .violatedWords(Collections.emptyList())
                .build();
    }
}