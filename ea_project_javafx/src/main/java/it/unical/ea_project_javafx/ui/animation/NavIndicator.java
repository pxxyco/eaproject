package it.unical.ea_project_javafx.ui.animation;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

/**
 * One indicator per scene, preserving its previous position across navigation.
 */
public final class NavIndicator {
    private static final Object KEY = new Object();
    private final Region line = new Region();
    private final Timeline motion = new Timeline();
    private boolean positioned;

    private NavIndicator(Scene scene) {
        line.setId("navIndicator");
        scene.widthProperty().addListener((o, a, b) -> Platform.runLater(() -> move(scene)));
        scene.heightProperty().addListener((o, a, b) -> Platform.runLater(() -> move(scene)));
        line.setManaged(false);
        line.setMouseTransparent(true);
        line.setViewOrder(-10);
        line.resize(18, 2);
    }

    public static void select(Scene scene) {
        if (scene == null)
            return;
        NavIndicator indicator = (NavIndicator) scene.getProperties().computeIfAbsent(KEY,
                key -> new NavIndicator(scene));
        Platform.runLater(() -> indicator.move(scene));
    }

    private void move(Scene scene) {
        if (!(scene.getRoot() instanceof Pane root))
            return;
        root.applyCss();
        root.layout();
        Label selected = root.lookupAll(".nav-item").stream().filter(n -> n instanceof Label
                && n.getPseudoClassStates().contains(PseudoClass.getPseudoClass("active")))
                .map(n -> (Label) n).findFirst().orElse(null);
        if (line.getParent() instanceof Pane old && old != root)
            old.getChildren().remove(line);
        if (!root.getChildren().contains(line))
            root.getChildren().add(line);
        if (selected == null) {
            line.setOpacity(0);
            return;
        }
        var bounds = root.sceneToLocal(selected.localToScene(selected.getLayoutBounds()));
        double x = (bounds.getMinX() + bounds.getMaxX()) / 2 - 9, y = bounds.getMaxY() - 3;
        line.setBackground(new Background(
                new BackgroundFill(UiEffects.accent(), new CornerRadii(2), Insets.EMPTY)));
        motion.stop();
        if (!positioned) {
            line.setLayoutX(x);
            line.setLayoutY(y);
            positioned = true;
        }
        motion.getKeyFrames().setAll(new KeyFrame(Duration.millis(280),
                new KeyValue(line.layoutXProperty(), x, Interpolator.EASE_BOTH),
                new KeyValue(line.layoutYProperty(), y, Interpolator.EASE_BOTH),
                new KeyValue(line.opacityProperty(), .9)));
        motion.playFromStart();
    }
}
