package it.unical.ea_project.chat;

import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.JwtService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {
    private final ChatService service;
    private final ChatEvents events;
    private final JwtService jwt;

    public ChatController(ChatService s, ChatEvents e, JwtService j) {
        service = s;
        events = e;
        jwt = j;
    }

    public record Open(Long organizerId) {
    }

    public record Send(String clientId, String body) {
    }

    public record Read(Long throughId) {
    }

    @GetMapping("/conversations")
    public List<ChatService.ConversationView> inbox(@AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page) {
        return service.inbox(user.id(), page);
    }

    @PostMapping("/conversations")
    public ChatService.ConversationView open(@AuthenticationPrincipal AuthUser user, @RequestBody Open request) {
        if (request.organizerId() == null)
            throw new IllegalArgumentException("Organizzatore obbligatorio.");
        return service.open(user.id(), request.organizerId());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatService.MessageView> history(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long before) {
        return service.history(id, user.id(), before);
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatService.MessageView send(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
            @RequestBody Send request) {
        return service.send(id, user.id(), request.clientId(), request.body());
    }

    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id, @AuthenticationPrincipal AuthUser user, @RequestBody Read request) {
        service.read(id, user.id(), request.throughId());
    }

    @GetMapping(value = "/events", produces = "text/event-stream")
    public org.springframework.http.ResponseEntity<SseEmitter> stream(@AuthenticationPrincipal AuthUser user,
            @RequestHeader("Authorization") String authorization) {
        var claims = jwt.parseToken(authorization.substring(7), JwtService.TYPE_ACCESS);
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control", "no-store")
                .header("X-Accel-Buffering", "no")
                .body(events.connect(user.id(), claims.getExpiration().toInstant().plusSeconds(30),
                        ((Number) claims.get("ver")).longValue()));
    }
}
