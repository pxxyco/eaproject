package it.unical.ea_project_javafx.controller.login;

import it.unical.ea_project_javafx.navigation.StepNavigator;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ForgotPasswordController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @FXML private TextField emailField;
    @FXML private Button btnResetPassword;
    @FXML private Label messageLabel;

    private StepNavigator navigator;

    public void setNavigator(StepNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    void handleResetPassword(ActionEvent event) {

        clearMessage();
        String email = emailField.getText().trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Inserisci un indirizzo email valido.");
            return;
        }

        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        ApiService.get(
                ApiService.BASE_URL + "/api/users/exists?email=" + encodedEmail,
                response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        showSuccess("La mail per il recupero è stata inviata all'indirizzo email corrispondente.");
                    } else if (response.statusCode() == 404) {
                        showError("La mail non esiste.");
                    } else {
                        showError("Non è stato possibile verificare la mail.");
                    }
                }),
                () -> Platform.runLater(() -> showError("Errore di connessione al server.")),
                navigator::setLoading
        );

    }

    @FXML
    void goToLogin(ActionEvent event) {

        clearMessage();
        navigator.goToLoginStep();

    }

    public void showError(String message) {
        showMessage(message, "message-error");
    }

    private void showSuccess(String message) {
        showMessage(message, "message-success");
    }

    private void showMessage(String message, String styleClass) {

        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.getStyleClass().add(styleClass);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

    }

    public void clearMessage() {

        messageLabel.setText("");
        messageLabel.getStyleClass().removeAll("message-error", "message-success");
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

    }
}