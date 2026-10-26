package it.unical.ea_project_javafx.ui.animation;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

public final class PageTransitions {
    private static final String FADE = PageTransitions.class.getName() + ".fade";

    private PageTransitions() {
    }

    public static void enter(Parent root) {
        Node content = root.lookup("#authCard");
        if (content == null)
            content = center(root);
        if (content == null)
            return;
        if (content.getProperties().get(FADE) instanceof FadeTransition previous)
            previous.stop();
        FadeTransition fade = new FadeTransition(Duration.millis(170), content);
        content.getProperties().put(FADE, fade);
        content.setOpacity(.94);
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
        if (node.getProperties().get(FADE) instanceof FadeTransition previous)
            previous.stop();
        node.setOpacity(.86);
        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        node.getProperties().put(FADE, fade);
        fade.setToValue(1);
        fade.play();
    }
}
