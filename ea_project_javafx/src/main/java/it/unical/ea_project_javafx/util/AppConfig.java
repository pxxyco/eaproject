package it.unical.ea_project_javafx.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javafx.scene.image.Image;

public final class AppConfig {

    private AppConfig() {
    }

    public static final String APP_TITLE = "Itinera [Dev Build " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "]";

    public static final Image APP_ICON = new Image(
        Objects.requireNonNull(
            AppConfig.class.getResourceAsStream("/it/unical/ea_project_javafx/images/logo_448x448.png")
        )
    );

    public static final double WINDOW_WIDTH = 1280.0;
    public static final double WINDOW_HEIGHT = 768.0;

    public static final double MIN_WINDOW_WIDTH = 1024.0;
    public static final double MIN_WINDOW_HEIGHT = 768.0;

}
