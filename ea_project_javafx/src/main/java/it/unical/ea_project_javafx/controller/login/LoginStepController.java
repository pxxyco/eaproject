package it.unical.ea_project_javafx.controller.login;

import com.google.gson.JsonObject;
import it.unical.ea_project_javafx.dto.user.UserDTO;
import it.unical.ea_project_javafx.navigation.StepNavigator;
import it.unical.ea_project_javafx.security.TokenStorage;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.ViewSupport;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class LoginStepController {

    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private TextField visiblePasswordField;
    @FXML
    private Button revealPasswordButton;
    @FXML
    private Button btnLogin;
    @FXML
    private Label errorLabel;
    @FXML
    private CheckBox rememberMeCheckBox;

    private StepNavigator navigator;

    @FXML
    public void initialize() {

        updatePasswordEye(false);
        visiblePasswordField.textProperty().bindBidirectional(loginPasswordField.textProperty());
        visiblePasswordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                handleLoginSubmit(null);
        });
        loginUsernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                handleLoginSubmit(null);
        });
        loginPasswordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                handleLoginSubmit(null);
        });

    }

    @FXML
    private void togglePassword() {
        boolean reveal = !visiblePasswordField.isVisible();
        TextInputControl from = reveal ? loginPasswordField : visiblePasswordField;
        TextInputControl to = reveal ? visiblePasswordField : loginPasswordField;
        int caret = from.getCaretPosition();
        loginPasswordField.setVisible(!reveal);
        loginPasswordField.setManaged(!reveal);
        visiblePasswordField.setVisible(reveal);
        visiblePasswordField.setManaged(reveal);
        revealPasswordButton.setAccessibleText(reveal ? "Nascondi password" : "Mostra password");
        updatePasswordEye(reveal);
        to.requestFocus();
        to.positionCaret(caret);
    }

    private void updatePasswordEye(boolean revealed) {
        SVGPath eye = new SVGPath();
        eye.setContent("M1 8 Q10 -4 19 8 Q10 20 1 8 Z M13 8 A3 3 0 1 1 7 8 A3 3 0 1 1 13 8"
                + (revealed ? " M2 1 L18 15" : ""));
        eye.setFill(Color.TRANSPARENT);
        eye.setStroke(Color.web("#d6d3d1"));
        eye.setStrokeWidth(1.5);
        revealPasswordButton.setText("");
        revealPasswordButton.setGraphic(eye);
    }

    public void setNavigator(StepNavigator navigator) {

        this.navigator = navigator;
        btnLogin.setDefaultButton(true);

    }

    public void showError(String message) {
        ViewSupport.show(errorLabel, message);
    }

    public void clearError() {
        ViewSupport.clear(errorLabel);
    }

    @FXML
    void handleLoginSubmit(ActionEvent event) {

        clearError();
        String identifier = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Inserisci email o username e password.");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("identifier", identifier);
        body.addProperty("password", password);

        ApiService.post(
                ApiService.BASE_URL + "/api/users/login",
                body.toString(),
                res -> Platform.runLater(() -> {
                    if (res.statusCode() == 200) {
                        try {
                            String authHeader = res.headers().firstValue("Authorization").orElse("");
                            String accessToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
                            String refreshToken = res.headers().firstValue("X-Refresh-Token").orElse(null);

                            UserDTO user = ApiService.fromJson(res.body(), UserDTO.class);

                            if (user != null) {
                                UserSession.getInstance().setSession(user);
                                UserSession.getInstance().setTokens(accessToken, refreshToken);

                                if (rememberMeCheckBox != null && rememberMeCheckBox.isSelected()) {
                                    TokenStorage.saveRefreshToken(refreshToken);
                                } else {
                                    TokenStorage.clear();
                                }

                                navigator.goToHome();
                            } else {
                                showError("Errore il profilo utente è vuoto.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            showError("Errore nella lettura della risposta del server.");
                        }
                    } else {
                        if (res.statusCode() == 401) {
                            showError("Credenziali non valide.");
                        } else if (res.statusCode() >= 500) {
                            showError("Errore interno del server.");
                        } else {
                            showError("Impossibile effettuare il login.");
                        }
                    }
                }),
                () -> Platform.runLater(() -> showError("Errore di connessione al server.")),
                navigator::setLoading);

    }

    @FXML
    void goToRoleSelection(ActionEvent event) {

        clearError();
        navigator.goToRoleStep();

    }

    @FXML
    void goToForgotPassword(ActionEvent event) {

        clearError();
        navigator.goToForgotPasswordStep();

    }
}
