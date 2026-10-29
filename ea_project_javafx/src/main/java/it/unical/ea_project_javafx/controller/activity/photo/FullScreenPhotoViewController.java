package it.unical.ea_project_javafx.controller.activity.photo;

import javafx.fxml.FXML;
import it.unical.ea_project_javafx.util.ImageUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class FullScreenPhotoViewController {

    @FXML
    private ImageView imageView;

    private Runnable onClose;

    public void setFoto(String url) {
        imageView.setImage(new Image(ImageUtils.buildFullUrl(url), 2400, 1600, true, true, true));
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
