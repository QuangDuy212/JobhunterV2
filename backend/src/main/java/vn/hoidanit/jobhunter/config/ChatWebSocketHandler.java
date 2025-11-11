package vn.hoidanit.jobhunter.config;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

import vn.hoidanit.jobhunter.service.GeminiService;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final GeminiService geminiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatWebSocketHandler(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        
        JsonNode rootNode = mapper.readTree(message.getPayload());
        JsonNode messageNode = rootNode.get("message");

        String userMessage;

        if (messageNode != null && messageNode.isTextual()) {
            userMessage = messageNode.asText();
        } else {
            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of("message", "Lỗi: Vui lòng gửi JSON có trường 'message'."))));
            return; 
        }

        geminiService.chatWithGemini(userMessage)
            .flatMap(botReply -> {
                try {
                    Map<String, String> response = Map.of("message", botReply);
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
                } catch (Exception e) {
                    System.err.println("Lỗi gửi tin nhắn WS: " + e.getMessage());
                }
                return Mono.empty();
            })
            .doOnError(error -> {
                System.err.println("Lỗi trong luồng Reactive: " + error.getMessage());
            })
            .subscribe();
    }
}