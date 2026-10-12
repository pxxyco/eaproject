package it.unical.ea_project_javafx.controller.login;

import it.unical.ea_project_javafx.navigation.StepNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class FeedbackOverlayController {

    @FXML private Label messageLabel;
    @FXML private Button btnAction;

    private StepNavigator navigator;

    public void setNavigator(StepNavigator navigator) {
        this.navigator = navigator;
    }

    public void showSuccess(String message) {
        messageLabel.setText(message);
    }

    @FXML
    private void handleContinue() {

        if (navigator != null) {
            navigator.goToLoginStep();
        }

    }

    public void hide() {
        messageLabel.setText("");
    }
}
