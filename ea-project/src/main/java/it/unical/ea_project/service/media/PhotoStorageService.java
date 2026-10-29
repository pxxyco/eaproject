package it.unical.ea_project.service.media;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhotoStorageService {
    private final Path storageRoot;
    private final Semaphore decoders = new Semaphore(2);
    private static final Set<String> FORMATS = Set.of("png", "jpeg", "jpg", "gif", "bmp");

    public PhotoStorageService(@Value("${app.storage.path:photo}") String storagePath) {
        storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public String storeActivityPhoto(Long activityId, Long imageId, MultipartFile file) {
        return storeOffer("activity", activityId, imageId, file);
    }

    public String storeTripPhoto(Long tripId, Long imageId, MultipartFile file) {
        return storeOffer("trip", tripId, imageId, file);
    }

    private String storeOffer(String type, Long entityId, Long imageId, MultipartFile file) {
        if (entityId == null || entityId <= 0 || imageId == null || imageId <= 0)
            throw new IllegalArgumentException("Identificatore della foto non valido.");
        String relative = type + "/" + entityId + "/" + imageId + ".jpg";
        Path target = storageRoot.resolve(relative);
        store(file, target, 10L * 1024 * 1024, 2400);
        onCompletion(false, () -> delete(target));
        return "/photo/" + relative;
    }

    public String storeProfilePhoto(Long userId, MultipartFile file) {
        if (userId == null || userId <= 0)
            throw new IllegalArgumentException("Utente non valido.");
        String relative = "profile/" + userId + "-" + UUID.randomUUID() + ".jpg";
        Path target = storageRoot.resolve(relative);
        store(file, target, 5L * 1024 * 1024, 1200);
        onCompletion(false, () -> delete(target));
        return "/photo/" + relative;
    }

    public void deleteProfilePhoto(Long userId, String url) {
        if (userId != null && userId > 0 && url != null
                && url.matches("/photo/profile/" + userId + "(?:-[a-f0-9-]{36})?\\.jpg(?:\\?.*)?")) {
            Path target = storageRoot.resolve(url.substring("/photo/".length()).split("\\?", 2)[0]);
            onCompletion(true, () -> delete(target));
        }
    }

    public void deleteOfferPhoto(String url) {
        if (url != null && url.matches("/photo/(activity|trip)/[0-9]+/[0-9]+\\.jpg")) {
            Path path = storageRoot.resolve(url.substring("/photo/".length()));
            onCompletion(true, () -> delete(path));
        }
    }

    public void validatePhotos(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > 10)
            throw new IllegalArgumentException("Seleziona da una a dieci foto.");
        files.forEach(this::validatePhoto);
    }

    public void validatePhoto(MultipartFile file) {
        checkSize(file, 10L * 1024 * 1024);
        try (var input = file.getInputStream(); var image = ImageIO.createImageInputStream(input)) {
            ImageReader reader = reader(image);
            try {
                checkDimensions(reader);
            } finally {
                reader.dispose();
            }
        } catch (IOException error) {
            throw invalidImage();
        }
    }

    private void store(MultipartFile file, Path target, long maxBytes, int maxSide) {
        checkSize(file, maxBytes);
        if (!decoders.tryAcquire())
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Elaborazione foto occupata. Riprova tra poco.");
        Path temporary = null;
        try {
            BufferedImage normalized;
            try (var input = file.getInputStream(); var image = ImageIO.createImageInputStream(input)) {
                ImageReader reader = reader(image);
                BufferedImage source;
                try {
                    checkDimensions(reader);
                    int sample = Math.max(1, Math.max(reader.getWidth(0), reader.getHeight(0)) / maxSide);
                    var options = reader.getDefaultReadParam();
                    options.setSourceSubsampling(sample, sample, 0, 0);
                    source = reader.read(0, options);
                } finally {
                    reader.dispose();
                }
                if (source == null)
                    throw invalidImage();
                double scale = Math.min(1, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
                int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
                normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                var graphics = normalized.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(source, 0, 0, width, height, null);
                } finally {
                    graphics.dispose();
                }
            }
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            if (!ImageIO.write(normalized, "jpg", temporary.toFile()))
                throw new IOException("Encoder JPEG assente.");
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            throw invalidImage();
        } finally {
            try {
                if (temporary != null)
                    delete(temporary);
            } finally {
                decoders.release();
            }
        }
    }

    private static ImageReader reader(ImageInputStream image) throws IOException {
        if (image == null)
            throw invalidImage();
        var readers = ImageIO.getImageReaders(image);
        if (!readers.hasNext())
            throw invalidImage();
        ImageReader reader = readers.next();
        if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
            reader.dispose();
            throw invalidImage();
        }
        reader.setInput(image, true, true);
        return reader;
    }

    private static void checkDimensions(ImageReader reader) throws IOException {
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1 || height < 1 || width > 8000 || height > 8000 || (long) width * height > 25_000_000)
            throw new IllegalArgumentException("La foto ha dimensioni eccessive. Riducila prima di caricarla.");
    }

    private static void checkSize(MultipartFile file, long max) {
        if (file == null || file.isEmpty())
            throw invalidImage();
        if (file.getSize() > max)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La foto supera il limite consentito.");
    }

    private static ResponseStatusException invalidImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa un'immagine PNG, JPEG, GIF o BMP valida.");
    }

    private static void onCompletion(boolean committed, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (committed)
                action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == committed)
                    action.run();
            }
        });
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException error) {
            throw new UncheckedIOException("Impossibile eliminare il file della foto.", error);
        }
    }
}
