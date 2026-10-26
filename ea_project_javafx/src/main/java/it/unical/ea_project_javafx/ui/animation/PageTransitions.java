package it.unical.ea_project_javafx.ui.animation;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public final class PageTransitions {
    private PageTransitions() {
    }

    public static void enter(Parent root) {
        Node content = root.lookup("#authCard");
        if (content == null)
            content = center(root);
        if (content == null)
            return;
        FadeTransition fade = new FadeTransition(Duration.millis(180), content);
        content.setOpacity(.72);
        fade.setToValue(1);
        fade.play();
    }

    private static Node center(Parent parent) {
        if (parent instanceof BorderPane pane && pane.getCenter() != null)
            return pane.getCenter();
        for (Node node : parent.getChildrenUnmodifiable())
            if (node instanceof Parent child) {
                Node result = center(child);
                if (result != null)
                    return result;
            }
        return null;
    }

    public static void appear(Node node) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setToValue(1);
        fade.play();
    }
}
