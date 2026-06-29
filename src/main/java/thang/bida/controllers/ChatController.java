package thang.bida.controllers;

import thang.bida.dto.ChatRequest;
import thang.bida.dto.ChatResponse;
import thang.bida.services.ChatService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    @org.springframework.security.access.prepost.PreAuthorize("permitAll()")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        String reply = chatService.reply(
                request.getMessage(),
                request.getTableCode());
        return new ChatResponse(reply);
    }
}
