package it.unical.ea_project_javafx.controller.activity.photo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class FullScreenPhotoViewController {

    @FXML
    private Button closeButton;

    @FXML
    private ImageView imageView;

    private Runnable onClose;

    public void setFoto(String url) {
        imageView.setImage(new Image(url));
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void handleClose() {

        if (onClose != null) {
            onClose.run();
        }

    }

    @FXML
    public void handleClickOverlay(MouseEvent mouseEvent) {

        if (onClose != null) {
            onClose.run();
        }

    }

}
