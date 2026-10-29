package it.unical.ea_project_javafx.ui.loading;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.value.ChangeListener;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.ColorConverter;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;

public final class LoadingRing extends Region {
    private static final Color DEFAULT_COLOR = Color.rgb(245, 245, 244);
    private static final CssMetaData<LoadingRing, Color> PROGRESS_COLOR = new CssMetaData<>(
            "-fx-progress-color", ColorConverter.getInstance(), DEFAULT_COLOR) {
        @Override
        public boolean isSettable(LoadingRing ring) {
            return !ring.progressColor.isBound();
        }

        @Override
        public StyleableProperty<Color> getStyleableProperty(LoadingRing ring) {
            return ring.progressColor;
        }
    };
    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA;
    static {
        var properties = new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
        properties.add(PROGRESS_COLOR);
        CSS_META_DATA = List.copyOf(properties);
    }

    private final StyleableObjectProperty<Color> progressColor = new StyleableObjectProperty<>(DEFAULT_COLOR) {
        @Override
        protected void invalidated() {
            renderedSize = -1;
            requestLayout();
        }

        @Override
        public Object getBean() {
            return LoadingRing.this;
        }

        @Override
        public String getName() {
            return "progressColor";
        }

        @Override
        public CssMetaData<LoadingRing, Color> getCssMetaData() {
            return PROGRESS_COLOR;
        }
    };

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }

    private final Canvas canvas = new Canvas();
    private final RotateTransition rotation = new RotateTransition(Duration.millis(1150), canvas);
    private Scene observedScene;
    private Window observedWindow;
    private double renderedSize = -1;
    private final ChangeListener<Window> windowChange = (o, a, b) -> watchWindow(b);
    private final List<Node> watched = new ArrayList<>();
    private final ChangeListener<Boolean> visibility = (o, a, b) -> update();

    public LoadingRing() {
        setMinSize(20, 20);
        setPrefSize(48, 48);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMouseTransparent(true);
        getStyleClass().add("loading-ring");
        getChildren().add(canvas);
        rotation.setByAngle(360);
        rotation.setInterpolator(Interpolator.LINEAR);
        rotation.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((o, a, b) -> {
            if (observedScene != null)
                observedScene.windowProperty().removeListener(windowChange);
            observedScene = b;
            if (b != null)
                b.windowProperty().addListener(windowChange);
            watchWindow(b == null ? null : b.getWindow());
            watchAncestors();
        });
        parentProperty().addListener((o, a, b) -> watchAncestors());
    }

    private void watchAncestors() {
        watched.forEach(n -> n.visibleProperty().removeListener(visibility));
        watched.clear();
        for (Node n = this; n != null; n = n.getParent()) {
            watched.add(n);
            n.visibleProperty().addListener(visibility);
        }
        update();
    }

    private void watchWindow(Window window) {
        if (observedWindow != null)
            observedWindow.showingProperty().removeListener(visibility);
        observedWindow = window;
        if (window != null)
            window.showingProperty().addListener(visibility);
        update();
    }

    private void update() {
        boolean active = observedWindow != null && observedWindow.isShowing() && getScene() != null
                && watched.stream().allMatch(Node::isVisible);
        if (active)
            rotation.play();
        else
            rotation.pause();
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        canvas.setWidth(size);
        canvas.setHeight(size);
        canvas.relocate((getWidth() - size) / 2, (getHeight() - size) / 2);
        if (size <= 0 || Math.abs(size - renderedSize) < .01)
            return;
        renderedSize = size;
        int resolution = Math.max(1, (int) Math.ceil(size * 2));
        WritableImage image = new WritableImage(resolution, resolution);
        double radius = .415, halfStroke = .0425, end = Math.toRadians(390);
        double headX = radius * Math.cos(end), headY = -radius * Math.sin(end);
        Color tint = progressColor.get() == null ? DEFAULT_COLOR : progressColor.get();
        for (int y = 0; y < resolution; y++) {
            for (int x = 0; x < resolution; x++) {
                double dx = (x + .5) / resolution - .5, dy = (y + .5) / resolution - .5;
                double angle = (Math.toDegrees(Math.atan2(-dy, dx)) - 90 + 360) % 360;
                double coverage = Math.max(0, Math.min(1,
                        .5 + (halfStroke - Math.abs(Math.hypot(dx, dy) - radius)) * resolution));
                double alpha = angle <= 300 ? coverage * (.015 + .985 * angle / 300) : 0;
                double cap = Math.max(0, Math.min(1,
                        .5 + (halfStroke - Math.hypot(dx - headX, dy - headY)) * resolution));
                image.getPixelWriter().setColor(x, y, Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(),
                        tint.getOpacity() * Math.max(alpha, cap)));
            }
        }
        var g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, size, size);
        g.drawImage(image, 0, 0, size, size);
    }
}
