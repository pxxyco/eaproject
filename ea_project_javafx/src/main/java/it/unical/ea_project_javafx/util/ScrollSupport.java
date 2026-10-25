package it.unical.ea_project_javafx.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

public final class ScrollSupport {
    private ScrollSupport() {
    }

    public static void reveal(Node target) {
        reveal(target, 20);
    }

    public static void reveal(Node target, double inset) {
        Platform.runLater(() -> {
            for (var parent = target.getParent(); parent != null; parent = parent.getParent()) {
                if (parent instanceof ScrollPane scroll) {
                    scroll.applyCss();
                    scroll.layout();
                    var content = scroll.getContent();
                    var bounds = content.sceneToLocal(target.localToScene(target.getBoundsInLocal()));
                    double range = content.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight();
                    if (range > 0)
                        scroll.setVvalue(scroll.getVmin() + (scroll.getVmax() - scroll.getVmin())
                                * Math.max(0, Math.min(1, (bounds.getMinY() - inset) / range)));
                    return;
                }
            }
        });
    }
}
