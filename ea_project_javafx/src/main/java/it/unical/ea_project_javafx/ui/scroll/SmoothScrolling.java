package it.unical.ea_project_javafx.ui.scroll;

import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public final class SmoothScrolling {
    private static final Object INSTALLED = new Object(), STATE = new Object();

    private SmoothScrolling() {
    }

    public static void install(Scene scene) {
        if (scene == null || scene.getProperties().putIfAbsent(INSTALLED, true) != null)
            return;
        scene.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isConsumed() || event.isControlDown() || event.isShiftDown() || event.isDirect()
                    || event.isInertia()
                    || Math.abs(event.getDeltaY()) < .01
                    || Math.abs(event.getDeltaY()) < 18
                            && event.getTextDeltaYUnits() != ScrollEvent.VerticalTextScrollUnits.LINES
                    || Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()))
                return;
            for (Node node = event.getTarget() instanceof Node n ? n : null; node != null; node = node.getParent()) {
                if (node instanceof TextInputControl || node instanceof TableView<?> || node instanceof ListView<?>
                        || node instanceof ComboBoxBase<?> || node instanceof ScrollBar)
                    return;
                if (node instanceof ScrollPane pane) {
                    if (range(pane) <= 0 || pane.getVmax() <= pane.getVmin())
                        continue;
                    Motion motion = (Motion) pane.getProperties().computeIfAbsent(STATE, key -> new Motion(pane));
                    motion.scroll(-event.getDeltaY() * 1.7);
                    event.consume();
                    return;
                }
            }
        });
    }

    private static double range(ScrollPane pane) {
        return pane.getContent() == null ? 0
                : Math.max(0, pane.getContent().getLayoutBounds().getHeight() - pane.getViewportBounds().getHeight());
    }

    private static final class Motion extends AnimationTimer {
        private final ScrollPane pane;
        private Node content;
        private double position, target, velocity, baseY;
        private long previous;
        private boolean running, applying;

        Motion(ScrollPane pane) {
            this.pane = pane;
            pane.vvalueProperty().addListener((o, a, b) -> {
                if (!applying && running)
                    finish();
            });
            pane.sceneProperty().addListener((o, a, b) -> {
                if (a != b)
                    finish();
            });
            pane.contentProperty().addListener((o, a, b) -> finish());
            pane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> finish());
        }

        void scroll(double pixels) {
            double max = range(pane);
            if (!running) {
                content = pane.getContent();
                baseY = content.getTranslateY();
                position = (pane.getVvalue() - pane.getVmin()) / (pane.getVmax() - pane.getVmin()) * max;
                target = position;
                velocity = 0;
                previous = 0;
                running = true;
                start();
            }
            double next = target + pixels;
            target = Math.max(0, Math.min(max, next));
            velocity += pixels * 2.3;
            if (next < 0 || next > max)
                velocity = Math.max(-220, Math.min(220, velocity));
        }

        @Override
        public void handle(long now) {
            if (pane.getScene() == null || pane.getScene().getWindow() == null
                    || !pane.getScene().getWindow().isShowing()) {
                finish();
                return;
            }
            if (previous == 0) {
                previous = now;
                return;
            }
            double dt = Math.min(.032, (now - previous) / 1_000_000_000.0);
            previous = now;
            double max = range(pane);
            target = Math.max(0, Math.min(max, target));
            velocity += ((target - position) * 120 - velocity * 16) * dt;
            position += velocity * dt;
            double clamped = Math.max(0, Math.min(max, position));
            applying = true;
            pane.setVvalue(
                    max == 0 ? pane.getVmin() : pane.getVmin() + clamped / max * (pane.getVmax() - pane.getVmin()));
            applying = false;
            double excess = position - clamped;
            content.setTranslateY(baseY - Math.copySign(Math.min(22, Math.abs(excess) * .5), excess));
            if (Math.abs(position - target) < .35 && Math.abs(velocity) < 3) {
                applying = true;
                pane.setVvalue(
                        max == 0 ? pane.getVmin() : pane.getVmin() + target / max * (pane.getVmax() - pane.getVmin()));
                applying = false;
                finish();
            }
        }

        private void finish() {
            stop();
            running = false;
            velocity = 0;
            previous = 0;
            if (content != null)
                content.setTranslateY(baseY);
        }
    }
}
