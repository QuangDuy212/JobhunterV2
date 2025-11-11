package vn.hoidanit.jobhunter.service;

import java.util.List;
import java.util.Map;

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
    
    // Khởi tạo WebClient và truyền Key qua Header X-Goog-Api-Key
    public GeminiService(@Value("${gemini.api.key}") String geminiApiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("X-Goog-Api-Key", geminiApiKey) 
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<String> chatWithGemini(String userMessage) {
        
        String url = "/v1beta/models/gemini-2.5-flash:generateContent";

        try {
            // 🚨 SỬA LỖI 400: LOẠI BỎ TRƯỜNG "config"
            Map<String, Object> bodyMap = Map.of(
                "contents", List.of(
                    Map.of(
                        "role", "user",
                        "parts", List.of(
                            Map.of("text", userMessage)
                        )
                    )
                )
                // KHÔNG CÒN TRƯỜNG "config" NỮA
            );
            
            String jsonBody = mapper.writeValueAsString(bodyMap);
            
            System.out.println("DEBUG JSON SENT: " + jsonBody);
            
            return webClient.post()
                    .uri(url)
                    .bodyValue(jsonBody) 
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> 
                         clientResponse.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                System.err.println("Google API Error Body: " + errorBody); 
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