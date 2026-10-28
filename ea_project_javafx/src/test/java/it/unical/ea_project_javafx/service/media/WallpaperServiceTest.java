package it.unical.ea_project_javafx.service.media;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WallpaperServiceTest {
    @TempDir
    Path cache;
    private final LocalDate day = LocalDate.of(2026, 10, 27);

    private byte[] photo() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/it/unical/ea_project_javafx/images/experience-fallback.jpg")) {
            return stream.readAllBytes();
        }
    }

    @Test
    void successfulBingDownloadIsReusedWithoutAnotherApiCall() throws Exception {
        var calls = new AtomicInteger();
        byte[] image = photo();
        String selected = WallpaperService.resolveDailyWallpaper(cache, day, url -> {
            calls.incrementAndGet();
            return new ByteArrayInputStream(url.getPath().contains("HPImageArchive")
                    ? "{\"images\":[{\"url\":\"/daily.jpg\"}]}".getBytes(StandardCharsets.UTF_8) : image);
        });
        assertTrue(selected.startsWith(cache.toUri().toString()));
        assertEquals(2, calls.get());
        assertEquals(selected, WallpaperService.resolveDailyWallpaper(cache, day, url -> {
            fail("La cache valida non deve richiamare Bing");
            return null;
        }));
    }

    @Test
    void fallbackIsSelectedOnlyAfterThreeFailedAttempts() {
        var attempts = new AtomicInteger();
        String selected = WallpaperService.resolveDailyWallpaper(cache, day, url -> {
            attempts.incrementAndGet();
            throw new IOException("Offline");
        });
        assertEquals(3, attempts.get());
        assertEquals(WallpaperService.localFallbackUrl(), selected);
    }

    @Test
    void emptyAndMalformedResponsesAreRetriedBeforeBingSucceeds() throws Exception {
        var attempts = new AtomicInteger();
        byte[] image = photo();
        String selected = WallpaperService.resolveDailyWallpaper(cache, day, url -> {
            if (!url.getPath().contains("HPImageArchive"))
                return new ByteArrayInputStream(image);
            return new ByteArrayInputStream(switch (attempts.incrementAndGet()) {
                case 1 -> "{\"images\":[]}".getBytes(StandardCharsets.UTF_8);
                case 2 -> "not-json".getBytes(StandardCharsets.UTF_8);
                default -> "{\"images\":[{\"url\":\"/daily.jpg\"}]}".getBytes(StandardCharsets.UTF_8);
            });
        });
        assertEquals(3, attempts.get());
        assertNotEquals(WallpaperService.localFallbackUrl(), selected);
    }

    @Test
    void corruptCacheAndDownloadsNeverBecomeTheChosenWallpaper() throws Exception {
        Files.writeString(cache.resolve("bing_wallpaper_2026-10-27_bad.jpg"), "not-an-image");
        String selected = WallpaperService.resolveDailyWallpaper(cache, day, url -> new ByteArrayInputStream(
                (url.getPath().contains("HPImageArchive")
                        ? "{\"images\":[{\"url\":\"/daily.jpg\"}]}" : "broken-jpeg").getBytes(StandardCharsets.UTF_8)));
        assertEquals(WallpaperService.localFallbackUrl(), selected);
        try (var files = Files.list(cache)) {
            assertFalse(files.anyMatch(p -> p.toString().endsWith(".part")));
        }
    }
}
