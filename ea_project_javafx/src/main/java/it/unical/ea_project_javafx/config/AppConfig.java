package it.unical.ea_project_javafx.config;

import java.util.Objects;
import javafx.scene.image.Image;

public final class AppConfig {

    private AppConfig() {
    }

    public static final String APP_TITLE = "EA Project - Beta Build";

    public static final Image APP_ICON = new Image(
            Objects.requireNonNull(
                    AppConfig.class.getResourceAsStream("/it/unical/ea_project_javafx/images/monogram_icon.png")));

    public static final double WINDOW_WIDTH = 1280.0;
    public static final double WINDOW_HEIGHT = 768.0;

    public static final double MIN_WINDOW_WIDTH = 1024.0;
    public static final double MIN_WINDOW_HEIGHT = 768.0;

}
