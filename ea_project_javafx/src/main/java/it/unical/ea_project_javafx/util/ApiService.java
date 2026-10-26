package it.unical.ea_project_javafx.util;

import com.google.gson.JsonObject;
import it.unical.ea_project_javafx.dto.UserDTO;
import it.unical.ea_project_javafx.security.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.Setter;

import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ApiService {

    public enum Method {
        GET, POST, PUT, DELETE
    }

    public static final String BASE_URL = System
            .getProperty("itinera.api.url", System.getenv().getOrDefault("ITINERA_API_URL", "http://localhost:8080"))
            .replaceAll("/+$", "");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private static CompletableFuture<Boolean> refreshFuture;

    public static <T> T fromJson(String json, java.lang.reflect.Type type) {
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

    /**
     * Shared transport for JSON and multipart requests, with one refresh/retry
     * policy.
     */
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

    /** Preserves the server's validation/conflict message for forms. */
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
                    if ("ERR-104".equalsIgnoreCase(code) && json.has("message") && !json.get("message").isJsonNull()) {
                        return "ERR-104 — " + json.get("message").getAsString();
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
            // The body may be plain text or a framework error document.
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

    public static void uploadFile(String url, Path file, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<Boolean> loadingSetter) {
        uploadFiles(url, java.util.List.of(file), "file", onSuccess, onFailure, null, loadingSetter);
    }

    public static void uploadFileDetailed(String url, Path file, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<String> onServerFailure,
            Consumer<Boolean> loadingSetter) {
        uploadFiles(url, java.util.List.of(file), "file", onSuccess, onFailure, onServerFailure, loadingSetter);
    }

    public static void uploadFiles(String url, java.util.List<Path> files, Consumer<HttpResponse<String>> onSuccess,
            Runnable onFailure, Consumer<Boolean> loadingSetter) {
        uploadFiles(url, files, "files", onSuccess, onFailure, null, loadingSetter);
    }

    public static void uploadFilesDetailed(String url, java.util.List<Path> files,
            Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<String> onServerFailure, Consumer<Boolean> loadingSetter) {
        uploadFiles(url, files, "files", onSuccess, onFailure, onServerFailure, loadingSetter);
    }

    private static void uploadFiles(String url, java.util.List<Path> files, String formField,
            Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
            Consumer<String> onServerFailure, Consumer<Boolean> loadingSetter) {
        // Copy the selection: later edits to the form cannot change an in-flight
        // upload.
        java.util.List<Path> selection = java.util.List.copyOf(files);
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

    private static HttpRequest buildMultipartRequest(String url, java.util.List<Path> files, String formField,
            String token) throws IOException {
        String boundary = "----CopilotUpload" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (Path file : files) {
            body.write(("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + formField + "\"; filename=\""
                    + file.getFileName().toString().replace("\"", "").replace("\r", "").replace("\n", "") + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(Files.readAllBytes(file));
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
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

            CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
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
