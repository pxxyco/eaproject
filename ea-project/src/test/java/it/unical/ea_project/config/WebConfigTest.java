package it.unical.ea_project.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class WebConfigTest {
    @TempDir
    Path directory;

    @Test
    void recoversAnEmptyDefaultPhotoAfterInterruptedInitialization() throws Exception {
        Path image = directory.resolve("default/experience.jpg");
        Files.createDirectories(image.getParent());
        Files.createFile(image);
        var config = new WebConfig();
        ReflectionTestUtils.setField(config, "storagePath", directory.toString());
        config.addResourceHandlers(
                new ResourceHandlerRegistry(new StaticWebApplicationContext(), new MockServletContext()));
        try (var input = new ClassPathResource("static/experience.jpg").getInputStream()) {
            assertArrayEquals(input.readAllBytes(), Files.readAllBytes(image));
        }
    }
}
