package it.unical.ea_project_javafx.service;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.*;
import java.util.function.*;

/**
 * One SSE connection shared by the navbar, full chat and compact chat. FX
 * thread only.
 */
public final class ChatEventHub {
    private static final Map<Long, LongConsumer> changes = new LinkedHashMap<>();
    private static final Map<Long, Consumer<String>> states = new LinkedHashMap<>();
    private static ChatStream stream;
    private static long next;
    private static String status = "Connessione…";
    private static final PauseTransition release = new PauseTransition(Duration.seconds(2));

    public static AutoCloseable subscribe(LongConsumer changed, Consumer<String> state) {
        release.stop();
        long id = ++next;
        changes.put(id, changed);
        states.put(id, state);
        state.accept(status);
        if (stream == null)
            stream = new ChatStream(value -> new ArrayList<>(changes.values()).forEach(c -> c.accept(value)), value -> {
                status = value;
                new ArrayList<>(states.values()).forEach(c -> c.accept(value));
            });
        return () -> {
            changes.remove(id);
            states.remove(id);
            if (changes.isEmpty()) {
                release.setOnFinished(e -> {
                    if (changes.isEmpty() && stream != null) {
                        stream.close();
                        stream = null;
                    }
                });
                release.playFromStart();
            }
        };
    }

    public static void reset() {
        release.stop();
        if (stream != null)
            stream.close();
        stream = null;
        changes.clear();
        states.clear();
        status = "Connessione…";
    }
}
