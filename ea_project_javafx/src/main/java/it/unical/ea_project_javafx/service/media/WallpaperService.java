package it.unical.ea_project_javafx.service.media;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;
import javax.imageio.ImageIO;

public class WallpaperService {

    private static final String BING_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=it-IT";
    private static final String LOCAL_FALLBACK_RESOURCE = "/it/unical/ea_project_javafx/images/experience-fallback.jpg";
    private static final String CACHE_FILE_PREFIX = "bing_wallpaper_";
    private static final String CACHE_FILE_SUFFIX = ".jpg";

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;

    @FunctionalInterface
    interface StreamOpener {
        InputStream open(URL url) throws IOException;
    }

    public static synchronized String getDailyWallpaperUrl() {
        return resolveDailyWallpaper(getSystemCacheDir(), LocalDate.now(), WallpaperService::openWithTimeout);
    }

    static String resolveDailyWallpaper(Path cacheDir, LocalDate day, StreamOpener opener) {
        String todayStr = day.toString();
        String todayPrefix = CACHE_FILE_PREFIX + todayStr + "_";
        Optional<Path> cached = findCachedWallpaper(cacheDir, todayPrefix);
        if (cached.isPresent())
            return cached.get().toUri().toString();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Path temporaryFile = null;
            try {
                Files.createDirectories(cacheDir);
                String relativeUrl;
                try (InputStream stream = opener.open(URI.create(BING_API_URL).toURL())) {
                    byte[] body = stream.readNBytes(64 * 1024 + 1);
                    if (body.length > 64 * 1024)
                        throw new IOException("Risposta Bing troppo grande.");
                    JsonObject json = JsonParser.parseString(new String(body, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    JsonArray images = json.getAsJsonArray("images");
                    if (images == null || images.isEmpty())
                        throw new IOException("Bing non ha restituito immagini.");
                    relativeUrl = images.get(0).getAsJsonObject().get("url").getAsString();
                }
                if (relativeUrl == null || !relativeUrl.startsWith("/") || relativeUrl.startsWith("//"))
                    throw new IOException("URL dello sfondo Bing non valido.");
                String imageUrl = "https://www.bing.com" + relativeUrl;
                Path targetFile = cacheDir.resolve(todayPrefix + hashUrl(imageUrl) + CACHE_FILE_SUFFIX);
                temporaryFile = Files.createTempFile(cacheDir, "bing-download-", ".part");
                try (InputStream stream = opener.open(URI.create(imageUrl).toURL());
                        var output = Files.newOutputStream(temporaryFile)) {
                    byte[] buffer = new byte[8192];
                    int total = 0, length;
                    while ((length = stream.read(buffer)) != -1) {
                        total += length;
                        if (total > MAX_IMAGE_BYTES)
                            throw new IOException("Sfondo Bing troppo grande.");
                        output.write(buffer, 0, length);
                    }
                }
                if (!validImage(temporaryFile))
                    throw new IOException("Immagine Bing vuota o danneggiata.");
                try {
                    Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                cleanOldCacheFiles(cacheDir, todayStr);
                return targetFile.toUri().toString();
            } catch (Exception error) {
                System.err.println("Sfondo Bing: tentativo " + attempt + "/" + MAX_ATTEMPTS
                        + " fallito: " + error.getMessage());
            } finally {
                if (temporaryFile != null)
                    try {
                        Files.deleteIfExists(temporaryFile);
                    } catch (IOException error) {
                        System.err.println("Impossibile eliminare il download temporaneo: " + error.getMessage());
                    }
            }
        }
        return localFallbackUrl();
    }

    public static String localFallbackUrl() {
        URL resource = WallpaperService.class.getResource(LOCAL_FALLBACK_RESOURCE);
        if (resource == null)
            throw new IllegalStateException("Sfondo di fallback mancante.");
        return resource.toExternalForm();
    }

    private static boolean validImage(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) == 0 || Files.size(path) > MAX_IMAGE_BYTES)
                return false;
            try (var input = ImageIO.createImageInputStream(path.toFile())) {
                if (input == null)
                    return false;
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext())
                    return false;
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    return width > 0 && height > 0 && (long) width * height <= 16_000_000
                            && reader.read(0) != null;
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | RuntimeException invalid) {
            return false;
        }
    }

    /**
     * Restituisce il percorso dell'immagine di oggi se è già presente nella cache
     * locale.
     */
    public static Optional<Path> getTodayCachedWallpaper() {

        String todayPrefix = CACHE_FILE_PREFIX + LocalDate.now().toString() + "_";

        return findCachedWallpaper(getSystemCacheDir(), todayPrefix);

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
        } catch (DirectoryIteratorException | IOException e) {
            System.err.println("Impossibile pulire la cache degli sfondi: " + e.getMessage());
        }

    }

    private static Optional<Path> findCachedWallpaper(Path cacheDir, String todayPrefix) {

        if (!Files.isDirectory(cacheDir)) {
            return Optional.empty();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, todayPrefix + "*" + CACHE_FILE_SUFFIX)) {
            for (Path entry : stream) {
                if (validImage(entry)) {
                    return Optional.of(entry);
                }
            }
        } catch (DirectoryIteratorException | IOException e) {
            System.err.println("Impossibile leggere la cache degli sfondi: " + e.getMessage());
        }

        return Optional.empty();

    }

    private static InputStream openWithTimeout(URL url) throws IOException {
        var connection = url.openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(3000);
        return connection.getInputStream();
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
