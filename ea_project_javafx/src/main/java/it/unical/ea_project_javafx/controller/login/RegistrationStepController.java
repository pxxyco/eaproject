package it.unical.ea_project_javafx.controller.login;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import it.unical.ea_project_javafx.dto.UserDTO;
import it.unical.ea_project_javafx.model.StepNavigator;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.PasswordPolicy;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//registrazione
public class RegistrationStepController {

    @FXML private Label wizardTitle;
    @FXML private Label errorLabel;
    @FXML private TextField regFirstNameField, regLastNameField, regUsernameField, regEmailField;
    @FXML private PasswordField regPasswordField, regConfirmPasswordField;
    @FXML private Button btnRegister;

    @Setter
    private StepNavigator navigator;
    private String selectedRole = "TRAVELER";

    @FXML
    public void initialize() {
        TextField[] fields = {regFirstNameField, regLastNameField, regUsernameField, regEmailField, regPasswordField, regConfirmPasswordField};
        for (TextField field : fields) {
            if (field != null) {
                field.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        handleRegistrationSubmit(null);
                    }
                });
            }
        }
    }

    public void setSelectedRole(String role) {
        this.selectedRole = role;
        wizardTitle.setText(role.equals("ORGANIZER") ? "Profilo Business" : "Profilo Personal");
        btnRegister.setDefaultButton(true);
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
    void backToRoleSelection() {
        clearError();
        navigator.goToRoleStep();
    }

    @FXML
    void handleRegistrationSubmit(ActionEvent event) {
        clearError();
        String firstName = regFirstNameField.getText().trim();
        String lastName = regLastNameField.getText().trim();
        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty()
                || email.isEmpty() || password.isEmpty() || !password.equals(confirmPassword)) {
            showError("Verifica che tutti i campi siano compilati e che le password coincidano.");
            return;
        }
        if (!PasswordPolicy.isValid(password)) {
            showError("La password deve contenere almeno 8 caratteri, una lettera, un numero e un carattere speciale.");
            return;
        }
        if (!email.contains("@")) {
            showError("Inserisci un'email valida.");
            return;
        }

        UserDTO requestDto = UserDTO.builder()
                .username(username)
                .fullName(firstName + " " + lastName)
                .email(email)
                .password(password)
                .role(selectedRole)
                .oauthProvider(false)
                .build();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        src == null ? null : new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString()))
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                        src == null ? null : new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                        LocalDate.parse(json.getAsString()))
                .create();

        String jsonBody = gson.toJson(requestDto);

        ApiService.post(
                ApiService.BASE_URL + "/api/users",
                jsonBody,
                res -> Platform.runLater(() -> {
                    if (res.statusCode() == 200 || res.statusCode() == 201) {
                        clearFields();
                        navigator.showSuccessMessage("Puoi accedere al tuo account.");
                    } else if (res.statusCode() == 409) {
                        showError("Username o Email già registrati.");
                    } else if (res.statusCode() == 400) {
                        showError("La password non rispetta i requisiti minimi.");
                    } else {
                        showError("Impossibile completare la registrazione.");
                    }
                }),
                () -> Platform.runLater(() -> {
                    //it.unical.ea_project_javafx.util.ViewNavigator.loadScene(btnRegister, "/it/unical/ea_project_javafx/fxml/pre-main.fxml", false);
                    SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/pre-main.fxml");
                }),
                navigator::setLoading
        );
    }

    private void clearFields() {
        regFirstNameField.clear();
        regLastNameField.clear();
        regUsernameField.clear();
        regEmailField.clear();
        regPasswordField.clear();
        regConfirmPasswordField.clear();
    }
}