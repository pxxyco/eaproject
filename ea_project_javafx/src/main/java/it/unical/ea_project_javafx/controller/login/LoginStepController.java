package it.unical.ea_project_javafx.controller.login;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import it.unical.ea_project_javafx.dto.UserDTO;
import it.unical.ea_project_javafx.navigation.StepNavigator;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.TokenStorage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoginStepController {

    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button btnLogin;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberMeCheckBox;

    private StepNavigator navigator;

    @FXML
    public void initialize() {

        loginUsernameField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleLoginSubmit(null); });
        loginPasswordField.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) handleLoginSubmit(null); });

    }

    public void setNavigator(StepNavigator navigator) {
    
        this.navigator = navigator;
        btnLogin.setDefaultButton(true);

    }

    public void showError(String message) {

        Platform.runLater(() -> {
            if (errorLabel != null) {
                errorLabel.setText(message);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });

    }

    public void clearError() {

        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

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

                            Gson gson = new GsonBuilder()
                                    .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
                                    .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString()))
                                    .create();

                            UserDTO user = gson.fromJson(res.body(), UserDTO.class);

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
                navigator::setLoading
        );

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