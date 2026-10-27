package it.unical.ea_project_javafx.service.chat;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javafx.application.Platform;

/**
 * One connection per visible chat. Both workers are virtual threads and close
 * with the page.
 */
public final class ChatStream implements AutoCloseable {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private volatile boolean closed;
    private volatile InputStream input;
    private volatile long lastActivity = System.nanoTime();
    private final Thread reader, watchdog;
    private final long generation;
    private final LongConsumer changed;
    private final Consumer<String> status;

    public ChatStream(LongConsumer changed, Consumer<String> status) {
        this.changed = changed;
        this.status = status;
        generation = UserSession.getInstance().getGeneration();
        reader = Thread.ofVirtual().name("chat-events").unstarted(this::listen);
        watchdog = Thread.ofVirtual().name("chat-watchdog").unstarted(() -> {
            while (active())
                try {
                    Thread.sleep(5000);
                    if (System.nanoTime() - lastActivity > Duration.ofSeconds(65).toNanos())
                        closeInput();
                } catch (InterruptedException ex) {
                    return;
                }
        });
        reader.start();
        watchdog.start();
    }

    private boolean active() {
        return !closed && UserSession.getInstance().isLoggedIn()
                && generation == UserSession.getInstance().getGeneration();
    }

    private void emit(Runnable action) {
        Platform.runLater(() -> {
            if (active())
                action.run();
        });
    }

    private void listen() {
        int backoff = 1;
        while (active()) {
            try {
                emit(() -> status.accept("Connessione alla chat…"));
                var request = HttpRequest.newBuilder(URI.create(ApiService.BASE_URL + "/api/chat/events"))
                        .header("Authorization", "Bearer " + UserSession.getInstance().getAccessToken())
                        .header("Accept", "text/event-stream").GET().build();
                var response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                input = response.body();
                lastActivity = System.nanoTime();
                if (response.statusCode() == 401) {
                    closeInput();
                    if (!refresh())
                        break;
                    continue;
                }
                if (response.statusCode() != 200) {
                    closeInput();
                    if (response.statusCode() == 403) {
                        emit(() -> status.accept("Accesso alla chat non disponibile."));
                        break;
                    }
                    if (response.statusCode() == 429) {
                        long retry = response.headers().firstValue("Retry-After").map(value -> {
                            try {
                                return Long.parseLong(value);
                            } catch (NumberFormatException ex) {
                                return 30L;
                            }
                        }).orElse(30L);
                        emit(() -> status.accept("Troppe connessioni. Riprovo tra poco…"));
                        Thread.sleep(Math.max(1, Math.min(900, retry)) * 1000L);
                        continue;
                    }
                    throw new IOException("Chat non disponibile");
                }
                emit(() -> status.accept("In tempo reale"));
                backoff = 1;
                try (var lines = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String event = "", data = "", line;
                    while (active() && (line = lines.readLine()) != null) {
                        lastActivity = System.nanoTime();
                        if (line.startsWith("event:"))
                            event = line.substring(6).strip();
                        else if (line.startsWith("data:"))
                            data = line.substring(5).strip();
                        else if (line.isEmpty()) {
                            if (event.equals("ready"))
                                emit(() -> changed.accept(-1));
                            if (event.equals("changed")) {
                                try {
                                    long id = Long.parseLong(data);
                                    emit(() -> changed.accept(id));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            event = "";
                            data = "";
                        }
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                emit(() -> status.accept("Connessione interrotta. Riprovo automaticamente…"));
            } finally {
                closeInput();
            }
            if (active())
                try {
                    Thread.sleep(backoff * 1000L);
                    backoff = Math.min(30, backoff * 2);
                } catch (InterruptedException ex) {
                    break;
                }
        }
    }

    private boolean refresh() throws InterruptedException {
        var done = new CountDownLatch(1);
        var success = new AtomicBoolean();
        Platform.runLater(() -> {
            if (!active()) {
                done.countDown();
                return;
            }
            ApiService.get(ApiService.BASE_URL + "/api/users/me", r -> {
                success.set(r.statusCode() == 200);
                done.countDown();
            }, done::countDown, null);
        });
        return done.await(20, TimeUnit.SECONDS) && success.get() && active();
    }

    private void closeInput() {
        var stream = input;
        input = null;
        if (stream != null)
            try {
                stream.close();
            } catch (IOException ignored) {
            }
    }

    @Override
    public void close() {
        closed = true;
        closeInput();
        reader.interrupt();
        watchdog.interrupt();
    }
}
