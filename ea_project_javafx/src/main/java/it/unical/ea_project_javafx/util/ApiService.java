package it.unical.ea_project_javafx.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import it.unical.ea_project_javafx.dto.UserDTO;
import it.unical.ea_project_javafx.security.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ApiService {

    public enum Method { GET, POST, PUT, DELETE }

    public static final String BASE_URL = "http://localhost:8080";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicBoolean isRefreshing = new AtomicBoolean(false);
    private static CompletableFuture<Boolean> refreshFuture;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                    deserializeLocalDateTime(json))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context) ->
                    deserializeLocalDate(json))
            .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, context) ->
                    deserializeLocalTime(json))
            .create();

    public static <T> T fromJson(String json, java.lang.reflect.Type type) {
        return GSON.fromJson(json, type);
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    private static LocalDate deserializeLocalDate(JsonElement json) {
        if (!json.isJsonArray()) {
            return LocalDate.parse(json.getAsString());
        }
        JsonArray parts = json.getAsJsonArray();
        if (parts.size() != 3) {
            throw new JsonParseException("Expected a LocalDate as an ISO string or a 3-part array.");
        }
        return LocalDate.of(parts.get(0).getAsInt(), parts.get(1).getAsInt(), parts.get(2).getAsInt());
    }

    private static LocalDateTime deserializeLocalDateTime(JsonElement json) {
        if (!json.isJsonArray()) {
            return LocalDateTime.parse(json.getAsString());
        }
        JsonArray parts = json.getAsJsonArray();
        if (parts.size() < 5 || parts.size() > 7) {
            throw new JsonParseException("Expected a LocalDateTime as an ISO string or a 5-to-7-part array.");
        }
        int second = parts.size() > 5 ? parts.get(5).getAsInt() : 0;
        int nano = parts.size() > 6 ? parts.get(6).getAsInt() : 0;
        return LocalDateTime.of(
                parts.get(0).getAsInt(),
                parts.get(1).getAsInt(),
                parts.get(2).getAsInt(),
                parts.get(3).getAsInt(),
                parts.get(4).getAsInt(),
                second,
                nano
        );
    }

    private static LocalTime deserializeLocalTime(JsonElement json) {
        if (!json.isJsonArray()) {
            return LocalTime.parse(json.getAsString());
        }
        JsonArray parts = json.getAsJsonArray();
        if (parts.size() < 2 || parts.size() > 4) {
            throw new JsonParseException("Expected a LocalTime as an ISO string or a 2-to-4-part array.");
        }
        int second = parts.size() > 2 ? parts.get(2).getAsInt() : 0;
        int nano = parts.size() > 3 ? parts.get(3).getAsInt() : 0;
        return LocalTime.of(
                parts.get(0).getAsInt(),
                parts.get(1).getAsInt(),
                second,
                nano
        );
    }

    @Setter
    private static Runnable onNetworkFailureGlobal;

    public static void get(String url, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure, Consumer<Boolean> loadingSetter) {
        call(url, null, Method.GET, onSuccess, onFailure, loadingSetter);
    }

    public static void post(String url, String body, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure, Consumer<Boolean> loadingSetter) {
        call(url, body, Method.POST, onSuccess, onFailure, loadingSetter);
    }

    public static void put(String url, String body, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure, Consumer<Boolean> loadingSetter) {
        call(url, body, Method.PUT, onSuccess, onFailure, loadingSetter);
    }

    public static void delete(String url, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure, Consumer<Boolean> loadingSetter) {
        call(url, null, Method.DELETE, onSuccess, onFailure, loadingSetter);
    }

    public static void call(String url, String body, Method method, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure, Consumer<Boolean> loadingSetter) {
        call(url, body, method, onSuccess, onFailure, loadingSetter, true);
    }

    private static void call(String url, String body, Method method, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure, Consumer<Boolean> loadingSetter, boolean allowRefresh) {
        setLoadingOnFxThread(loadingSetter, true);

        String sentToken = UserSession.getInstance().getAccessToken();
        boolean hadToken = sentToken != null && !sentToken.isBlank();

        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                return CLIENT.send(
                        buildRequest(url, body, method),
                        HttpResponse.BodyHandlers.ofString()
                );
            }
        };

        task.setOnSucceeded(e -> {
            HttpResponse<String> response = task.getValue();

            if (response.statusCode() == 401 && hadToken && allowRefresh) {
                CompletableFuture<Boolean> future;

                synchronized (ApiService.class) {
                    if (isRefreshing.compareAndSet(false, true)) {
                        refreshFuture = new CompletableFuture<>();
                        executeRefreshTokenCall();
                    }
                    future = refreshFuture;
                }

                future.thenAcceptAsync(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        call(url, body, method, onSuccess, onFailure, loadingSetter, false);
                    } else {
                        setLoadingOnFxThread(loadingSetter, false);
                        forceLogout();
                    }
                }, Platform::runLater);

                return;
            }

            setLoadingOnFxThread(loadingSetter, false);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (onSuccess != null) {
                    onSuccess.accept(response);
                }
            } else {
                handleFailure(onFailure);
            }
        });

        task.setOnFailed(e -> {
            setLoadingOnFxThread(loadingSetter, false);
            handleFailure(onFailure);
        });

        EXECUTOR.execute(task);
    }

    private static void executeRefreshTokenCall() {
        String refresh = UserSession.getInstance().getRefreshToken();

        if (refresh == null || refresh.isBlank()) {
            completeRefresh(false);
            return;
        }

        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/users/refresh"))
                        .timeout(Duration.ofSeconds(10))
                        .header("X-Refresh-Token", refresh)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                return CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
            }
        };

        task.setOnSucceeded(e -> {
            HttpResponse<String> response = task.getValue();
            String authorization = response.headers()
                    .firstValue("Authorization")
                    .orElse("");

            if (response.statusCode() == 200 && authorization.startsWith("Bearer ")) {
                String newAccessToken = authorization.substring(7);
                UserSession.getInstance().setTokens(newAccessToken, refresh);
                completeRefresh(true);
            } else {
                completeRefresh(false);
            }
        });

        task.setOnFailed(e -> completeRefresh(false));
        EXECUTOR.execute(task);
    }

    private static synchronized void completeRefresh(boolean success) {
        isRefreshing.set(false);

        if (refreshFuture != null) {
            refreshFuture.complete(success);
            refreshFuture = null;
        }
    }

    private static void forceLogout() {
        UserSession.getInstance().clear();
        TokenStorage.clear();
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/splash-screen.fxml", null);
    }

    private static HttpRequest buildRequest(String url, String body, Method method) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));

        String token = UserSession.getInstance().getAccessToken();

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher publisher =
                (body == null || body.isEmpty())
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body);

        switch (method) {
            case POST -> builder.header("Content-Type", "application/json").POST(publisher);
            case PUT -> builder.header("Content-Type", "application/json").PUT(publisher);
            case DELETE -> {
                if (body != null && !body.isEmpty()) {
                    builder.header("Content-Type", "application/json").method("DELETE", publisher);
                } else {
                    builder.DELETE();
                }
            }
            case GET -> builder.GET();
        }

        return builder.build();
    }

    private static void handleFailure(Runnable onFailure) {
        EXECUTOR.execute(() -> {
            boolean reachable = isBackendReachable();

            Platform.runLater(() -> {
                if (!reachable && onNetworkFailureGlobal != null) {
                    onNetworkFailureGlobal.run();
                } else if (onFailure != null) {
                    onFailure.run();
                }
            });
        });
    }

    public static boolean isBackendReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<Void> response = CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setLoadingOnFxThread(Consumer<Boolean> loadingSetter, boolean value) {
        if (loadingSetter == null) return;

        if (Platform.isFxApplicationThread()) {
            loadingSetter.accept(value);
        } else {
            Platform.runLater(() -> loadingSetter.accept(value));
        }
    }

    public static void restoreSessionFromStoredRefreshToken(Runnable onSuccess, Runnable onInvalidToken, Runnable onNetworkError) {
        String refreshToken = TokenStorage.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            Platform.runLater(onInvalidToken);
            return;
        }

        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/users/refresh"))
                        .timeout(Duration.ofSeconds(10))
                        .header("X-Refresh-Token", refreshToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                return CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
            }
        };

        task.setOnSucceeded(event -> {
            HttpResponse<String> response = task.getValue();

            if (response.statusCode() == 401) {
                Platform.runLater(onInvalidToken);
                return;
            }

            String authorization = response.headers()
                    .firstValue("Authorization")
                    .orElse("");

            if (response.statusCode() != 200 || !authorization.startsWith("Bearer ")) {
                Platform.runLater(onNetworkError);
                return;
            }

            String accessToken = authorization.substring(7);

            UserSession.getInstance().setTokens(
                    accessToken,
                    refreshToken
            );

            get(
                    BASE_URL + "/api/users/me",
                    meResponse -> {
                        if (meResponse.statusCode() == 200) {
                            try {
                                UserDTO user = GSON.fromJson(
                                        meResponse.body(),
                                        UserDTO.class
                                );

                                if (user != null) {
                                    UserSession.getInstance().setSession(user);
                                    Platform.runLater(onSuccess);
                                } else {
                                    Platform.runLater(onInvalidToken);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Platform.runLater(onNetworkError);
                            }
                        } else if (meResponse.statusCode() == 401) {
                            Platform.runLater(onInvalidToken);
                        } else {
                            Platform.runLater(onNetworkError);
                        }
                    },
                    () -> Platform.runLater(onNetworkError),
                    null
            );
        });

        task.setOnFailed(event -> Platform.runLater(onNetworkError));
        EXECUTOR.execute(task);
    }
}