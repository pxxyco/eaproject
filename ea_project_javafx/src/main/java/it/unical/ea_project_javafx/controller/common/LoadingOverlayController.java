package it.unical.ea_project_javafx.controller.common;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import it.unical.ea_project_javafx.ui.loading.LoadingRing;

public class LoadingOverlayController {
    @FXML
    private StackPane loadingRoot;
    @FXML
    private LoadingRing ring;
    @FXML
    private Label label, errorIcon;
    private Parent observedParent;
    private final Map<Node, Effect> effects = new LinkedHashMap<>();
    private final ChangeListener<Boolean> visibility = (o, a, b) -> update();

    @FXML
    private void initialize() {
        loadingRoot.parentProperty().addListener((o, old, parent) -> {
            if (observedParent != null)
                observedParent.visibleProperty().removeListener(visibility);
            observedParent = parent;
            if (parent != null)
                parent.visibleProperty().addListener(visibility);
            update();
        });
        loadingRoot.sceneProperty().addListener((o, a, b) -> update());
        loadingRoot.visibleProperty().addListener(visibility);
    }

    private void update() {
        boolean active = loadingRoot.getScene() != null;
        for (Node node = loadingRoot; node != null; node = node.getParent())
            active &= node.isVisible();
        if (active) {
            Node branch = loadingRoot;
            while (branch.getParent() != null) {
                Parent parent = branch.getParent();
                for (Node sibling : parent.getChildrenUnmodifiable()) {
                    if (sibling != branch && sibling.isVisible() && !effects.containsKey(sibling)) {
                        effects.put(sibling, sibling.getEffect());
                        sibling.setEffect(new GaussianBlur(14));
                    }
                }
                branch = parent;
            }
        } else {
            effects.forEach(Node::setEffect);
            effects.clear();
        }
    }

    public void setMessage(String message) {
        ring.setVisible(true);
        ring.setManaged(true);
        errorIcon.setVisible(false);
        errorIcon.setManaged(false);
        label.setText(message);
        update();
    }

    public void setError(String message) {
        ring.setVisible(false);
        ring.setManaged(false);
        errorIcon.setVisible(true);
        errorIcon.setManaged(true);
        label.setText(message);
        update();
    }
}
