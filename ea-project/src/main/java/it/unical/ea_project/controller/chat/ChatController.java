package it.unical.ea_project.controller.chat;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.unical.ea_project.event.chat.ChatEvents;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.chat.ChatService;
import it.unical.ea_project.service.security.JwtService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    public record Open(Long organizerId, Long travelerId) {
    }

    public record Send(String clientId, String body, Long replyToId) {
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
        if ("ORGANIZER".equals(user.role()) && request.travelerId() != null)
            return service.openTraveler(user.id(), request.travelerId());
        if (request.organizerId() == null)
            throw new IllegalArgumentException("Organizzatore obbligatorio.");
        return service.open(user.id(), request.organizerId());
    }

    @GetMapping("/conversations/{id}")
    public ChatService.ConversationView conversation(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.conversation(id, user.id());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<ChatService.MessageView> history(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long before) {
        return service.history(id, user.id(), before);
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatService.MessageView send(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
            @RequestBody Send request) {
        return service.send(id, user.id(), request.clientId(), request.body(), request.replyToId(), null);
    }

    @PostMapping(value = "/conversations/{id}/photos", consumes = "multipart/form-data")
    public ChatService.MessageView photo(@PathVariable Long id, @AuthenticationPrincipal AuthUser user,
            @RequestParam String clientId, @RequestParam(defaultValue = "") String body,
            @RequestParam(required = false) Long replyToId, @RequestParam MultipartFile file) {
        return service.send(id, user.id(), clientId, body, replyToId, file);
    }

    @GetMapping("/conversations/{id}/messages/{message}/photo")
    public ResponseEntity<Map<String, String>> photo(@PathVariable Long id, @PathVariable Long message,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(Map.of("data", service.photo(id, user.id(), message)));
    }

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        service.delete(id, user.id());
    }

    @PostMapping("/conversations/{id}/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        service.readAll(id, user.id());
    }

    @PostMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id, @AuthenticationPrincipal AuthUser user, @RequestBody Read request) {
        service.read(id, user.id(), request.throughId());
    }

    @GetMapping(value = "/events", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> stream(@AuthenticationPrincipal AuthUser user,
            @RequestHeader("Authorization") String authorization) {
        var claims = jwt.parseToken(authorization.substring(7), JwtService.TYPE_ACCESS);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .header("X-Accel-Buffering", "no")
                .body(events.connect(user.id(), claims.getExpiration().toInstant().plusSeconds(30),
                        ((Number) claims.get("ver")).longValue()));
    }
}
