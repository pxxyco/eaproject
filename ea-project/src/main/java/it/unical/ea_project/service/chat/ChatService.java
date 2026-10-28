package it.unical.ea_project.service.chat;

import it.unical.ea_project.domain.account.AccountSettings;
import it.unical.ea_project.domain.chat.ChatMessage;
import it.unical.ea_project.domain.chat.Conversation;
import it.unical.ea_project.domain.notification.Notification;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.event.chat.ChatEvents;
import it.unical.ea_project.repository.account.AccountSettingsRepository;
import it.unical.ea_project.repository.chat.ChatMessageRepository;
import it.unical.ea_project.repository.chat.ConversationRepository;
import it.unical.ea_project.repository.notification.NotificationRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.repository.booking.BookingRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ChatService {
    public record ConversationView(Long id, Long otherUserId, String name, String role, String preview,
            Instant updatedAt, long unread) {
    }

    public record ReplyView(Long id, Long senderId, String body) {
    }

    public record MessageView(Long id, Long senderId, String clientId, String body, Instant sentAt,
            ReplyView replyTo, boolean hasPhoto) {
    }

    private final BookingRepository bookings;
    private final ChatPhotoStorage photos;
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final NotificationRepository notifications;
    private final AccountSettingsRepository preferences;

    public ChatService(ConversationRepository c, ChatMessageRepository m, UserRepository u,
            ApplicationEventPublisher e, NotificationRepository notifications,
            AccountSettingsRepository preferences, ChatPhotoStorage photos, BookingRepository bookings) {
        this.bookings = bookings;
        this.photos = photos;
        conversations = c;
        messages = m;
        users = u;
        events = e;
        this.notifications = notifications;
        this.preferences = preferences;
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
        return openPair(travelerId, organizerId, travelerId);
    }

    private ConversationView openPair(Long travelerId, Long organizerId, Long viewer) {
        User traveler = active(travelerId), organizer = active(organizerId);
        if (traveler.getRole() != User.Role.TRAVELER || organizer.getRole() != User.Role.ORGANIZER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La chat collega un viaggiatore a un organizzatore.");
        users.lockForChat(organizerId).orElseThrow();
        Conversation c = conversations.findByTravelerIdAndOrganizerId(travelerId, organizerId).orElseGet(() -> {
            Conversation created = new Conversation();
            created.setTraveler(traveler);
            created.setOrganizer(organizer);
            return conversations.saveAndFlush(created);
        });
        if (viewer.equals(travelerId))
            c.setTravelerDeletedAt(null);
        else
            c.setOrganizerDeletedAt(null);
        return view(c, viewer, 0);
    }

    @Transactional
    public ConversationView openTraveler(Long organizer, Long traveler) {
        active(organizer);
        active(traveler);
        if (conversations.findByTravelerIdAndOrganizerId(traveler, organizer).isEmpty()
                && bookings.countContactBookings(traveler, organizer) == 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Puoi contattare solo chi ha prenotato una tua esperienza.");
        return openPair(traveler, organizer, organizer);
    }

    public ConversationView conversation(Long id, Long user) {
        return view(access(id, user, false), user, 0);
    }

    public List<MessageView> history(Long conversation, Long user, Long before) {
        Conversation c = access(conversation, user, false);
        return messages.history(conversation, before, cleared(c, user), PageRequest.of(0, 50)).stream()
                .map(m -> view(m, user)).toList();
    }

    @Transactional
    public MessageView send(Long conversation, Long user, String clientId, String body) {
        return send(conversation, user, clientId, body, null, null);
    }

    @Transactional
    public MessageView send(Long conversation, Long user, String clientId, String body, Long replyTo,
            MultipartFile photo) {
        var c = access(conversation, user, true);
        String hash = photo == null ? null : photos.hash(photo);
        String normalized = body == null ? "" : body.strip();
        if ((normalized.isBlank() && photo == null) || normalized.length() > 4000
                || normalized.codePoints().anyMatch(ch -> Character.isISOControl(ch) && ch != '\n' && ch != '\t'))
            throw new IllegalArgumentException("Il messaggio deve contenere da 1 a 4000 caratteri validi.");
        try {
            clientId = UUID.fromString(clientId).toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Identificatore del messaggio non valido.");
        }
        var previous = messages.findByConversationIdAndSenderIdAndClientId(conversation, user, clientId);
        if (previous.isPresent()) {
            if (previous.get().getId() <= cleared(c, user))
                throw new ResponseStatusException(HttpStatus.GONE,
                        "Il messaggio è stato eliminato dalla tua cronologia.");
            if (!previous.get().getBody().equals(normalized)
                    || !Objects.equals(previous.get().getAttachmentHash(), hash)
                    || !Objects.equals(previous.get().getReplyTo() == null ? null : previous.get().getReplyTo().getId(),
                            replyTo))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Identificatore già usato per un altro messaggio.");
            return view(previous.get(), user);
        }
        ChatMessage message = new ChatMessage();
        message.setConversation(c);
        message.setSenderId(user);
        message.setClientId(clientId);
        message.setBody(normalized);
        if (replyTo != null) {
            ChatMessage replied = visibleMessage(c, user, replyTo);
            message.setReplyTo(replied);
        }
        if (photo != null) {
            message.setAttachmentKey(photos.store(photo));
            message.setAttachmentHash(hash);
        }
        c.setTravelerDeletedAt(null);
        c.setOrganizerDeletedAt(null);
        message = messages.saveAndFlush(message);
        c.setUpdatedAt(message.getSentAt());
        String preview = normalized.isBlank() ? "📷 Foto" : normalized;
        c.setPreview(preview.substring(0,
                preview.offsetByCodePoints(0, Math.min(160, preview.codePointCount(0, preview.length())))));
        Long recipient = c.getTraveler().getId().equals(user) ? c.getOrganizer().getId() : c.getTraveler().getId();
        if (preferences.findById(recipient).map(AccountSettings::isChatAlerts)
                .orElse(true)) {
            var n = notifications.findFirstByUser_IdAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(recipient,
                    "CHAT", conversation).orElseGet(Notification::new);
            n.setUser(users.getReferenceById(recipient));
            n.setType(Notification.NotificationType.NOTICE);
            n.setTitle("Nuovo messaggio da @" + users.findById(user).orElseThrow().getUsername());
            n.setMessage("Hai nuovi messaggi nella conversazione. Apri la chat per leggerli.");
            n.setRead(false);
            n.setRelatedEntityType("CHAT");
            n.setRelatedEntityId(conversation);
            notifications.save(n);
        }
        events.publishEvent(new ChatEvents.Changed(c.getId(), c.getTraveler().getId(), c.getOrganizer().getId()));
        return view(message, user);
    }

    @Transactional
    public void read(Long conversation, Long user, Long through) {
        var c = access(conversation, user, true);
        if (through == null || !messages.existsByIdAndConversationId(through, conversation))
            throw new IllegalArgumentException("Messaggio non trovato in questa conversazione.");
        Long latest = messages.latest(conversation);
        if (latest != null && through >= latest)
            notifications.readConversation(user, conversation);
        if (c.getTraveler().getId().equals(user))
            c.setTravelerReadId(Math.max(c.getTravelerReadId() == null ? 0 : c.getTravelerReadId(), through));
        else
            c.setOrganizerReadId(Math.max(c.getOrganizerReadId() == null ? 0 : c.getOrganizerReadId(), through));
    }

    private long cleared(Conversation c, Long user) {
        Long boundary = c.getTraveler().getId().equals(user) ? c.getTravelerClearedThroughId()
                : c.getOrganizerClearedThroughId();
        return boundary == null ? 0 : boundary;
    }

    private ChatMessage visibleMessage(Conversation c, Long user, Long id) {
        return messages.findById(id)
                .filter(m -> m.getConversation().getId().equals(c.getId()) && m.getId() > cleared(c, user))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Messaggio non disponibile."));
    }

    public String photo(Long conversation, Long user, Long message) {
        ChatMessage m = visibleMessage(access(conversation, user, false), user, message);
        if (m.getAttachmentKey() == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto non disponibile.");
        return photos.read(m.getAttachmentKey());
    }

    @Transactional
    public void readAll(Long conversation, Long user) {
        access(conversation, user, true);
        Long latest = messages.latest(conversation);
        if (latest != null)
            read(conversation, user, latest);
        events.publishEvent(new ChatEvents.Changed(conversation, user, user));
    }

    @Transactional
    public void delete(Long conversation, Long user) {
        Conversation c = access(conversation, user, true);
        Long latest = messages.latest(conversation);
        if (c.getTraveler().getId().equals(user)) {
            c.setTravelerDeletedAt(Instant.now());
            c.setTravelerClearedThroughId(latest);
            c.setTravelerReadId(latest);
        } else {
            c.setOrganizerDeletedAt(Instant.now());
            c.setOrganizerClearedThroughId(latest);
            c.setOrganizerReadId(latest);
        }
        notifications.readConversation(user, conversation);
        events.publishEvent(new ChatEvents.Changed(conversation, user, user));
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
                cleared(c, user) > 0 && cleared(c, user) >= Objects.requireNonNullElse(messages.latest(c.getId()), 0L)
                        ? ""
                        : c.getPreview(),
                c.getUpdatedAt(), unread);
    }

    private MessageView view(ChatMessage m, Long viewer) {
        ChatMessage reply = m.getReplyTo();
        if (reply != null && reply.getId() <= cleared(m.getConversation(), viewer))
            reply = null;
        return new MessageView(m.getId(), m.getSenderId(), m.getClientId(), m.getBody(), m.getSentAt(),
                reply == null ? null
                        : new ReplyView(reply.getId(), reply.getSenderId(),
                                reply.getBody().isBlank() ? "📷 Foto"
                                        : reply.getBody().substring(0, Math.min(240, reply.getBody().length()))),
                m.getAttachmentKey() != null);
    }
}
