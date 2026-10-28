package it.unical.ea_project_javafx.ui.background;

import it.unical.ea_project_javafx.service.media.WallpaperService;
import it.unical.ea_project_javafx.ui.animation.UiEffects;
import it.unical.ea_project_javafx.ui.scroll.SmoothScrolling;
import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
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
     * Carica lo sfondo del giorno in modo asincrono e lo adatta alle dimensioni del
     * contenitore StackPane.
     *
     * @param bgImageView L'ImageView in cui caricare lo sfondo.
     */
    public static void setupBackground(ImageView bgImageView) {

        if (bgImageView == null) {
            return;
        }

        bgImageView.setEffect(new GaussianBlur(18));
        bgImageView.setOpacity(1);
        bgImageView.setCache(true);
        bgImageView.setCacheHint(CacheHint.QUALITY);
        bgImageView.setMouseTransparent(true);

        setupBackgroundResize(bgImageView);

        var future = sharedBackground();
        Image ready = future.isCompletedExceptionally() ? null : future.getNow(null);
        if (ready != null && !ready.isError())
            bgImageView.setImage(ready);
        else if (lastImage != null && !WallpaperService.localFallbackUrl().equals(lastImage.getUrl()))
            bgImageView.setImage(lastImage);
        var target = new WeakReference<>(bgImageView);
        future.whenComplete((image, error) -> Platform.runLater(() -> {
            ImageView view = target.get();
            if (view != null && error == null && image != null && !image.isError()) {
                lastImage = image;
                view.setImage(image);
                UiEffects.useWallpaper(view.getScene(), image);
            }
        }));
    }

    private static Image lastImage;
    private static CompletableFuture<Image> sharedImage;
    private static LocalDate imageDay;

    public static CompletableFuture<Void> prepareBackground() {
        return sharedBackground().thenApply(image -> null);
    }

    private static synchronized CompletableFuture<Image> sharedBackground() {
        if (sharedImage == null || !LocalDate.now().equals(imageDay)) {
            imageDay = LocalDate.now();
            sharedImage = CompletableFuture.supplyAsync(() -> {
                String url = WallpaperService.getDailyWallpaperUrl();
                Image image = new Image(url, 1920, 1080, true, true, false);
                if (image.isError())
                    throw new IllegalStateException("Impossibile decodificare lo sfondo selezionato.",
                            image.getException());
                return image;
            }, EXECUTOR);
        }

        return sharedImage;
    }

    /**
     * Associa larghezza e altezza dell'ImageView al suo StackPane genitore.
     */
    private static void setupBackgroundResize(ImageView bgImageView) {

        Runnable configure = () -> {
            if (!bgImageView.isVisible())
                return;
            if (!(bgImageView.getParent() instanceof StackPane root))
                return;
            if (!root.getStyleClass().contains("app-root"))
                root.getStyleClass().add("app-root");
            Region shade = null;
            for (var node : root.getChildren()) {
                if (node instanceof Region region && region.getClass() == Region.class
                        && region.getStyleClass().stream()
                                .anyMatch(c -> c.endsWith("overlay") || c.equals("app-background-shade"))) {
                    shade = region;
                    break;
                }
            }
            if (shade == null) {
                shade = new Region();
                root.getChildren().add(root.getChildren().indexOf(bgImageView) + 1, shade);
            }
            shade.getStyleClass().setAll("app-background-shade");
            shade.setMouseTransparent(true);
            bgImageView.fitWidthProperty().bind(root.widthProperty());
            bgImageView.fitHeightProperty().bind(root.heightProperty());
        };
        configure.run();
        bgImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                configure.run();
                SmoothScrolling.install(newScene);
                UiEffects.useWallpaper(newScene, bgImageView.getImage());
            }
        });
    }
}
