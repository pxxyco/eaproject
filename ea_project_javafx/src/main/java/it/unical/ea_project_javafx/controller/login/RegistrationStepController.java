package it.unical.ea_project_javafx.controller.login;

import it.unical.ea_project_javafx.dto.user.UserDTO;
import it.unical.ea_project_javafx.navigation.StepNavigator;
import it.unical.ea_project_javafx.security.PasswordPolicy;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.ViewSupport;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import lombok.Setter;

//registrazione
public class RegistrationStepController {

    @FXML
    private Label wizardTitle;
    @FXML
    private Label errorLabel;
    @FXML
    private TextField regFirstNameField, regLastNameField, regUsernameField, regEmailField;
    @FXML
    private PasswordField regPasswordField, regConfirmPasswordField;
    @FXML
    private Button btnRegister;

    @Setter
    private StepNavigator navigator;
    private String selectedRole = "TRAVELER";

    @FXML
    public void initialize() {

        TextField[] fields = { regFirstNameField, regLastNameField, regUsernameField, regEmailField, regPasswordField,
                regConfirmPasswordField };
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
        ViewSupport.show(errorLabel, message);
    }

    public void clearError() {
        ViewSupport.clear(errorLabel);
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

        String jsonBody = ApiService.toJson(requestDto);

        ApiService.callDetailed(
                ApiService.BASE_URL + "/api/users",
                jsonBody,
                ApiService.Method.POST,
                res -> {
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
                },
                this::showError,
                navigator::setLoading);

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
