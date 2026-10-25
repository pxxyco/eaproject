package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.util.UiEffects;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.ArrayList;

/**
 * One set of controls, pixel-aligned in either location, with a continuous
 * palette transition.
 */
final class StickySearch implements AutoCloseable {
    private final ScrollPane scroll;
    private final VBox dock;
    private final Region navbar;
    private final SearchBarController search;
    private Node viewport, originalClip;
    private final Rectangle fade = new Rectangle();
    private final DoubleProperty pinAmount = new SimpleDoubleProperty();
    private final Timeline transition = new Timeline();
    private Background normalBackground;
    private Border normalBorder;
    private boolean pinned, moving, scheduled, closed;
    private double maskWidth = -1, maskHeight = -1, maskCutoff = -1;

    StickySearch(ScrollPane scroll, VBox dock, SearchBarController search, Region navbar) {
        this.scroll = scroll;
        this.dock = dock;
        this.search = search;
        this.navbar = navbar;
        dock.setVisible(false);
        dock.setManaged(false);
        dock.setPadding(Insets.EMPTY);
        scroll.vvalueProperty().addListener((o, a, b) -> update());
        scroll.viewportBoundsProperty().addListener((o, a, b) -> schedule());
        scroll.getContent().layoutBoundsProperty().addListener((o, a, b) -> schedule());
        scroll.sceneProperty().addListener((o, a, b) -> {
            if (b != null)
                schedule();
        });
        navbar.heightProperty().addListener((o, a, b) -> schedule());
        pinAmount.addListener((o, a, b) -> mask());
        schedule();
    }

    private void schedule() {
        if (closed || scheduled)
            return;
        scheduled = true;
        Platform.runLater(() -> {
            scheduled = false;
            update();
        });
    }

    private void update() {
        if (closed || moving || scroll.getScene() == null)
            return;
        if (viewport == null) {
            viewport = scroll.lookup(".viewport");
            if (viewport == null)
                return;
            originalClip = viewport.getClip();
        }
        var content = scroll.getContent();
        double slotTop = content.sceneToLocal(search.slot().localToScene(search.slot().getLayoutBounds())).getMinY();
        double range = Math.max(0, content.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight());
        double offset = scroll.getVmax() <= scroll.getVmin() ? 0
                : (scroll.getVvalue() - scroll.getVmin()) / (scroll.getVmax() - scroll.getVmin()) * range;
        boolean shouldPin = slotTop < offset + navbar.getHeight();
        if (shouldPin != pinned)
            relocate(shouldPin);
        if (pinned)
            positionDock();
        mask();
    }

    private void positionDock() {
        // Use the slot's actual screen X and width, not the center of a pane containing
        // a scrollbar.
        var bounds = dock.getParent().sceneToLocal(search.slot().localToScene(search.slot().getLayoutBounds()));
        double height = search.controls().prefHeight(bounds.getWidth());
        dock.resizeRelocate(bounds.getMinX(), navbar.getHeight() + 8, bounds.getWidth(), height);
        dock.layout();
    }

    private void mask() {
        if (viewport == null)
            return;
        double width = scroll.getViewportBounds().getWidth(), height = scroll.getViewportBounds().getHeight();
        double cutoff = navbar.getHeight() - 14 + pinAmount.get() * (search.controls().getHeight() + 24);
        if (width == maskWidth && height == maskHeight && cutoff == maskCutoff)
            return;
        maskWidth = width;
        maskHeight = height;
        maskCutoff = cutoff;
        fade.setWidth(width);
        fade.setHeight(height);
        fade.setFill(new LinearGradient(0, 0, 0, cutoff + 30, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(Math.max(0, cutoff) / (cutoff + 30), Color.TRANSPARENT),
                new Stop(1, Color.WHITE)));
        viewport.setClip(fade);
    }

    private void relocate(boolean pin) {
        moving = true;
        Node controls = search.controls();
        Node focused = scroll.getScene().getFocusOwner();
        boolean restoreFocus = focused != null && inside(focused, search.controls());
        double oldY = controls.localToScene(controls.getLayoutBounds()).getMinY();
        if (normalBackground == null) {
            search.controls().applyCss();
            normalBackground = search.controls().getBackground();
            normalBorder = search.controls().getBorder();
            search.controls().backgroundProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(
                    () -> blendedBackground(pinAmount.get()), pinAmount));
            search.controls().borderProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(
                    () -> blendedBorder(pinAmount.get()), pinAmount));
        }
        transition.stop();
        search.relocated();
        if (pin) {
            double height = search.controls().getHeight();
            search.slot().setMinHeight(height);
            search.slot().setPrefHeight(height);
            search.slot().getChildren().remove(controls);
            dock.getChildren().setAll(controls);
            dock.setVisible(true);
        } else {
            dock.getChildren().clear();
            dock.setVisible(false);
            search.slot().getChildren().setAll(controls);
            search.slot().setMinHeight(Region.USE_COMPUTED_SIZE);
            search.slot().setPrefHeight(Region.USE_COMPUTED_SIZE);
            search.slot().layout();
        }
        pinned = pin;
        if (pin)
            positionDock();
        double newY = controls.localToScene(controls.getLayoutBounds()).getMinY();
        controls.setTranslateY(Math.max(-10, Math.min(10, oldY - newY)));
        controls.setOpacity(.90);
        transition.getKeyFrames().setAll(new KeyFrame(Duration.millis(300),
                new KeyValue(pinAmount, pin ? 1 : 0, Interpolator.EASE_BOTH),
                new KeyValue(controls.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(controls.opacityProperty(), 1, Interpolator.EASE_OUT)));
        transition.playFromStart();
        moving = false;
        if (restoreFocus)
            focused.requestFocus();
    }

    private Background blendedBackground(double amount) {
        var fills = new ArrayList<BackgroundFill>();
        for (BackgroundFill fill : normalBackground.getFills()) {
            Paint paint = fill.getFill();
            if (paint instanceof Color color)
                paint = color.interpolate(Color.web("#242722"), amount);
            fills.add(new BackgroundFill(paint, fill.getRadii(), fill.getInsets()));
        }
        return new Background(fills, normalBackground.getImages());
    }

    private Border blendedBorder(double amount) {
        if (normalBorder != null && !normalBorder.getStrokes().isEmpty()) {
            var base = normalBorder.getStrokes().getFirst();
            Color color = base.getTopStroke() instanceof Color c ? c : Color.TRANSPARENT;
            return new Border(new BorderStroke(color.interpolate(UiEffects.accent(), amount * .65),
                    BorderStrokeStyle.SOLID, base.getRadii(), base.getWidths()));
        }
        return normalBorder;
    }

    private static boolean inside(Node node, Parent parent) {
        for (; node != null; node = node.getParent())
            if (node == parent)
                return true;
        return false;
    }

    @Override
    public void close() {
        transition.stop();
        closed = true;
        search.controls().backgroundProperty().unbind();
        search.controls().borderProperty().unbind();
        if (viewport != null)
            viewport.setClip(originalClip);
    }
}
