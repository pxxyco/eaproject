package it.unical.ea_project_javafx.util;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Fabbrica unica dei popup in-app. Centralizza overlay, blur, animazioni,
 * pulsante di chiusura, dimensioni e stile del contenitore.
 * Tutte le operazioni devono essere invocate dal JavaFX Application Thread.
 */
public final class PopupFactory {
    private static final String COMMON_CSS = "/it/unical/ea_project_javafx/css/common/style.css";
    private static final Map<StackPane, Handle> ACTIVE_POPUPS = new WeakHashMap<>();

    private PopupFactory() { }

    public enum Size {
        COMPACT(540, 500),
        MEDIUM(760, 660),
        LARGE(1000, 790),
        EXTRA_LARGE(1180, 860);

        private final double maxWidth;
        private final double maxHeight;

        Size(double maxWidth, double maxHeight) {
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
        }
    }

    public static Handle show(StackPane sceneRoot, Parent content) {
        return show(sceneRoot, content, Size.MEDIUM);
    }

    public static Handle show(StackPane sceneRoot, Parent content, Size size) {
        requireFxThread();
        if (sceneRoot == null || content == null) {
            throw new IllegalArgumentException("La radice della scena e il contenuto del popup sono obbligatori.");
        }
        Size resolvedSize = size == null ? Size.MEDIUM : size;
        // Un bottone o una schermata annidata può passare un contenitore interno.
        // L'overlay deve invece appartenere sempre alla radice reale della Scene:
        // in questo modo blur e backdrop coprono anche navbar e sfondo.
        sceneRoot = fullSceneRoot(sceneRoot);

        Handle previous = ACTIVE_POPUPS.get(sceneRoot);
        if (previous != null) previous.closeImmediately();

        ensureStylesheet(sceneRoot.getScene());
        ensurePopupStylesheet(content);
        if (!content.getStyleClass().contains("app-popup-root")) content.getStyleClass().add("app-popup-root");

        Map<Node, Effect> previousEffects = new LinkedHashMap<>();
        for (Node node : new ArrayList<>(sceneRoot.getChildren())) {
            previousEffects.put(node, node.getEffect());
            node.setEffect(new GaussianBlur(6.0));
        }

        double viewportWidth = sceneRoot.getScene() == null ? sceneRoot.getWidth() : sceneRoot.getScene().getWidth();
        double viewportHeight = sceneRoot.getScene() == null ? sceneRoot.getHeight() : sceneRoot.getScene().getHeight();
        double maxWidth = resolvedSize.maxWidth;
        double maxHeight = resolvedSize.maxHeight;
        // Mantiene margine visibile anche su finestre ridimensionate; non impone una
        // larghezza minima superiore allo spazio reale disponibile.
        if (viewportWidth > 0) maxWidth = Math.min(maxWidth, Math.max(200, viewportWidth - 40));
        if (viewportHeight > 0) maxHeight = Math.min(maxHeight, Math.max(180, viewportHeight - 40));
        applyMaxSize(content, maxWidth, maxHeight);
        if (!content.getStyleClass().contains("app-modal-panel")
                && !content.getStyleClass().contains("popup-factory-transparent-surface")) {
            content.getStyleClass().add("app-modal-panel");
        }

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("app-modal-overlay");
        overlay.setPickOnBounds(true);
        overlay.setMinSize(0, 0);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlay.setOpacity(0.0);
        Region backdrop = new Region();
        backdrop.getStyleClass().add("app-modal-backdrop");
        backdrop.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        backdrop.setPickOnBounds(true);

        overlay.getChildren().addAll(backdrop, content);
        StackPane.setAlignment(content, Pos.CENTER);
        StackPane.setMargin(content, new Insets(18));
        sceneRoot.getChildren().add(overlay);

        Handle handle = new Handle(sceneRoot, overlay, previousEffects);
        ACTIVE_POPUPS.put(sceneRoot, handle);
        if (!hasCloseControl(content)) addStandardCloseButton(content, handle);
        overlay.setFocusTraversable(true);
        overlay.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) handle.close();
        });
        overlay.requestFocus();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), overlay);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        return handle;
    }

    /** Risolve una radice StackPane che copre l'intera scena, anche se la vista ne usa un altro tipo. */
    public static StackPane resolveRoot(Node anchor) {
        if (anchor == null || anchor.getScene() == null) return null;
        return fullSceneRoot(anchor.getScene());
    }

    private static StackPane fullSceneRoot(StackPane requestedRoot) {
        if (requestedRoot == null) return null;
        Scene scene = requestedRoot.getScene();
        return scene == null ? requestedRoot : fullSceneRoot(scene);
    }

    private static StackPane fullSceneRoot(Scene scene) {
        if (scene.getRoot() instanceof StackPane stackPane) return stackPane;
        Parent oldRoot = scene.getRoot();
        StackPane wrapper = new StackPane(oldRoot);
        wrapper.getStyleClass().add("app-popup-scene-wrapper");
        wrapper.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scene.setRoot(wrapper);
        ensureStylesheet(scene);
        return wrapper;
    }

    public static Handle showMessage(StackPane root, String title, String message, Size size) {
        VBox panel = new VBox(14);
        Label heading = new Label(title == null || title.isBlank() ? "Informazione" : title);
        heading.getStyleClass().add("app-modal-title");
        Label body = new Label(message == null ? "" : message);
        body.getStyleClass().add("app-modal-body");
        body.setWrapText(true);
        Button close = new Button("Ho capito");
        close.getStyleClass().add("btn-primary");
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(heading, body, actions);
        Handle handle = show(root, panel, size == null ? Size.COMPACT : size);
        close.setOnAction(event -> handle.close());
        return handle;
    }

    public static Handle showConfirmation(StackPane root, String title, String message,
                                          Size size, Runnable onConfirm) {
        VBox panel = new VBox(16);
        Label heading = new Label(title == null || title.isBlank() ? "Conferma operazione" : title);
        heading.getStyleClass().add("app-modal-title");
        Label body = new Label(message == null ? "" : message);
        body.getStyleClass().add("app-modal-body");
        body.setWrapText(true);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Annulla");
        cancel.getStyleClass().add("btn-secondary");
        Button confirm = new Button("Conferma");
        confirm.getStyleClass().add("btn-primary");
        actions.getChildren().addAll(cancel, confirm);
        panel.getChildren().addAll(heading, body, actions);
        Handle handle = show(root, panel, size == null ? Size.COMPACT : size);
        cancel.setOnAction(event -> handle.close());
        confirm.setOnAction(event -> {
            handle.close();
            if (onConfirm != null) onConfirm.run();
        });
        return handle;
    }

    private static void addStandardCloseButton(Parent content, Handle handle) {
        Button close = new Button("×");
        close.getStyleClass().add("app-modal-close");
        close.setAccessibleText("Chiudi popup");
        close.setOnAction(event -> handle.close());
        HBox toolbar = new HBox(close);
        toolbar.getStyleClass().add("app-popup-toolbar");
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        if (content instanceof VBox box) {
            box.getChildren().add(0, toolbar);
            return;
        }

        // Per i pannelli non VBox, la barra di chiusura deve stare dentro la stessa
        // superficie glass; altrimenti la X resta fuori dal pannello e sembra un popup diverso.
        if (content.getParent() instanceof StackPane parent) {
            int index = parent.getChildren().indexOf(content);
            if (index >= 0) {
                boolean hadModalPanel = content.getStyleClass().remove("app-modal-panel");
                double maxWidth = content instanceof Region region ? region.getMaxWidth() : Double.MAX_VALUE;
                double maxHeight = content instanceof Region region ? region.getMaxHeight() : Double.MAX_VALUE;
                VBox wrapper = new VBox(8, toolbar, content);
                wrapper.getStyleClass().add("app-popup-content-wrapper");
                if (hadModalPanel) wrapper.getStyleClass().add("app-modal-panel");
                if (content.getStyleClass().contains("popup-factory-transparent-surface")) {
                    wrapper.getStyleClass().add("popup-factory-transparent-surface");
                }
                if (maxWidth > 0) wrapper.setMaxWidth(maxWidth);
                if (maxHeight > 0) wrapper.setMaxHeight(maxHeight);
                ensurePopupStylesheet(wrapper);
                parent.getChildren().set(index, wrapper);
                StackPane.setAlignment(wrapper, Pos.CENTER);
                StackPane.setMargin(wrapper, new Insets(18));
            }
        }
    }

    private static boolean hasCloseControl(Node node) {
        if (node.getStyleClass().contains("app-modal-close")
                || node.getStyleClass().contains("btn-icon-close")
                || node.getStyleClass().contains("popup-close-button")
                || node.getStyleClass().contains("sharing-popup-close")) return true;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (hasCloseControl(child)) return true;
            }
        }
        return false;
    }

    private static void applyMaxSize(Parent content, double maxWidth, double maxHeight) {
        if (content instanceof Region region) {
            double currentWidth = region.getMaxWidth();
            double currentHeight = region.getMaxHeight();
            region.setMaxWidth(currentWidth <= 0 || currentWidth == Double.MAX_VALUE
                    ? maxWidth : Math.min(currentWidth, maxWidth));
            region.setMaxHeight(currentHeight <= 0 || currentHeight == Double.MAX_VALUE
                    ? maxHeight : Math.min(currentHeight, maxHeight));
        }
    }

    private static void ensureStylesheet(Scene scene) {
        if (scene == null) return;
        var resource = PopupFactory.class.getResource(COMMON_CSS);
        if (resource == null) return;
        String css = resource.toExternalForm();
        if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
    }

    /** Applica la stylesheet comune direttamente al popup, dopo i fogli specifici del FXML. */
    private static void ensurePopupStylesheet(Parent content) {
        var resource = PopupFactory.class.getResource(COMMON_CSS);
        if (resource == null) return;
        String css = resource.toExternalForm();
        content.getStylesheets().remove(css);
        content.getStylesheets().add(css);
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("PopupFactory deve essere utilizzata dal JavaFX Application Thread.");
        }
    }

    public static final class Handle {
        private final StackPane root;
        private final StackPane overlay;
        private final Map<Node, Effect> previousEffects;
        private FadeTransition closeTransition;
        private boolean closed;

        private Handle(StackPane root, StackPane overlay, Map<Node, Effect> previousEffects) {
            this.root = root;
            this.overlay = overlay;
            this.previousEffects = previousEffects;
        }

        public void close() {
            requireFxThread();
            if (closed || closeTransition != null) return;
            closeTransition = new FadeTransition(Duration.millis(150), overlay);
            closeTransition.setFromValue(overlay.getOpacity());
            closeTransition.setToValue(0.0);
            closeTransition.setOnFinished(event -> finishClose());
            closeTransition.play();
        }

        private void closeImmediately() {
            if (closeTransition != null) closeTransition.stop();
            finishClose();
        }

        private void finishClose() {
            if (closed) return;
            closed = true;
            root.getChildren().remove(overlay);
            previousEffects.forEach((node, effect) -> {
                if (root.getChildren().contains(node)) node.setEffect(effect);
            });
            // Stacca i contenuti dall'overlay rimosso: i popup FXML riutilizzati
            // (es. gestione condivisioni) possono essere aperti una seconda volta.
            overlay.getChildren().clear();
            if (ACTIVE_POPUPS.get(root) == this) ACTIVE_POPUPS.remove(root);
        }

        public StackPane getOverlay() { return overlay; }
        public boolean isClosed() { return closed; }
    }
}
