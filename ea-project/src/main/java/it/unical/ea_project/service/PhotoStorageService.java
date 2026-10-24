package it.unical.ea_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Paths;
import java.util.Iterator;

@Service
public class PhotoStorageService {

    private final Path storageRoot;

    public PhotoStorageService(@Value("${app.storage.path:photo}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    public String storeActivityPhoto(Long activityId, Long imageId, MultipartFile file) {
        return storeCover("activity", activityId, imageId, file);
    }

    public String storeTripPhoto(Long tripId, Long imageId, MultipartFile file) {
        return storeCover("trip", tripId, imageId, file);
    }

    /** 
     * Saves a validated user avatar as a normalized JPEG. 
     * Only the authenticated user ID is used in the path. 
     */
    public String storeProfilePhoto(Long userId, MultipartFile file) {

        final long maxBytes = 5L * 1024L * 1024L;
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utente non valido.");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleziona una foto del profilo.");
        }
    
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "La foto del profilo non può superare 5 MB.");
        }

        BufferedImage source;
        try (var inputStream = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {

            if (imageInput == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Il file non è un'immagine supportata. Usa PNG, JPG, GIF o BMP.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Il file non è un'immagine supportata. Usa PNG, JPG, GIF o BMP.");
            }

            ImageReader reader = readers.next();
            try {

                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (width < 1 || height < 1 || width > 8000 || height > 8000 || pixels > 25_000_000L) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La foto ha dimensioni eccessive. Riducila prima di caricarla.");
                }

                source = reader.read(0);
            } finally {
                reader.dispose();
            }

        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossibile leggere la foto del profilo. Usa un'immagine PNG, JPG, GIF o BMP valida.");
        }

        if (source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Il file non è un'immagine supportata. Usa PNG, JPG, GIF o BMP.");
        }

        double scale = Math.min(1.0, 1200.0 / Math.max(source.getWidth(), source.getHeight()));
        int outputWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int outputHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage normalized = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, normalized.getWidth(), normalized.getHeight());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, outputWidth, outputHeight, null);
        graphics.dispose();

        Path profileDirectory = storageRoot.resolve("profile").normalize();
        Path targetFile = profileDirectory.resolve(userId + ".jpg").normalize();
        if (!targetFile.startsWith(storageRoot) || !targetFile.startsWith(profileDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percorso della foto non valido.");
        }

        Path temporaryFile = null;
        try {

            Files.createDirectories(profileDirectory);
            temporaryFile = Files.createTempFile(profileDirectory, userId + "-", ".tmp");
            if (!ImageIO.write(normalized, "jpg", temporaryFile.toFile())) {
                throw new IOException("Nessun encoder JPEG disponibile.");
            }

            try {
                Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException exception) {
            if (temporaryFile != null) {
                try { Files.deleteIfExists(temporaryFile); } catch (IOException ignored) { /* cleanup best effort */ }
            }
            throw new UncheckedIOException("Impossibile salvare la foto del profilo.", exception);
        }

        return "/photo/profile/" + userId + ".jpg";

    }

    public void deleteProfilePhoto(Long userId) {

        if (userId == null || userId <= 0) {
            return;
        }

        Path profileDirectory = storageRoot.resolve("profile").normalize();
        Path targetFile = profileDirectory.resolve(userId + ".jpg").normalize();
        if (!targetFile.startsWith(storageRoot) || !targetFile.startsWith(profileDirectory)) {
            throw new IllegalArgumentException("Percorso della foto non valido.");
        }
        
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException exception) {
            throw new UncheckedIOException("Impossibile eliminare la foto del profilo.", exception);
        }

    }

    public void validatePhoto(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleziona una foto valida.");
        }

        try (var inputStream = file.getInputStream()) {
            if (ImageIO.read(inputStream) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il file caricato non è un'immagine valida.");
            }

        } catch (IOException exception) {
            throw new UncheckedIOException("Impossibile leggere l'immagine caricata.", exception);
        }

    }

    private String storeCover(String entityType, Long entityId, Long imageId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleziona un'immagine.");
        }

        BufferedImage image;
        try (var inputStream = file.getInputStream()) {
            image = ImageIO.read(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Impossibile leggere l'immagine caricata.", exception);
        }

        if (image == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il file caricato non è un'immagine valida.");
        }

        BufferedImage jpegImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = jpegImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();

        Path targetDirectory = storageRoot.resolve(entityType).resolve(entityId.toString()).normalize();
        if (!targetDirectory.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Percorso immagine non valido.");
        }

        Path targetFile = targetDirectory.resolve(imageId + ".jpg");
        try {
            Files.createDirectories(targetDirectory);
            if (!ImageIO.write(jpegImage, "jpg", targetFile.toFile())) {
                throw new IOException("Nessun encoder JPEG disponibile.");
            }

        } catch (IOException exception) {
            throw new UncheckedIOException("Impossibile salvare l'immagine caricata.", exception);
        }

        return "/photo/" + entityType + "/" + entityId + "/" + imageId + ".jpg";
    }
}