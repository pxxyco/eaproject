package it.unical.ea_project_javafx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

import it.unical.ea_project_javafx.controller.activity.photo.FullScreenPhotoViewController;

public class FullscreenPhotoViewer {

    static final private String path = "/it/unical/ea_project_javafx/fxml/";

    public static void mostra(StackPane root, String url) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(FullscreenPhotoViewer.class.getResource(path + "activity/photo/full-screen-photo-view.fxml"));
            Node overlayNode = loader.load();

            FullScreenPhotoViewController controller = loader.getController();
            controller.setFoto(url);
            controller.setOnClose(() -> root.getChildren().remove(overlayNode));

            root.getChildren().add(overlayNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
