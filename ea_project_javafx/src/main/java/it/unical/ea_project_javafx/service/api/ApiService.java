package it.unical.ea_project_javafx.service.api;

import com.google.gson.JsonObject;
import it.unical.ea_project_javafx.dto.user.UserDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.security.TlsSupport;
import it.unical.ea_project_javafx.security.TokenStorage;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.notification.NotificationPollingService;
import it.unical.ea_project_javafx.util.JsonCodec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Setter;

public class ApiService {

    public static void init() {
        TlsSupport.installAsDefault();
    }

    public enum Method {
        GET, POST, PUT, DELETE
    }

    public static final String BASE_URL = System
            .getProperty("eaproject.api.url",
                    System.getenv().getOrDefault("EA_PROJECT_API_URL", "https://localhost:8443"))
            .replaceAll("/+$", "");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .sslContext(TlsSupport.context())
            .build();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private static CompletableFuture<Boolean> refreshFuture;

    public static <T> T fromJson(String json, Type type) {
        return JsonCodec.read(json, type);
    }

    public static String toJson(Object value) {
        return JsonCodec.write(value);
    }

    public static String resolveUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return BASE_URL + path;
    }

    @Setter
    private static Runnable onNetworkFailureGlobal;

    @FunctionalInterface
    private interface RequestFactory {
        HttpRequest build(String token) throws Exception;
    }

    private static void executeRequest(RequestFactory requestFactory,
            Consumer<HttpResponse<String>> onSuccess, Consumer<String> onServerFailure,
            Consumer<Throwable> onNetworkFailure, Consumer<Boolean> loadingSetter, boolean allowRefresh) {
        setLoadingOnFxThread(loadingSetter, true);
        long generation = UserSession.getInstance().getGeneration();
        String token = UserSession.getInstance().getAccessToken();
        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                return CLIENT.send(requestFactory.build(token), HttpResponse.BodyHandlers.ofString());
            }
        };
        task.setOnSucceeded(event -> {
            if (generation != UserSession.getInstance().getGeneration()) {
                setLoadingOnFxThread(loadingSetter, false);
                return;
            }
            HttpResponse<String> response = task.getValue();
            if (response.statusCode() == 401 && token != null && !token.isBlank() && allowRefresh) {
                refreshAccessToken().thenAcceptAsync(success -> {
                    if (generation != UserSession.getInstance().getGeneration()) {
                        setLoadingOnFxThread(loadingSetter, false);
                        return;
                    }
                    if (Boolean.TRUE.equals(success)) {
                        executeRequest(requestFactory, onSuccess, onServerFailure, onNetworkFailure, loadingSetter,
                                false);
                    } else {
                        setLoadingOnFxThread(loadingSetter, false);
                        forceLogout();
                    }
                }, Platform::runLater);
                return;
            }
            setLoadingOnFxThread(loadingSetter, false);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (onSuccess != null)
                    onSuccess.accept(response);
            } else if (onServerFailure != null) {
                onServerFailure.accept(extractErrorMessage(response.body(), response.statusCode()));
            }
        });
        task.setOnFailed(event -> {
            setLoadingOnFxThread(loadingSetter, false);
            if (generation == UserSession.getInstance().getGeneration() && onNetworkFailure != null)
                onNetworkFailure.accept(task.getException());
        });
        EXECUTOR.execute(task);
    }

    private static synchronized CompletableFuture<Boolean> refreshAccessToken() {
        if (refreshFuture != null)
            return refreshFuture;
        CompletableFuture<Boolean> pending = new CompletableFuture<>();
        refreshFuture = pending;
        executeRefreshTokenCall();
        return pending;
    }

    public static void callDetailed(String url, String body, Method method,
            Consumer<HttpResponse<String>> onSuccess, Consumer<String> onFailure,
            Consumer<Boolean> loadingSetter) {
        executeRequest(token -> buildRequest(url, body, method, token), onSuccess, onFailure,
                error -> {
                    if (onFailure != null)
                        onFailure.accept(
                                "Impossibile completare la richiesta: verifica che il server sia raggiungibile.");
                },
                loadingSetter, true);
    }

    public static String extractErrorMessage(String responseBody, int statusCode) {
        String fallback = switch (statusCode) {
            case 400 -> "I dati inseriti non sono validi.";
            case 401 -> "Sessione scaduta o credenziali non corrette.";
            case 403 -> "Non hai i permessi per eseguire questa operazione.";
            case 404 -> "Risorsa non trovata.";
            case 409 -> "I dati inseriti sono già utilizzati da un altro account.";
            case 413 -> "Il file caricato è troppo grande.";
            case 429 -> "Hai effettuato troppe richieste. Riprova tra poco.";
            default ->
                statusCode >= 500 ? "Errore interno del server. Riprova più tardi." : "Richiesta non completata.";
        };
        if (responseBody == null || responseBody.isBlank())
            return fallback;
        try {
            JsonObject json = JsonCodec.read(responseBody, JsonObject.class);
            if (json != null) {
                if (statusCode == 429 && json.has("code") && !json.get("code").isJsonNull()) {
                    String code = json.get("code").getAsString();
                    if (("ERR-104".equalsIgnoreCase(code) || "ERR-105".equalsIgnoreCase(code))
                            && json.has("message") && !json.get("message").isJsonNull()) {
                        return code.toUpperCase(Locale.ROOT) + " — " + json.get("message").getAsString();
                    }
                }
                for (String key : new String[] { "message", "detail", "reason", "error" }) {
                    if (json.has(key) && !json.get(key).isJsonNull()) {
                        String value = json.get(key).getAsString();
                        if (!value.isBlank() && !value.equalsIgnoreCase("Bad Request")
                                && !value.equalsIgnoreCase("Conflict") && !value.equalsIgnoreCase("Unauthorized")) {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String plain = responseBody.trim();
        if (!plain.startsWith("{") && plain.length() <= 250 && !plain.contains("<html"))
            return plain;
        return fallback;
    }

    public static void get(String url, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<Boolean> loadingSetter) {
        call(url, null, Method.GET, onSuccess, onFailure, loadingSetter);
    }

    public static void post(String url, String body, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<Boolean> loadingSetter) {
        call(url, body, Method.POST, onSuccess, onFailure, loadingSetter);
    }

    public static void put(String url, String body, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<Boolean> loadingSetter) {
        call(url, body, Method.PUT, onSuccess, onFailure, loadingSetter);
    }

    public static void delete(String url, Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<Boolean> loadingSetter) {
        call(url, null, Method.DELETE, onSuccess, onFailure, loadingSetter);
    }

    public static void call(String url, String body, Method method, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<Boolean> loadingSetter) {
        executeRequest(token -> buildRequest(url, body, method, token), onSuccess,
                message -> {
                    if (onFailure != null)
                        onFailure.run();
                },
                error -> handleFailure(onFailure), loadingSetter, true);
    }

    public static void uploadPhoto(String url, Path file, Map<String, String> fields,
            Consumer<HttpResponse<String>> success, Consumer<String> failure) {
        Map<String, String> snapshot = Map.copyOf(fields);
        executeRequest(token -> {
            if (Files.size(file) > 8 * 1024 * 1024)
                throw new IOException("Foto troppo grande");
            return buildMultipartRequest(url, List.of(file), "file", token, snapshot);
        }, success, failure, e -> failure.accept("Impossibile caricare la foto. Controlla il file e la connessione."),
                null, true);
    }

    public static void uploadFile(String url, Path file, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<Boolean> loadingSetter) {
        uploadFiles(url, List.of(file), "file", onSuccess, onFailure, null, loadingSetter);
    }

    public static void uploadFileDetailed(String url, Path file, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<String> onServerFailure,
            Consumer<Boolean> loadingSetter) {
        uploadFiles(url, List.of(file), "file", onSuccess, onFailure, onServerFailure, loadingSetter);
    }

    public static void uploadFiles(String url, List<Path> files, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<Boolean> loadingSetter) {
        uploadFiles(url, files, "files", onSuccess, onFailure, null, loadingSetter);
    }

    public static void uploadFilesDetailed(String url, List<Path> files,
            Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<String> onServerFailure, Consumer<Boolean> loadingSetter) {
        uploadFiles(url, files, "files", onSuccess, onFailure, onServerFailure, loadingSetter);
    }

    private static void uploadFiles(String url, List<Path> files, String formField,
            Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<String> onServerFailure, Consumer<Boolean> loadingSetter) {
        List<Path> selection = List.copyOf(files);
        executeRequest(token -> buildMultipartRequest(url, selection, formField, token), onSuccess,
                message -> {
                    if (onServerFailure != null)
                        onServerFailure.accept(message);
                    else if (onFailure != null)
                        onFailure.run();
                }, error -> {
                    if (onServerFailure != null)
                        onServerFailure.accept(
                                "Impossibile caricare le foto: verifica la connessione e che i file siano leggibili.");
                    else
                        handleFailure(onFailure);
                }, loadingSetter, true);
    }

    private static void executeRefreshTokenCall() {
        long generation = UserSession.getInstance().getGeneration();
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
                        HttpResponse.BodyHandlers.ofString());
            }
        };

        task.setOnSucceeded(e -> {
            HttpResponse<String> response = task.getValue();
            String authorization = response.headers()
                    .firstValue("Authorization")
                    .orElse("");

            if (response.statusCode() == 200 && authorization.startsWith("Bearer ")) {
                String newAccessToken = authorization.substring(7);
                if (generation != UserSession.getInstance().getGeneration()) {
                    completeRefresh(false);
                    return;
                }
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
        CompletableFuture<Boolean> pending = refreshFuture;
        refreshFuture = null;
        if (pending != null)
            pending.complete(success);
    }

    private static void forceLogout() {
        UserSession.getInstance().clear();
        TokenStorage.clear();
        NotificationPollingService.getInstance().clearSessionState();
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/common/splash-screen.fxml", null);
    }

    private static HttpRequest buildRequest(String url, String body, Method method, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30));

        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher publisher = (body == null || body.isEmpty())
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

    private static HttpRequest buildMultipartRequest(String url, List<Path> files, String formField,
            String token) throws IOException {
        return buildMultipartRequest(url, files, formField, token, Map.of());
    }

    private static HttpRequest buildMultipartRequest(String url, List<Path> files, String formField,
            String token, Map<String, String> fields) throws IOException {
        String boundary = "----EAProjectUpload" + UUID.randomUUID();
        List<HttpRequest.BodyPublisher> parts = new ArrayList<>();
        for (var field : fields.entrySet()) {
            if (!field.getKey().matches("[A-Za-z][A-Za-z0-9]*"))
                throw new IOException("Campo non valido");
            parts.add(HttpRequest.BodyPublishers.ofString(
                    "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field.getKey()
                            + "\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n" + field.getValue() + "\r\n",
                    StandardCharsets.UTF_8));
        }
        for (Path file : files) {
            parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + formField + "\"; filename=\""
                    + file.getFileName().toString().replace("\"", "").replace("\r", "").replace("\n", "") + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n", StandardCharsets.UTF_8));
            parts.add(HttpRequest.BodyPublishers.ofFile(file));
            parts.add(HttpRequest.BodyPublishers.ofString("\r\n"));
        }
        parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "--\r\n"));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.POST(HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new))).build();
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

            var response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setLoadingOnFxThread(Consumer<Boolean> loadingSetter, boolean value) {
        if (loadingSetter == null)
            return;

        if (Platform.isFxApplicationThread()) {
            loadingSetter.accept(value);
        } else {
            Platform.runLater(() -> loadingSetter.accept(value));
        }
    }

    public static void restoreSessionFromStoredRefreshToken(Runnable onSuccess, Runnable onInvalidToken,
            Runnable onNetworkError) {
        long generation = UserSession.getInstance().getGeneration();
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
                        HttpResponse.BodyHandlers.ofString());
            }
        };

        task.setOnSucceeded(event -> {
            if (generation != UserSession.getInstance().getGeneration())
                return;
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

            if (generation != UserSession.getInstance().getGeneration())
                return;
            String accessToken = authorization.substring(7);

            UserSession.getInstance().setTokens(
                    accessToken,
                    refreshToken);

            get(
                    BASE_URL + "/api/users/me",
                    meResponse -> {
                        if (meResponse.statusCode() == 200) {
                            try {
                                UserDTO user = JsonCodec.read(
                                        meResponse.body(),
                                        UserDTO.class);

                                if (user != null && generation == UserSession.getInstance().getGeneration()) {
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
                    null);
        });

        task.setOnFailed(event -> {
            if (generation == UserSession.getInstance().getGeneration())
                Platform.runLater(onNetworkError);
        });
        EXECUTOR.execute(task);
    }
}
