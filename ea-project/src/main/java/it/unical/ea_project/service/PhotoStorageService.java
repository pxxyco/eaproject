package it.unical.ea_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
