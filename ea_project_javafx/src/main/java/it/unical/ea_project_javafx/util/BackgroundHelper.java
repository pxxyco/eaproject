package it.unical.ea_project_javafx.util;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Path;

import javafx.application.Platform;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;


public class BackgroundHelper {

    private BackgroundHelper() {
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "background-wallpaper-loader");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Carica lo sfondo del giorno in modo asincrono e lo adatta alle dimensioni del contenitore StackPane.
     *
     * @param bgImageView L'ImageView in cui caricare lo sfondo.
     */
    public static void setupBackground(ImageView bgImageView) {

        if (bgImageView == null) {
            return;
        }

        bgImageView.setEffect(new GaussianBlur(18));

        setupBackgroundResize(bgImageView);

        Optional<Path> cachedPath = WallpaperService.getTodayCachedWallpaper();

        if (cachedPath.isPresent()) {
            Image cachedImage = new Image(
                cachedPath.get().toUri().toString(), 
                1920, 
                1080, 
                true, 
                true, 
                false
            );
            bgImageView.setImage(cachedImage);
        } else {
            //  Manda la task al thread pool
            EXECUTOR.submit(() -> {
                String imageUrl = WallpaperService.getDailyWallpaperUrl();
                Platform.runLater(() -> bgImageView.setImage(new Image(
                        imageUrl,
                        1920,
                        1080,
                        true,
                        true,
                        false
                )));
            });
        }

    }

    /**
     * Associa larghezza e altezza dell'ImageView al suo StackPane genitore.
     */
    private static void setupBackgroundResize(ImageView bgImageView) {

        bgImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && bgImageView.getParent() instanceof StackPane parentPane) {
                bgImageView.fitWidthProperty().bind(parentPane.widthProperty());
                bgImageView.fitHeightProperty().bind(parentPane.heightProperty());
            }
        });

    }

}
