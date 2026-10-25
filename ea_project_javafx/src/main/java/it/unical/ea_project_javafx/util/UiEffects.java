package it.unical.ea_project_javafx.util;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared, finite transitions. Only interacted controls animate; text is never
 * delayed or replaced.
 */
public final class UiEffects {
    private static final Object INSTALLED = new Object(), WATCHED = new Object(), STATE = new Object();
    private static Color accent = Color.web("#d0b38b");
    private static Image paletteImage;
    private static boolean observingWindows;

    private UiEffects() {
    }

    public static Color accent() {
        return accent;
    }

    public static void useWallpaper(Scene scene, Image image) {
        if (image != null && !image.isError() && image.getPixelReader() != null && paletteImage != image) {
            double r = 0, g = 0, b = 0, weight = 0;
            for (int y = 1; y < 14; y++)
                for (int x = 1; x < 20; x++) {
                    Color c = image.getPixelReader().getColor((int) (image.getWidth() * x / 20),
                            (int) (image.getHeight() * y / 14));
                    double w = c.getSaturation() * c.getBrightness();
                    if (c.getBrightness() < .18 || c.getBrightness() > .94 || c.getSaturation() < .12)
                        continue;
                    r += c.getRed() * w;
                    g += c.getGreen() * w;
                    b += c.getBlue() * w;
                    weight += w;
                }
            if (weight > 0) {
                Color sample = Color.color(r / weight, g / weight, b / weight);
                accent = Color.hsb(sample.getHue(), Math.min(.42, Math.max(.18, sample.getSaturation())), .86);
            }
            paletteImage = image;
        }
        install(scene);
        if (scene != null) {
            Parent root = scene.getRoot();
            String base = root.getStyle().replaceAll("-app-accent\\s*:[^;]*;?", "").replaceAll(";\\s*;", ";").strip();
            root.setStyle((base.isBlank() ? "" : base + (base.endsWith(";") ? "" : ";")) + " -app-accent: "
                    + css(accent) + ";");
            Platform.runLater(() -> {
                root.applyCss();
                refresh(root);
            });
        }
    }

    public static void install(Scene scene) {
        if (scene == null || scene.getProperties().putIfAbsent(INSTALLED, Boolean.TRUE) != null)
            return;
        if (!observingWindows) {
            observingWindows = true;
            javafx.stage.Window.getWindows().addListener((ListChangeListener<javafx.stage.Window>) change -> {
                while (change.next())
                    if (change.wasAdded())
                        for (var window : change.getAddedSubList())
                            Platform.runLater(() -> {
                                if (window.isShowing() && window.getScene() != null)
                                    useWallpaper(window.getScene(), null);
                            });
            });
        }
        scene.getStylesheets().add(
                UiEffects.class.getResource("/it/unical/ea_project_javafx/css/common/motion.css").toExternalForm());
        watch(scene.getRoot());
    }

