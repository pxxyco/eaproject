package it.unical.ea_project.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${app.storage.path:photo}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Usa lo stesso percorso configurato dal servizio che salva le immagini.
        Path baseDir = Paths.get(storagePath).toAbsolutePath().normalize();

        // Creazione cartelle
        createDir(baseDir.resolve("activity"));
        createDir(baseDir.resolve("default"));
        createDir(baseDir.resolve("trip"));
        createDir(baseDir.resolve("profile"));

        // Inserimento immagine di default
        copyDefaultResource("static/experience.jpg", baseDir.resolve("default/experience.jpg"));

        // Registrazione handler per servire i file
        registry.addResourceHandler("/photo/**")
                .addResourceLocations(baseDir.toUri().toString());

        log.info("Servizio foto attivo sulla cartella: {}", baseDir);
    }

    private void createDir(Path dirPath) {
        try {
            if (Files.notExists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("Creata cartella: {}", dirPath);
            }
        } catch (Exception e) {
            log.error("Impossibile creare la cartella {}: {}", dirPath, e.getMessage());
        }
    }

    private void copyDefaultResource(String resourcePath, Path targetPath) {
        Path temporary = null;
        try {
            if (Files.exists(targetPath) && Files.size(targetPath) > 0)
                return;
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists())
                throw new IllegalStateException("Immagine predefinita assente dal pacchetto.");
            temporary = Files.createTempFile(targetPath.getParent(), "default-", ".tmp");
            try (InputStream input = resource.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Inizializzata immagine predefinita: {}", targetPath);
        } catch (Exception failure) {
            throw new IllegalStateException("Impossibile inizializzare l'immagine predefinita.", failure);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
