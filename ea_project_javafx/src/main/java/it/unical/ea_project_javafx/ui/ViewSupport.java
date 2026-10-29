package it.unical.ea_project_javafx.ui;

import it.unical.ea_project_javafx.controller.common.NavbarController;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public final class ViewSupport {

    private ViewSupport() {
    }

    public static NavbarController loadNavbar(StackPane host) {
        return loadNavbar(host, System.err::println);
    }

    public static NavbarController loadNavbar(StackPane host, Consumer<String> onFailure) {
        if (host == null) {
            return null;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    ViewSupport.class.getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbar = loader.load();
            host.getChildren().setAll(navbar);
            return loader.getController();
        } catch (IOException error) {
            if (onFailure != null) {
                onFailure.accept("Impossibile caricare la barra di navigazione.");
            }

            return null;
        }
    }

    public static void error(Label label, String text) {
        message(label, text, "status-error");
    }

    public static void success(Label label, String text) {
        message(label, text, "status-success");
    }

    public static void show(Label label, String text) {
        message(label, text, null);
    }

    public static void clear(Label label) {
        show(label, "");
    }

    private static void message(Label label, String text, String style) {
        if (label == null) {
            return;
        }

        Runnable update = () -> {
            label.setText(text == null ? "" : text);
            if (style != null)
                label.getStyleClass().setAll("status-message", style);
            boolean visible = text != null && !text.isBlank();
            label.setVisible(visible);
            label.setManaged(visible);
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }

    }

}
