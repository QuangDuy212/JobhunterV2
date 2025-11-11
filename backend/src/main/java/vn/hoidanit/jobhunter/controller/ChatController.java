package vn.hoidanit.jobhunter.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.service.GeminiService;

@RestController
@RequestMapping("/ws/chat")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // @MessageMapping("/chat")
    // @SendTo("/topic/messages")
    // public String handleMessage(String message) {
    //     String response = geminiService.chatWithGemini(message);
    //     return response;
    // }
}

