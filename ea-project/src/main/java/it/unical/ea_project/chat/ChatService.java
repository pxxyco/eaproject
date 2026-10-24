package it.unical.ea_project.chat;

import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ChatService {
    public record ConversationView(Long id, Long otherUserId, String name, String role, String preview,
            Instant updatedAt, long unread) {
    }

    public record MessageView(Long id, Long senderId, String clientId, String body, Instant sentAt) {
    }

    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public ChatService(ConversationRepository c, ChatMessageRepository m, UserRepository u,
            ApplicationEventPublisher e) {
        conversations = c;
        messages = m;
        users = u;
        events = e;
    }

    public List<ConversationView> inbox(Long user, int page) {
        var pageRows = conversations.inbox(user, PageRequest.of(Math.max(0, Math.min(page, 10000)), 30));
        if (pageRows.isEmpty())
            return List.of();
        Map<Long, Long> unread = new HashMap<>();
        messages.unread(user, pageRows.stream().map(Conversation::getId).toList())
                .forEach(r -> unread.put((Long) r[0], (Long) r[1]));
        return pageRows.stream().map(c -> view(c, user, unread.getOrDefault(c.getId(), 0L))).toList();
    }

    @Transactional
    public ConversationView open(Long travelerId, Long organizerId) {
        User traveler = active(travelerId), organizer = active(organizerId);
        if (traveler.getRole() != User.Role.TRAVELER || organizer.getRole() != User.Role.ORGANIZER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La chat collega un viaggiatore a un organizzatore.");
        conversations.ensure(travelerId, organizerId);
        return view(conversations.findByTravelerIdAndOrganizerId(travelerId, organizerId).orElseThrow(), travelerId, 0);
    }

    public List<MessageView> history(Long conversation, Long user, Long before) {
        access(conversation, user, false);
        return messages.history(conversation, before, PageRequest.of(0, 50)).stream().map(this::view).toList();
    }

    @Transactional
    public MessageView send(Long conversation, Long user, String clientId, String body) {
        var c = access(conversation, user, true);
        String normalized = body == null ? "" : body.strip();
        if (normalized.isBlank() || normalized.length() > 4000
                || normalized.codePoints().anyMatch(ch -> Character.isISOControl(ch) && ch != '\n' && ch != '\t'))
            throw new IllegalArgumentException("Il messaggio deve contenere da 1 a 4000 caratteri validi.");
        try {
            clientId = UUID.fromString(clientId).toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Identificatore del messaggio non valido.");
        }
        var previous = messages.findByConversationIdAndSenderIdAndClientId(conversation, user, clientId);
        if (previous.isPresent()) {
            if (!previous.get().getBody().equals(normalized))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Identificatore già usato per un altro messaggio.");
            return view(previous.get());
        }
        ChatMessage message = new ChatMessage();
        message.setConversation(c);
        message.setSenderId(user);
        message.setClientId(clientId);
        message.setBody(normalized);
        message = messages.saveAndFlush(message);
        c.setUpdatedAt(message.getSentAt());
        c.setPreview(normalized.substring(0,
                normalized.offsetByCodePoints(0, Math.min(160, normalized.codePointCount(0, normalized.length())))));
        events.publishEvent(new ChatEvents.Changed(c.getId(), c.getTraveler().getId(), c.getOrganizer().getId()));
        return view(message);
    }

    @Transactional
    public void read(Long conversation, Long user, Long through) {
        var c = access(conversation, user, true);
        if (through == null || !messages.existsByIdAndConversationId(through, conversation))
            throw new IllegalArgumentException("Messaggio non trovato in questa conversazione.");
        if (c.getTraveler().getId().equals(user))
            c.setTravelerReadId(Math.max(c.getTravelerReadId() == null ? 0 : c.getTravelerReadId(), through));
        else
            c.setOrganizerReadId(Math.max(c.getOrganizerReadId() == null ? 0 : c.getOrganizerReadId(), through));
    }

    private User active(Long id) {
        return users.findById(id).filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non disponibile."));
    }

    private Conversation access(Long id, Long user, boolean lock) {
        var c = (lock ? conversations.lock(id) : conversations.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversazione non disponibile."));
        if (!c.getTraveler().getId().equals(user) && !c.getOrganizer().getId().equals(user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversazione privata.");
        if (c.getTraveler().getDeletedAt() != null || c.getOrganizer().getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.GONE, "Un partecipante non è più disponibile.");
        return c;
    }

    private ConversationView view(Conversation c, Long user, long unread) {
        User other = c.getTraveler().getId().equals(user) ? c.getOrganizer() : c.getTraveler();
        return new ConversationView(c.getId(), other.getId(), other.getUsername(), other.getRole().name(),
                c.getPreview(), c.getUpdatedAt(), unread);
    }

    private MessageView view(ChatMessage m) {
        return new MessageView(m.getId(), m.getSenderId(), m.getClientId(), m.getBody(), m.getSentAt());
    }
}
