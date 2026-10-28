package it.unical.ea_project_javafx.service.api;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiServiceMultipartTest {
    @TempDir
    Path directory;

    private HttpRequest request(Path file) throws Exception {
        Method method = ApiService.class.getDeclaredMethod("buildMultipartRequest", String.class, List.class,
                String.class, String.class, Map.class);
        method.setAccessible(true);
        return (HttpRequest) method.invoke(null, "http://localhost/api/chat/conversations/1/photos",
                List.of(file), "file", "test-token", Map.of("clientId", "test-client"));
    }

    @Test
    void streamingUploadPreservesMultipartFieldsAndFileContents() throws Exception {
        Path file = Files.writeString(directory.resolve("photo.png"), "sample-photo");
        HttpRequest request = request(file);
        String boundary = request.headers().firstValue("Content-Type").orElseThrow().split("boundary=")[1];
        var result = new CompletableFuture<byte[]>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
            }
            public void onError(Throwable error) { result.completeExceptionally(error); }
            public void onComplete() { result.complete(bytes.toByteArray()); }
        });
        byte[] body = result.get(5, TimeUnit.SECONDS);
        String text = new String(body, StandardCharsets.UTF_8);
        assertTrue(text.contains("name=\"clientId\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\ntest-client\r\n"));
        assertTrue(text.contains("name=\"file\"; filename=\"photo.png\"\r\nContent-Type: application/octet-stream\r\n\r\nsample-photo\r\n"));
        assertTrue(text.endsWith("--" + boundary + "--\r\n"));
        assertEquals(body.length, request.bodyPublisher().orElseThrow().contentLength());
        assertEquals("Bearer test-token", request.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void largeFileDoesNotNeedToBeReadWhenBuildingRequest() throws Exception {
        Path file = directory.resolve("large.png");
        try (var channel = java.nio.channels.FileChannel.open(file,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
            channel.position(128L * 1024 * 1024 - 1);
            channel.write(ByteBuffer.wrap(new byte[] {0}));
        }
        assertTrue(request(file).bodyPublisher().orElseThrow().contentLength() > Files.size(file));
    }
}
