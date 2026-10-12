package it.unical.ea_project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Path relativo alla root di esecuzione dell'applicazione
        Path baseDir = Paths.get("photo").toAbsolutePath();

        // Creazione cartelle
        createDir(baseDir.resolve("activity"));
        createDir(baseDir.resolve("default"));
        createDir(baseDir.resolve("trip"));

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
        if (Files.notExists(targetPath)) {
            try {
                ClassPathResource resource = new ClassPathResource(resourcePath);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Copiata risorsa di default in: {}", targetPath);
                    }
                }
            } catch (Exception e) {
                log.error("Errore durante la copia della risorsa di default: {}", e.getMessage());
            }
        }
    }
}