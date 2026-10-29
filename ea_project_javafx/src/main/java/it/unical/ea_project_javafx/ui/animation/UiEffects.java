package it.unical.ea_project_javafx.ui.animation;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Window;
import javafx.util.Duration;

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
            int[] counts = new int[4096];
            for (int y = 1; y < 25; y++)
                for (int x = 1; x < 33; x++) {
                    Color c = image.getPixelReader().getColor((int) (image.getWidth() * x / 33),
                            (int) (image.getHeight() * y / 25));
                    if (c.getBrightness() < .11 || c.getBrightness() > .96)
                        continue;
                    int bucket = ((int) (c.getRed() * 255) >> 4) << 8 | ((int) (c.getGreen() * 255) >> 4) << 4
                            | ((int) (c.getBlue() * 255) >> 4);
                    counts[bucket]++;
                }
            int dominant = 0;
            for (int i = 1; i < counts.length; i++)
                if (counts[i] > counts[dominant])
                    dominant = i;
            Color sample = counts[dominant] == 0 ? Color.web("#b8a48b")
                    : Color.rgb(((dominant >> 8) & 15) * 16 + 8, ((dominant >> 4) & 15) * 16 + 8,
                            (dominant & 15) * 16 + 8);
            accent = sample.interpolate(Color.WHITE, .45);
            paletteImage = image;
        }
        install(scene);
        if (scene != null) {
            Parent root = scene.getRoot();
            String base = root.getStyle().replaceAll(
                    "-(?:app-accent|color-accent|color-text-secondary|color-menu(?:-hover)?|ea-glass(?:-inset|-border)?|tone-(?:forest|mint)-[0-9]+)\\s*:[^;]*;?",
                    "").replaceAll(";\\s*;", ";").strip();
            String styled = (base.isBlank() ? "" : base + (base.endsWith(";") ? "" : ";")) + " -app-accent: "
                    + css(accent) + "; -color-accent: " + css(accent) + "; -color-menu: " + css(tintedSurface())
                    + "; -color-menu-hover: " + css(tintedSurface().interpolate(accent, .2)) + ";";
            for (String level : new String[] { "180", "550", "650", "780", "940", "980" })
                styled += " -tone-forest-" + level + ": " + rgba(tintedSurface(), Integer.parseInt(level) / 1000d)
                        + ";";
            for (String level : new String[] { "080", "120", "160", "180", "260", "320" })
                styled += " -tone-mint-" + level + ": " + rgba(accent, Integer.parseInt(level) / 1000d) + ";";
            styled += " -color-text-secondary: " + css(accent.interpolate(Color.WHITE, .65)) + ";";
            styled += " -ea-glass: " + rgba(tintedSurface(), .72) + "; -ea-glass-border: " + rgba(accent, .2) + ";";
            if (root.getStyle().equals(styled))
                return;
            root.setStyle(styled);
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
            Window.getWindows().addListener((ListChangeListener<Window>) change -> {
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
                UiEffects.class.getResource("/it/unical/ea_project_javafx/css/app.css").toExternalForm());
        watch(scene.getRoot());
        scene.rootProperty().addListener((o, old, root) -> {
            watch(root);
            useWallpaper(scene, null);
        });
    }

    public static void prepare(Parent root) {
        root.applyCss();
        root.layout();
        prepareStates(root);
    }

    private static void prepareStates(Node node) {
        if (node instanceof Region region && eligible(region))
            region.getProperties().computeIfAbsent(STATE, key -> new State(region));
        if (node instanceof Parent parent)
            List.copyOf(parent.getChildrenUnmodifiable()).forEach(UiEffects::prepareStates);
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
        if (node instanceof ScrollBar bar) {
            Runnable hideVertical = () -> {
                if (bar.getOrientation() == Orientation.VERTICAL)
                    bar.setStyle("-fx-min-width:0;-fx-pref-width:0;-fx-max-width:0;-fx-opacity:0;");
            };
            hideVertical.run();
            bar.orientationProperty().addListener((o, a, v) -> hideVertical.run());
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
        if (node instanceof javafx.scene.control.ListCell<?>) {
            for (Node parent = node; parent != null; parent = parent.getParent())
                if (parent.getStyleClass().contains("history-feed"))
                    return false;
        }
        if (node.getStyleClass().contains("history-feed") || node.getStyleClass().contains("password-reveal")
                || node.getStyleClass().contains("favorites-tabs") || node.getStyleClass().contains("back-link"))
            return false;
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

    private static Color tintedSurface() {
        return Color.web("#141414").interpolate(accent, .12);
    }

    private static String rgba(Color c, double opacity) {
        return String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,%.3f)", (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), opacity);
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
        private final ObjectProperty<Border> animatedBorder = new SimpleObjectProperty<>();
        private final ObjectProperty<Background> animatedBackground = new SimpleObjectProperty<>();
        private final ObjectProperty<Paint> animatedText = new SimpleObjectProperty<>();
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
            boolean glass = node.getStyleClass().stream()
                    .anyMatch(c -> c.endsWith("-card") || c.endsWith("-panel") || c.endsWith("-tray")
                            || c.equals("glass-card") || c.equals("context-menu") || c.equals("booking-search-box"))
                    && !node.getStyleClass().contains("popup-factory-transparent-surface");
            double opacity = node.getStyleClass().contains("app-modal-panel")
                    || node.getStyleClass().contains("context-menu") ? .94 : .68;
            boolean section = node.getStyleClass().contains("section-button")
                    || node.getStyleClass().contains("section-button-active");
            background = section
                    ? new Background(
                            new BackgroundFill(Color.rgb(255, 255, 255, .09), new CornerRadii(20), Insets.EMPTY))
                    : glass ? new Background(
                            new BackgroundFill(alpha(tintedSurface(), opacity), new CornerRadii(22), Insets.EMPTY))
                            : surface.getBackground();
            border = surface.getBorder();
            CornerRadii chosen = border != null && !border.getStrokes().isEmpty()
                    ? border.getStrokes().getFirst().getRadii()
                    : background != null && !background.getFills().isEmpty()
                            ? background.getFills().getFirst().getRadii()
                            : new CornerRadii(12);
            radii = chosen.getTopLeftHorizontalRadius() < 3 && !(node instanceof RadioButton)
                    ? new CornerRadii(12)
                    : chosen;
            nav = node.getStyleClass().contains("nav-item");
            animatedBorder.set(border);
            animatedBackground.set(background);
            if (!surface.borderProperty().isBound())
                surface.borderProperty().bind(animatedBorder);
            boolean nativePopup = false;
            for (Node ancestor = node; ancestor != null; ancestor = ancestor.getParent())
                nativePopup |= ancestor.getStyleClass().contains("combo-box-popup");
            if (!nativePopup
                    && !(node instanceof CheckBox || node instanceof RadioButton || node instanceof ToggleButton
                            || node instanceof ListView<?>)
                    && !surface.backgroundProperty().isBound())
                surface.backgroundProperty().bind(animatedBackground);
            if (nav && node instanceof Label label) {
                animatedText.set(label.getTextFill());
                label.textFillProperty().bind(animatedText);
                animatedBackground.set(Background.EMPTY);
            }
            marker = null;
            value.addListener((o, a, b) -> paint(b.doubleValue()));
            node.getStyleClass().addListener((ListChangeListener<String>) change -> transition());
            node.hoverProperty().addListener((o, a, b) -> transition());
            node.focusedProperty().addListener((o, a, b) -> {
                focused = b;
                transition();
            });
            node.getPseudoClassStates()
                    .addListener((SetChangeListener<PseudoClass>) change -> transition());
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
            return node.getPseudoClassStates().contains(PseudoClass.getPseudoClass("active"))
                    || node instanceof ToggleButton toggle && toggle.isSelected()
                    || node.getStyleClass().contains("nav-menu-active")
                    || node.getStyleClass().contains("section-button-active")
                    || node.getStyleClass().contains("tab")
                            && node.getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected"));
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
                    : focused || node.getPseudoClassStates().contains(PseudoClass.getPseudoClass("focused"))
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
                animatedBackground.set(Background.EMPTY);
                animatedText.set(Color.web("#d8d5d0").interpolate(accent, amount * .85));
            } else {
                Color neutral = alpha(accent,
                        node.getStyleClass().contains("menu-item") || node.getStyleClass().contains("discovery-link")
                                ? 0
                                : .19);
                Color color = neutral.interpolate(alpha(accent, .95), amount);
                animatedBorder
                        .set(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, radii, new BorderWidths(1))));
            }
            if (!nav && background != null && !background.getFills().isEmpty()) {
                List<BackgroundFill> fills = new ArrayList<>();
                for (BackgroundFill fill : background.getFills()) {
                    var paint = fill.getFill();
                    if (paint instanceof Color c) {
                        if (node.getStyleClass().stream().anyMatch(name -> name.endsWith("-card")
                                || name.endsWith("-panel") || name.equals("btn-create-offer")))
                            c = alpha(tintedSurface(), c.getOpacity());
                        paint = node.getStyleClass().contains("menu-item")
                                ? c.interpolate(alpha(accent, .18), amount)
                                : c.interpolate(alpha(accent, Math.max(c.getOpacity(), .08)), amount * .13);
                    }
                    fills.add(new BackgroundFill(paint, fill.getRadii().getTopLeftHorizontalRadius() < 3
                            && !(node instanceof RadioButton) ? radii : fill.getRadii(), fill.getInsets()));
                }
                animatedBackground.set(new Background(fills, background.getImages()));
            }
        }
    }
}
