package it.unical.ea_project_javafx.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

public class WallpaperService {

    private static final String BING_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=it-IT";
    private static final String LOCAL_FALLBACK_RESOURCE = "/it/unical/ea_project_javafx/images/fallback_bg.avif";
    private static final String CACHE_FILE_PREFIX = "bing_wallpaper_";
    private static final String CACHE_FILE_SUFFIX = ".jpg";

    public static synchronized String getDailyWallpaperUrl() {

        String todayStr = LocalDate.now().toString();
        Path cacheDir = getSystemCacheDir();
        String todayPrefix = CACHE_FILE_PREFIX + todayStr + "_";

        try {
            Files.createDirectories(cacheDir);
            cleanOldCacheFiles(cacheDir, todayStr);

            URL apiUrl = URI.create(BING_API_URL).toURL();
            try (InputStream is = apiUrl.openStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray imagesArray = jsonObject.getAsJsonArray("images");

                if (imagesArray != null && imagesArray.size() > 0) {
                    JsonObject firstImage = imagesArray.get(0).getAsJsonObject();
                    String relativeUrl = firstImage.get("url").getAsString();
                    String bingImageUrl = "https://www.bing.com" + relativeUrl;
                    Path targetFile = cacheDir.resolve(todayPrefix + hashUrl(bingImageUrl) + CACHE_FILE_SUFFIX);

                    if (!Files.exists(targetFile)) {
                        Path temporaryFile = cacheDir.resolve(targetFile.getFileName() + ".part");
                        try (InputStream imageStream = URI.create(bingImageUrl).toURL().openStream()) {
                            Files.copy(imageStream, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                        try {
                            Files.move(temporaryFile, targetFile,
                                    StandardCopyOption.ATOMIC_MOVE,
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (AtomicMoveNotSupportedException e) {
                            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }

                    return targetFile.toUri().toString();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossibile scaricare lo sfondo da Bing, uso il fallback locale: " + e.getMessage());
        }

        Optional<Path> cachedWallpaper = findCachedWallpaper(cacheDir, todayPrefix);
        if (cachedWallpaper.isPresent()) {
            return cachedWallpaper.get().toUri().toString();
        }

        URL resourceUrl = WallpaperService.class.getResource(LOCAL_FALLBACK_RESOURCE);
        if (resourceUrl != null) {
            return resourceUrl.toExternalForm();
        }

        return "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1920&q=80";

    }

    /**
     * Elimina i vecchi file di sfondo salvati in precedenza.
     */
    private static void cleanOldCacheFiles(Path cacheDir, String todayStr) {

        String todayPrefix = CACHE_FILE_PREFIX + todayStr + "_";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, CACHE_FILE_PREFIX + "*.jpg")) {
            for (Path entry : stream) {
                if (!entry.getFileName().toString().startsWith(todayPrefix)) {
                    Files.deleteIfExists(entry);
                }
            }
        } catch (DirectoryIteratorException | java.io.IOException e) {
            System.err.println("Impossibile pulire la cache degli sfondi: " + e.getMessage());
        }

    }

    private static Optional<Path> findCachedWallpaper(Path cacheDir, String todayPrefix) {

        if (!Files.isDirectory(cacheDir)) {
            return Optional.empty();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, todayPrefix + "*" + CACHE_FILE_SUFFIX)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    return Optional.of(entry);
                }
            }
        } catch (DirectoryIteratorException | java.io.IOException e) {
            System.err.println("Impossibile leggere la cache degli sfondi: " + e.getMessage());
        }

        return Optional.empty();

    }

    private static String hashUrl(String url) {

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 non disponibile", e);
        }

    }

    /**
     * Ottiene la directory temporanea corretta in base al sistema operativo in uso.
     */
    private static Path getSystemCacheDir() {

        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {

            // Windows usa %LOCALAPPDATA%
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                return Paths.get(localAppData, "ea_project", "cache");
            }
            return Paths.get(userHome, "AppData", "Local", "ea_project", "cache");

        } else if (os.contains("mac")) {

            // macOS usa ~/Library/Caches/
            return Paths.get(userHome, "Library", "Caches", "ea_project");

        } else {

            // Linux/Unix usa ~/.cache/
            String xdgCache = System.getenv("XDG_CACHE_HOME");
            if (xdgCache != null) {
                return Paths.get(xdgCache, "ea_project");
            }
            return Paths.get(userHome, ".cache", "ea_project");

        }

    }

}