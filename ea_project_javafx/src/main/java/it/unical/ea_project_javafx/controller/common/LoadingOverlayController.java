package it.unical.ea_project_javafx.controller.common;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

public class LoadingOverlayController {

    @FXML
    public ProgressIndicator loadingSpinner;
    @FXML
    private Label label;
    @FXML
    private ImageView errorImage;

    public void setMessage(String message) {
        label.getStyleClass().removeAll("loading-error");
        if (!label.getStyleClass().contains("loading-text")) {
            label.getStyleClass().add("loading-text");
        }
        label.setText(message);
    }

    public void setError(String message) {
        loadingSpinner.setVisible(false);
        loadingSpinner.setManaged(false);
        errorImage.setManaged(true);
        errorImage.setVisible(true);
        label.getStyleClass().removeAll("loading-text");
        if (!label.getStyleClass().contains("loading-error")) {
            label.getStyleClass().add("loading-error");
        }
        label.setText(message);
    }

}
