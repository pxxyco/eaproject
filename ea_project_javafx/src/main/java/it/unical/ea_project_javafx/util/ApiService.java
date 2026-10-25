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

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.URI;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ApiService {

    public enum Method { GET, POST, PUT, DELETE }

    public static final String BASE_URL = "https://localhost:8443";

    private static final SSLContext SSL_CONTEXT = buildSslContext();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .sslContext(SSL_CONTEXT)
            .build();

    static {
        SSLContext.setDefault(SSL_CONTEXT);
        HttpsURLConnection.setDefaultSSLSocketFactory(SSL_CONTEXT.getSocketFactory());
    }
    public static void init() { }

    private static SSLContext buildSslContext() {
        try {
            TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultTmf.init((KeyStore) null);
            X509TrustManager defaultTm = firstX509(defaultTmf.getTrustManagers());
            X509TrustManager devTm = loadDevTrustManager();

            X509TrustManager combined = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    defaultTm.checkClientTrusted(chain, authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    try {
                        defaultTm.checkServerTrusted(chain, authType);
                    } catch (CertificateException e) {
                        if (devTm == null) throw e;
                        devTm.checkServerTrusted(chain, authType);
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return defaultTm.getAcceptedIssuers();
                }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{combined}, null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile inizializzare il contesto SSL", e);
        }
    }

    private static X509TrustManager loadDevTrustManager() throws Exception {
        try (InputStream in = ApiService.class.getResourceAsStream(
                "/it/unical/ea_project_javafx/certs/dev-ca.crt")) {
            if (in == null) return null;
            KeyStore devStore = KeyStore.getInstance(KeyStore.getDefaultType());
            devStore.load(null, null);
            devStore.setCertificateEntry("dev-ca", CertificateFactory.getInstance("X.509").generateCertificate(in));
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(devStore);
            return firstX509(tmf.getTrustManagers());
        }
    }
    private static X509TrustManager firstX509(TrustManager[] managers) {
        for (TrustManager tm : managers) {
            if (tm instanceof X509TrustManager x509) return x509;
        }
        throw new IllegalStateException("Nessun X509TrustManager disponibile");
    }

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


    @Setter
    private static Consumer<Integer> onRateLimitedGlobal;

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
        uploadFiles(url, files, formField, onSuccess, onFailure, onServerFailure, loadingSetter, true);
    }

    private static void uploadFiles(String url, java.util.List<Path> files, String formField,
                                    Consumer<HttpResponse<String>> onSuccess, Runnable onFailure,
                                    Consumer<String> onServerFailure, Consumer<Boolean> loadingSetter,
                                    boolean allowRefresh) {
        setLoadingOnFxThread(loadingSetter, true);

        String sentToken = UserSession.getInstance().getAccessToken();
        boolean hadToken = sentToken != null && !sentToken.isBlank();

        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                return CLIENT.send(buildMultipartRequest(url, files, formField), HttpResponse.BodyHandlers.ofString());
            }
        };

        task.setOnSucceeded(event -> {
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
                        uploadFiles(url, files, formField, onSuccess, onFailure, onServerFailure, loadingSetter, false);
                    } else {
                        setLoadingOnFxThread(loadingSetter, false);
                        forceLogout();
                    }
                }, Platform::runLater);
                return;
            }

            setLoadingOnFxThread(loadingSetter, false);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (onSuccess != null) onSuccess.accept(response);
            } else if (onServerFailure != null) {
                String details = response.body() == null ? "" : response.body().trim();
                if (details.length() > 300) details = details.substring(0, 300) + "...";
                String message = "Errore HTTP " + response.statusCode()
                        + (details.isBlank() ? "" : ": " + details);
                Platform.runLater(() -> onServerFailure.accept(message));
            } else {
                handleFailure(onFailure);
            }
        });

        task.setOnFailed(event -> {
            setLoadingOnFxThread(loadingSetter, false);
            if (onServerFailure != null) {
                Throwable exception = task.getException();
                String details = exception == null
                        ? "causa non disponibile"
                        : exception.getClass().getSimpleName()
                        + (exception.getMessage() == null || exception.getMessage().isBlank()
                        ? ""
                        : ": " + exception.getMessage());
                if (details.length() > 300) details = details.substring(0, 300) + "...";
                String message = "Errore durante la richiesta di upload: " + details;
                Platform.runLater(() -> onServerFailure.accept(message));
            } else {
                handleFailure(onFailure);
            }
        });

        EXECUTOR.execute(task);
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
                        future = refreshFuture;
                        executeRefreshTokenCall();
                    } else {
                        future = refreshFuture;
                    }
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
            } else if (response.statusCode() == 429) {
                int retryAfter = response.headers().firstValue("Retry-After")
                        .map(v -> {
                            try { return Integer.parseInt(v.trim()); }
                            catch (NumberFormatException ex) { return 60; }
                        })
                        .orElse(60);
                if (onRateLimitedGlobal != null) {
                    onRateLimitedGlobal.accept(retryAfter);
                }
                if (onFailure != null) {
                    onFailure.run();
                }
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
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/common/splash-screen.fxml", null);
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

    private static HttpRequest buildMultipartRequest(String url, java.util.List<Path> files, String formField) throws IOException {
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
        String token = UserSession.getInstance().getAccessToken();
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
           // e.printStackTrace();
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