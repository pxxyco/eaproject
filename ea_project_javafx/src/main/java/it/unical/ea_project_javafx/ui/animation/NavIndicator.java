package it.unical.ea_project_javafx.ui.animation;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Animation;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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

public final class NavIndicator {
    private static final Object KEY = new Object();
    private final Region line = new Region();
    private final Timeline motion = new Timeline();
    private boolean positioned;
    private final DoubleProperty length = new SimpleDoubleProperty(18);
    private double targetX = Double.NaN, targetY = Double.NaN;

    private NavIndicator(Scene scene) {
        line.setId("navIndicator");
        scene.widthProperty().addListener((o, a, b) -> Platform.runLater(() -> move(scene)));
        scene.heightProperty().addListener((o, a, b) -> Platform.runLater(() -> move(scene)));
        line.setManaged(false);
        line.setMouseTransparent(true);
        line.setViewOrder(-10);
        line.resize(18, 2);
        length.addListener((o, a, b) -> line.resize(b.doubleValue(), 2));
    }

    public static void select(Scene scene) {
        if (scene == null)
            return;
        NavIndicator indicator = (NavIndicator) scene.getProperties().computeIfAbsent(KEY,
                key -> new NavIndicator(scene));
        Platform.runLater(() -> indicator.move(scene));
    }

    public static void preview(Label label) {
        Scene scene = label.getScene();
        if (scene == null)
            return;
        var active = PseudoClass.getPseudoClass("active");
        scene.getRoot().lookupAll(".nav-item").forEach(node -> node.pseudoClassStateChanged(active, node == label));
        NavIndicator indicator = (NavIndicator) scene.getProperties().computeIfAbsent(KEY,
                key -> new NavIndicator(scene));
        indicator.move(scene);
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
        if (motion.getStatus() == Animation.Status.RUNNING && Math.abs(targetX - x) < .5 && Math.abs(targetY - y) < .5)
            return;
        motion.stop();
        targetX = x;
        targetY = y;
        if (!positioned) {
            line.setLayoutX(x);
            line.setLayoutY(y);
            positioned = true;
        }
        double left = Math.min(line.getLayoutX(), x), stretch = Math.abs(line.getLayoutX() - x) + 18;
        motion.getKeyFrames().setAll(
                new KeyFrame(Duration.millis(280),
                        new KeyValue(line.layoutXProperty(), left, Interpolator.SPLINE(.55, 0, .8, .65)),
                        new KeyValue(length, stretch, Interpolator.SPLINE(.55, 0, .8, .65)),
                        new KeyValue(line.opacityProperty(), .95)),
                new KeyFrame(Duration.millis(850),
                        new KeyValue(line.layoutXProperty(), x, Interpolator.SPLINE(.15, .5, .2, 1)),
                        new KeyValue(line.layoutYProperty(), y, Interpolator.EASE_BOTH),
                        new KeyValue(length, 18, Interpolator.SPLINE(.15, .5, .2, 1))));
        motion.playFromStart();
    }
}
