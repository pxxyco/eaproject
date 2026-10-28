package it.unical.ea_project.service.chat;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
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

/**
 * Private chat media: validates decoded format and dimensions, strips metadata
 * and bounds output.
 */
@Service
public class ChatPhotoStorage {
    private final Semaphore decoders = new Semaphore(2);
    private final Path directory;

    public ChatPhotoStorage(@Value("${app.storage.path:photo}") String publicDirectory,
            @Value("${app.chat.storage.path:}") String configured) {
        Path publicPath = Path.of(publicDirectory).toAbsolutePath().normalize();
        directory = (configured.isBlank() ? Path.of(publicPath + "-private/chat") : Path.of(configured))
                .toAbsolutePath().normalize();
        if (directory.startsWith(publicPath))
            throw new IllegalArgumentException("Le foto chat devono essere private.");
    }

    public String hash(MultipartFile file) {
        if (file.isEmpty())
            throw new IllegalArgumentException("Scegli una foto PNG, JPEG, GIF o BMP entro 8 MB.");
        if (file.getSize() > 8 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La foto supera il limite di 8 MB.");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossibile leggere la foto.");
        }
    }

    public String store(MultipartFile file) {
        if (!decoders.tryAcquire())
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Elaborazione foto occupata. Riprova fra qualche secondo.");
        try {
            return normalize(file);
        } finally {
            decoders.release();
        }
    }

    private String normalize(MultipartFile file) {
        String key = UUID.randomUUID() + ".jpg";
        Path destination = directory.resolve(key);
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                throw new IllegalArgumentException("Formato immagine non supportato.");
            ImageReader reader = readers.next();
            BufferedImage source;
            try {
                if (!Set.of("JPEG", "PNG", "GIF", "BMP").contains(reader.getFormatName().toUpperCase()))
                    throw new IllegalArgumentException("Formato immagine non supportato.");
                reader.setInput(input, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > 10000 || h > 10000 || (long) w * h > 24_000_000)
                    throw new IllegalArgumentException("Dimensioni della foto eccessive (massimo 24 megapixel).");
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
            double scale = Math.min(1, 1600.0 / Math.max(source.getWidth(), source.getHeight()));
            BufferedImage output = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)),
                    Math.max(1, (int) (source.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
            var graphics = output.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(source, 0, 0, output.getWidth(), output.getHeight(), null);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(output, "jpg", bytes);
            Files.createDirectories(directory);
            Files.write(destination, bytes.toByteArray());
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                Files.deleteIfExists(destination);
                            } catch (IOException ignored) {
                            }
                        }
                    }
                });
            }
            return key;
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {
            }
            throw new IllegalArgumentException("La foto è danneggiata o non può essere salvata.");
        }
    }

    public String read(String key) {
        if (!key.matches("[a-f0-9-]{36}\\.jpg"))
            throw new IllegalArgumentException("Foto non valida.");
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(directory.resolve(key)));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto non disponibile.");
        }
    }
}