    private static void watch(Node node) {
        if (node.getProperties().putIfAbsent(WATCHED, Boolean.TRUE) != null)
            return;
        if (node instanceof Region region && eligible(region)) {
            Platform.runLater(() -> {
                if (region.getScene() == null)
                    return;
                region.applyCss();
                region.getProperties().computeIfAbsent(STATE, key -> new State(region));
            });
        }
        if (node instanceof ScrollPane scroll && !"homeScroll".equals(scroll.getId())
                && scroll.getStyleClass().contains("main-scroll"))
            PageViewportFade.install(scroll);
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(UiEffects::watch);
            parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) change -> {
                while (change.next())
                    if (change.wasAdded())
                        change.getAddedSubList().forEach(UiEffects::watch);
            });
        }
    }

    private static void refresh(Node node) {
        if (node.getProperties().get(STATE) instanceof State state)
            state.paint(state.value.get());
        if (node instanceof Parent parent)
            parent.getChildrenUnmodifiable().forEach(UiEffects::refresh);
    }

    private static boolean eligible(Region node) {
        // Skin internals are styled by their owning control rather than animated
        // individually.
        if (node instanceof TextInputControl && node.getStyleClass().contains("text-field")
                && (node.getParent() instanceof Spinner<?> || node.getParent() instanceof ComboBoxBase<?>))
            return false;
        return node instanceof ButtonBase || node instanceof TextInputControl || node instanceof ComboBoxBase<?>
                || node instanceof Spinner<?> || node instanceof Slider || node instanceof TableView<?>
                || node instanceof ListView<?> || node instanceof TabPane
                || node.getStyleClass().stream().anyMatch(c -> c.equals("nav-item")
                        || c.equals("notification-bell-container")
                        || c.equals("experience-card") || c.equals("itinerary-panel") || c.equals("account-card")
                        || c.startsWith("btn-") || c.equals("load-more-label") || c.equals("glass-card")
                        || c.equals("notification-card") || c.equals("home-guide")
                        || c.equals("profile-card") || c.equals("account-panel") || c.equals("dashboard-space-panel")
                        || c.equals("chat-panel") || c.equals("app-modal-panel") || c.equals("tab")
                        || c.equals("menu-item")
                        || c.endsWith("-card") || c.endsWith("-panel") || c.endsWith("-tray") || c.endsWith("-toast"));
    }

    private static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private static String css(Color c) {
        return String.format("#%02x%02x%02x", (int) (c.getRed() * 255), (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private static final class State {
        private final Region node, surface;
        private final Background background;
        private final Border border;
        private final CornerRadii radii;
        private final DoubleProperty value = new SimpleDoubleProperty();
        private final Timeline animation = new Timeline();
        private final javafx.beans.property.ObjectProperty<Border> animatedBorder = new javafx.beans.property.SimpleObjectProperty<>();
        private final javafx.beans.property.ObjectProperty<Background> animatedBackground = new javafx.beans.property.SimpleObjectProperty<>();
        private final javafx.beans.property.ObjectProperty<javafx.scene.paint.Paint> animatedText = new javafx.beans.property.SimpleObjectProperty<>();
        private final Region marker;
        private final boolean nav;
        private boolean focused;
        private Node typingGlyph;

        State(Region node) {
            this.node = node;
            Node inner = node instanceof CheckBox ? node.lookup(".box")
                    : node instanceof RadioButton ? node.lookup(".radio")
                            : node instanceof Slider ? node.lookup(".thumb") : null;
            surface = node.getStyleClass().stream().anyMatch(c -> c.startsWith("transparent-"))
                    && node.getParent() instanceof Region parent && parent.getStyleClass().contains("search-field")
                            ? parent
                            : inner instanceof Region region ? region : node;
            background = surface.getBackground();
            border = surface.getBorder();
            radii = border != null && !border.getStrokes().isEmpty() ? border.getStrokes().getFirst().getRadii()
                    : background != null && !background.getFills().isEmpty()
                            ? background.getFills().getFirst().getRadii()
                            : new CornerRadii(10);
            nav = node.getStyleClass().contains("nav-item");
            animatedBorder.set(border);
            animatedBackground.set(background);
            if (!surface.borderProperty().isBound())
                surface.borderProperty().bind(animatedBorder);
            // Native selected fills and check/radio marks must retain their contrast.
            if (!(node instanceof CheckBox || node instanceof RadioButton || node instanceof ToggleButton)
                    && !surface.backgroundProperty().isBound())
                surface.backgroundProperty().bind(animatedBackground);
            if (nav && node instanceof Label label) {
                animatedText.set(label.getTextFill());
                label.textFillProperty().bind(animatedText);
                marker = new Region();
                marker.setMinSize(18, 2);
                marker.setPrefSize(18, 2);
                marker.setMaxSize(18, 2);
                marker.setMouseTransparent(true);
                label.setGraphic(marker);
                label.setContentDisplay(ContentDisplay.BOTTOM);
                label.setGraphicTextGap(5);
            } else
                marker = null;
            value.addListener((o, a, b) -> paint(b.doubleValue()));
            node.hoverProperty().addListener((o, a, b) -> transition());
            node.focusedProperty().addListener((o, a, b) -> {
                focused = b;
                transition();
            });
            node.getPseudoClassStates()
                    .addListener((javafx.collections.SetChangeListener<javafx.css.PseudoClass>) change -> transition());
            if (node instanceof ButtonBase button)
                button.armedProperty().addListener((o, a, b) -> transition());
            if (node instanceof TextInputControl input)
                input.textProperty().addListener((o, a, b) -> {
                    if (input.isFocused()) {
                        animation.stop();
                        Node glyph = input.lookup(".text");
                        typingGlyph = glyph;
                        if (glyph != null && !glyph.opacityProperty().isBound())
                            glyph.setOpacity(.84);
                        var frames = new ArrayList<KeyFrame>();
                        frames.add(new KeyFrame(Duration.millis(65), new KeyValue(value, 1)));
                        if (glyph != null && !glyph.opacityProperty().isBound())
                            frames.add(new KeyFrame(Duration.millis(170),
                                    new KeyValue(glyph.opacityProperty(), 1, Interpolator.EASE_OUT)));
                        frames.add(new KeyFrame(Duration.millis(330), new KeyValue(value, .72, Interpolator.EASE_OUT)));
                        animation.getKeyFrames().setAll(frames);
                        animation.playFromStart();
                    }
                });
            node.sceneProperty().addListener((o, a, b) -> {
                if (a != b)
                    animation.stop();
            });
            focused = node.isFocused();
            paint(0);
            transition();
        }

        private boolean selected() {
            return node.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("active"))
                    || node instanceof ToggleButton toggle && toggle.isSelected()
                    || node.getStyleClass().contains("nav-menu-active")
                    || node.getStyleClass().contains("tab")
                            && node.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("selected"));
        }

        private void transition() {
            if (typingGlyph != null && !typingGlyph.opacityProperty().isBound())
                typingGlyph.setOpacity(1);
            if (node.isDisabled()) {
                animation.stop();
                paint(0);
                return;
            }
            double target = selected() ? .82
                    : focused || node.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("focused"))
                            ? .72
                            : node.isHover() ? .42 : 0;
            if (node instanceof ButtonBase button && button.isArmed())
                target = .95;
            animation.stop();
            if (Math.abs(value.get() - target) < .00001) {
                paint(target);
                return;
            }
            animation.getKeyFrames()
                    .setAll(new KeyFrame(Duration.millis(240), new KeyValue(value, target, Interpolator.EASE_BOTH)));
            animation.playFromStart();
        }

        private void paint(double amount) {
            if (nav) {
                animatedBorder.set(Border.EMPTY);
                marker.setBackground(
                        new Background(new BackgroundFill(accent, new CornerRadii(2), javafx.geometry.Insets.EMPTY)));
                marker.setOpacity(selected() ? Math.max(.3, amount) : amount * .45);
                animatedText.set(Color.web("#d8d5d0").interpolate(accent, amount * .85));
            } else {
                Color neutral = alpha(accent, node.getStyleClass().contains("menu-item") ? 0 : .19);
                Color color = neutral.interpolate(alpha(accent, .95), amount);
                animatedBorder
                        .set(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, radii, new BorderWidths(1))));
            }
            if (background != null && !background.getFills().isEmpty()) {
                List<BackgroundFill> fills = new ArrayList<>();
                for (BackgroundFill fill : background.getFills()) {
                    var paint = fill.getFill();
                    if (paint instanceof Color c)
                        paint = node.getStyleClass().contains("menu-item")
                                ? c.interpolate(alpha(accent, .18), amount)
                                : c.interpolate(alpha(accent, Math.max(c.getOpacity(), .08)), amount * .13);
                    fills.add(new BackgroundFill(paint, fill.getRadii(), fill.getInsets()));
                }
                animatedBackground.set(new Background(fills, background.getImages()));
            }
        }
    }
}
