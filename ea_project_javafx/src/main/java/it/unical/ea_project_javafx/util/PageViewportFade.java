package it.unical.ea_project_javafx.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;

/** Gentle top fade for pages with a stationary header. */
final class PageViewportFade {
    static void install(ScrollPane pane) {
        final Rectangle mask = new Rectangle();
        final Node[] viewport = { null }, original = { null };
        Runnable refresh = () -> {
            if (pane.getScene() == null)
                return;
            if (viewport[0] == null) {
                viewport[0] = pane.lookup(".viewport");
                if (viewport[0] != null)
                    original[0] = viewport[0].getClip();
            }
            if (viewport[0] == null)
                return;
            if (pane.getVvalue() <= pane.getVmin() + .00001) {
                viewport[0].setClip(original[0]);
                return;
            }
            mask.setWidth(pane.getViewportBounds().getWidth());
            mask.setHeight(pane.getViewportBounds().getHeight());
            if (mask.getFill() == Color.BLACK)
                mask.setFill(new LinearGradient(0, 0, 0, 26, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.TRANSPARENT), new Stop(1, Color.WHITE)));
            viewport[0].setClip(mask);
        };
        pane.vvalueProperty().addListener((o, a, b) -> refresh.run());
        pane.viewportBoundsProperty().addListener((o, a, b) -> refresh.run());
        pane.sceneProperty().addListener((o, a, b) -> {
            if (b != null)
                Platform.runLater(refresh);
        });
        Platform.runLater(refresh);
    }
}
