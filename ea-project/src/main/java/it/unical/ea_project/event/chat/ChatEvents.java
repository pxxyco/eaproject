package it.unical.ea_project.event.chat;

import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.user.UserRepository;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Events contain only a conversation ID. Database history is the source of
 * truth after reconnect.
 */
@Component
@EnableScheduling
public class ChatEvents {
    public record Changed(Long conversationId, Long travelerId, Long organizerId) {
    }

    private record Connection(SseEmitter emitter, Instant expires, long version) {
    }

    private final Map<Long, CopyOnWriteArrayList<Connection>> clients = new ConcurrentHashMap<>();
    private final UserRepository users;
    private final ThreadPoolExecutor delivery = new ThreadPoolExecutor(1, 4, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(256), r -> {
                Thread t = new Thread(r, "chat-delivery");
                t.setDaemon(true);
                return t;
            });

    public ChatEvents(UserRepository users) {
        this.users = users;
    }

    public synchronized SseEmitter connect(Long user, Instant expires, long version) {
        if (clients.values().stream().mapToInt(List::size).sum() >= 500)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chat temporaneamente occupata. Riprova tra poco.");
        var connections = clients.computeIfAbsent(user, k -> new CopyOnWriteArrayList<>());
        if (connections.size() >= 3)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Troppe connessioni chat aperte.");
        SseEmitter emitter = new SseEmitter(300_000L);
        var connection = new Connection(emitter, expires, version);
        connections.add(connection);
        Runnable remove = () -> remove(user, connection);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected").reconnectTime(3000));
        } catch (IOException ex) {
            remove.run();
        }
        return emitter;
    }

    private synchronized void remove(Long id, Connection c) {
        var list = clients.get(id);
        if (list != null) {
            list.remove(c);
            if (list.isEmpty())
                clients.remove(id, list);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void changed(Changed event) {
        try {
            delivery.execute(() -> {
                send(event.travelerId(), event.conversationId());
                send(event.organizerId(), event.conversationId());
            });
        } catch (RejectedExecutionException ex) {
            disconnect(event.travelerId());
            disconnect(event.organizerId());
        }
    }

    private void disconnect(Long user) {
        for (var c : clients.getOrDefault(user, new CopyOnWriteArrayList<>())) {
            c.emitter.complete();
            remove(user, c);
        }
    }

    private void send(Long user, Long conversation) {
        var account = users.findById(user).orElse(null);
        for (var c : clients.getOrDefault(user, new CopyOnWriteArrayList<>())) {
            if (account == null || account.getDeletedAt() != null || account.getAuthVersion() != c.version
                    || !c.expires.isAfter(Instant.now())) {
                c.emitter.complete();
                remove(user, c);
                continue;
            }
            try {
                c.emitter.send(SseEmitter.event().name("changed").data(conversation.toString()));
            } catch (Exception ex) {
                remove(user, c);
            }
        }
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        Map<Long, User> accounts = new HashMap<>();
        users.findAllById(clients.keySet()).forEach(u -> accounts.put(u.getId(), u));
        clients.forEach((id, list) -> {
            var user = accounts.get(id);
            for (var c : list) {
                if (user == null || user.getDeletedAt() != null || user.getAuthVersion() != c.version
                        || !c.expires.isAfter(Instant.now())) {
                    c.emitter.complete();
                    remove(id, c);
                    continue;
                }
                try {
                    c.emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception ex) {
                    remove(id, c);
                }
            }
        });
    }

    @PreDestroy
    public void close() {
        delivery.shutdownNow();
        clients.values().forEach(l -> l.forEach(c -> c.emitter.complete()));
        clients.clear();
    }
}
