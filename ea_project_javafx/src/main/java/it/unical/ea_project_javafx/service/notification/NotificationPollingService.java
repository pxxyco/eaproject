package it.unical.ea_project_javafx.service.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.service.chat.ChatEventHub;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public final class NotificationPollingService {
    private static final NotificationPollingService INSTANCE = new NotificationPollingService();
    private static final String UNREAD_URL = ApiService.BASE_URL + "/api/notifications/unread";
    private static final int POLLING_SECONDS = 60;
    private static final int MAX_REMEMBERED_IDS = 1200;

    private final Set<Consumer<Snapshot>> listeners = new LinkedHashSet<>();
    private final Set<Long> seenUnreadIds = new HashSet<>();
    private final List<TrayCallback> trayCallbacks = new ArrayList<>();
    private Timeline timeline;
    private AutoCloseable chatSubscription;
    private final PauseTransition chatRefresh = new PauseTransition(
            Duration.millis(350));
    private boolean requestInFlight;
    private boolean initialPoll = true;
    private boolean hasSnapshot;
    private JsonArray latestUnread = new JsonArray();
    private long snapshotAt;
    private int lastUnreadCount;
    private long retryAfterEpochMillis;
    private long sessionGeneration;
    private String lastRateLimitMessage = "ERR-104 — Troppe richieste. Riprova tra 60 secondi.";

    private NotificationPollingService() {
    }

    public static NotificationPollingService getInstance() {
        return INSTANCE;
    }

    public Subscription subscribe(Consumer<Snapshot> listener) {
        requireFxThread();
        if (listener == null)
            throw new IllegalArgumentException("Il listener delle notifiche è obbligatorio.");
        listeners.add(listener);
        if (hasSnapshot)
            listener.accept(new Snapshot(lastUnreadCount, null));
        if (chatSubscription == null) {
            chatRefresh.setOnFinished(e -> pollNow());
            chatSubscription = ChatEventHub
                    .subscribe(id -> chatRefresh.playFromStart(), status -> {
                    });
        }
        if (timeline == null) {
            pollNow();
            timeline = new Timeline(new KeyFrame(Duration.seconds(POLLING_SECONDS), event -> pollNow()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }
        return new Subscription(this, listener);
    }

    public void refreshNow() {
        requireFxThread();
        pollNow();
    }

    public void fetchUnreadForTray(Consumer<JsonArray> onSuccess, Consumer<String> onFailure) {
        requireFxThread();
        if (!UserSession.getInstance().isLoggedIn()) {
            if (onFailure != null)
                onFailure.accept("Effettua l'accesso per vedere le notifiche.");
            return;
        }
        if (hasSnapshot && System.currentTimeMillis() - snapshotAt < 5000) {
            if (onSuccess != null)
                onSuccess.accept(latestUnread.deepCopy());
            return;
        }
        if (isRateLimitBackoffActive()) {
            if (onFailure != null)
                onFailure.accept(lastRateLimitMessage);
            return;
        }
        if (onSuccess != null || onFailure != null)
            trayCallbacks.add(new TrayCallback(onSuccess, onFailure));
        pollNow();
    }

    public void clearSessionState() {
        requireFxThread();
        sessionGeneration++;
        chatRefresh.stop();
        ChatEventHub.reset();
        chatSubscription = null;
        seenUnreadIds.clear();
        trayCallbacks.clear();
        initialPoll = true;
        hasSnapshot = false;
        latestUnread = new JsonArray();
        snapshotAt = 0;
        lastUnreadCount = 0;
        retryAfterEpochMillis = 0L;
        lastRateLimitMessage = "ERR-104 — Troppe richieste. Riprova tra 60 secondi.";
        publish(new Snapshot(0, null));
    }

    private void pollNow() {
        if (requestInFlight || !UserSession.getInstance().isLoggedIn()
                || (listeners.isEmpty() && trayCallbacks.isEmpty()) || isRateLimitBackoffActive())
            return;
        requestInFlight = true;
        long requestGeneration = sessionGeneration;
        ApiService.callDetailed(UNREAD_URL, null, ApiService.Method.GET, response -> {
            requestInFlight = false;
            if (requestGeneration != sessionGeneration) {
                if (UserSession.getInstance().isLoggedIn()
                        && (!listeners.isEmpty() || !trayCallbacks.isEmpty()))
                    pollNow();
                return;
            }
            if ((!listeners.isEmpty() || !trayCallbacks.isEmpty()) && UserSession.getInstance().isLoggedIn()) {
                try {
                    JsonArray unread = JsonParser.parseString(response.body()).getAsJsonArray();
                    List<Long> currentIds = new ArrayList<>();
                    JsonObject firstNew = null;
                    for (JsonElement element : unread) {
                        if (!element.isJsonObject())
                            continue;
                        JsonObject notification = element.getAsJsonObject();
                        if (!notification.has("notificationId") || notification.get("notificationId").isJsonNull())
                            continue;
                        long id = notification.get("notificationId").getAsLong();
                        currentIds.add(id);
                        if (initialPoll) {
                            seenUnreadIds.add(id);
                        } else if (seenUnreadIds.add(id) && firstNew == null) {
                            firstNew = notification;
                        }
                    }
                    if (initialPoll)
                        initialPoll = false;
                    if (seenUnreadIds.size() > MAX_REMEMBERED_IDS) {
                        seenUnreadIds.clear();
                        seenUnreadIds.addAll(currentIds);
                    } else {
                        seenUnreadIds.retainAll(new HashSet<>(currentIds));
                    }
                    latestUnread = unread;
                    snapshotAt = System.currentTimeMillis();
                    lastUnreadCount = unread.size();
                    hasSnapshot = true;
                    publish(new Snapshot(lastUnreadCount, trayCallbacks.isEmpty() ? firstNew : null));
                    notifyTraySuccess(unread);
                } catch (RuntimeException ignored) {
                    notifyTrayFailure("Non è stato possibile leggere le notifiche.");
                }
            } else {
                notifyTrayFailure("La sessione non è più attiva.");
            }
        }, message -> {
            requestInFlight = false;
            if (requestGeneration != sessionGeneration) {
                if (UserSession.getInstance().isLoggedIn()
                        && (!listeners.isEmpty() || !trayCallbacks.isEmpty()))
                    pollNow();
                return;
            }
            String failure = message == null || message.isBlank()
                    ? "Non è stato possibile caricare le notifiche."
                    : message;
            if (failure.toUpperCase(Locale.ROOT).contains("ERR-104")) {
                retryAfterEpochMillis = System.currentTimeMillis() + POLLING_SECONDS * 1000L;
                lastRateLimitMessage = failure;
            }
            notifyTrayFailure(failure);
        }, null);
    }

    private boolean isRateLimitBackoffActive() {
        return System.currentTimeMillis() < retryAfterEpochMillis;
    }

    private void notifyTraySuccess(JsonArray unread) {
        List<TrayCallback> copy = new ArrayList<>(trayCallbacks);
        trayCallbacks.clear();
        for (TrayCallback callback : copy) {
            if (callback.success() != null)
                callback.success().accept(unread);
        }
    }

    private void notifyTrayFailure(String message) {
        if (trayCallbacks.isEmpty())
            return;
        List<TrayCallback> copy = new ArrayList<>(trayCallbacks);
        trayCallbacks.clear();
        for (TrayCallback callback : copy) {
            if (callback.failure() != null)
                callback.failure().accept(message);
        }
    }

    private record TrayCallback(Consumer<JsonArray> success, Consumer<String> failure) {
    }

    private void publish(Snapshot snapshot) {
        List<Consumer<Snapshot>> copy = new ArrayList<>(listeners);
        for (int i = 0; i < copy.size(); i++) {
            Consumer<Snapshot> listener = copy.get(i);
            listener.accept(i == 0 ? snapshot : new Snapshot(snapshot.unreadCount(), null));
        }
    }

    private void unsubscribe(Consumer<Snapshot> listener) {
        requireFxThread();
        listeners.remove(listener);
        if (listeners.isEmpty() && chatSubscription != null) {
            try {
                chatSubscription.close();
            } catch (Exception ignored) {
            }
            chatSubscription = null;
            chatRefresh.stop();
        }
        if (listeners.isEmpty() && timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "NotificationPollingService deve essere utilizzato dal JavaFX Application Thread.");
        }
    }

    public record Snapshot(int unreadCount, JsonObject newNotification) {
    }

    public static final class Subscription implements AutoCloseable {
        private final NotificationPollingService owner;
        private final Consumer<Snapshot> listener;
        private boolean closed;

        private Subscription(NotificationPollingService owner, Consumer<Snapshot> listener) {
            this.owner = owner;
            this.listener = listener;
        }

        @Override
        public void close() {
            if (closed)
                return;
            closed = true;
            owner.unsubscribe(listener);
        }
    }
}
