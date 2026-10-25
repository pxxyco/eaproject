package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.PasswordPolicy;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsController {

    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private Label statusMessageLabel;

    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private CheckBox emailBookingNotificationCheck;
    @FXML
    private CheckBox emailMarketingCheck;

    @FXML
    private VBox organizerSettingsSection;
    @FXML
    private TextField organizerBrandNameField;
    @FXML
    private TextArea organizerBioArea;

    @FXML
    private VBox travelerSettingsSection;
    @FXML
    private CheckBox publicProfileCheck;

    @FXML
    private Label passwordStrengthLabel;
    @FXML
    private Button passwordButton, preferencesButton;
    private boolean settingsLoaded;
    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {

        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupRoleSpecificSections();
        newPasswordField.textProperty()
                .addListener((o, a,
                        b) -> passwordStrengthLabel.setText(b.isEmpty() ? ""
                                : PasswordPolicy.isValid(b) ? "La password rispetta i requisiti."
                                        : "Servono almeno 8 caratteri, una lettera, un numero e un simbolo."));
        passwordStrengthLabel.managedProperty().bind(passwordStrengthLabel.textProperty().isNotEmpty());
        passwordStrengthLabel.visibleProperty().bind(passwordStrengthLabel.managedProperty());
        preferencesButton.setDisable(true);
        ApiService.callDetailed(ApiService.BASE_URL + "/api/account/settings", null, ApiService.Method.GET, r -> {
            var data = com.google.gson.JsonParser.parseString(r.body()).getAsJsonObject();
            emailBookingNotificationCheck.setSelected(data.get("bookingAlerts").getAsBoolean());
            emailMarketingCheck.setSelected(data.get("chatAlerts").getAsBoolean());
            publicProfileCheck.setSelected(data.get("personalized").getAsBoolean());
            if (data.has("brand") && !data.get("brand").isJsonNull())
                organizerBrandNameField.setText(data.get("brand").getAsString());
            if (data.has("bio") && !data.get("bio").isJsonNull())
                organizerBioArea.setText(data.get("bio").getAsString());
            settingsLoaded = true;
            preferencesButton.setDisable(false);
        }, this::showError, null);

    }

    private void loadNavbar() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setupRoleSpecificSections() {

        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(userSession.getRole());

        if (organizerSettingsSection != null) {
            organizerSettingsSection.setVisible(isOrganizer);
            organizerSettingsSection.setManaged(isOrganizer);
        }

        if (travelerSettingsSection != null) {
            travelerSettingsSection.setVisible(!isOrganizer);
            travelerSettingsSection.setManaged(!isOrganizer);
        }

    }

    @FXML
    private void handleChangePassword(ActionEvent event) {

        clearMessage();

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (blank(currentPassword) || blank(newPassword) || blank(confirmPassword)) {
            showError("Compila tutti i campi della password.");
            return;
        }

        if (!PasswordPolicy.isValid(newPassword)) {
            showError("La password deve contenere almeno 8 caratteri, una lettera, un numero e un carattere speciale.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("La nuova password e la conferma non coincidono.");
            return;
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("currentPassword", currentPassword);
        payload.put("newPassword", newPassword);

        ApiService.callDetailed(
                ApiService.BASE_URL + "/api/users/change-password",
                ApiService.toJson(payload),
                ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    userSession.clear();
                    it.unical.ea_project_javafx.util.TokenStorage.clear();
                    it.unical.ea_project_javafx.util.NotificationPollingService.getInstance().clearSessionState();
                    showSuccess("Password aggiornata. Le sessioni sono state chiuse: accedi di nuovo dal menu.");
                    loadNavbar();
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                }),
                message -> Platform.runLater(() -> showError(message)),
                passwordButton::setDisable);

    }

    @FXML
    private void handleSaveOrganizerInfo(ActionEvent event) {

        handleSavePreferences();
    }

    @FXML
    private void handleSavePreferences() {
        if (!settingsLoaded)
            return;
        var payload = new LinkedHashMap<String, Object>();
        payload.put("chatAlerts", emailMarketingCheck.isSelected());
        payload.put("bookingAlerts", emailBookingNotificationCheck.isSelected());
        payload.put("personalized", publicProfileCheck.isSelected());
        payload.put("brand", organizerBrandNameField.getText());
        payload.put("bio", organizerBioArea.getText());
        ApiService.callDetailed(ApiService.BASE_URL + "/api/account/settings", ApiService.toJson(payload),
                ApiService.Method.PUT, r -> showSuccess("Preferenze salvate."), this::showError,
                preferencesButton::setDisable);
    }

    @FXML
    private void handleRevokeSessions() {
        var root = it.unical.ea_project_javafx.util.PopupFactory.resolveRoot(navbarContainer);
        it.unical.ea_project_javafx.util.PopupFactory.showConfirmation(root, "Chiudere tutte le sessioni?",
                "Dovrai accedere nuovamente anche su questo dispositivo.",
                it.unical.ea_project_javafx.util.PopupFactory.Size.COMPACT, () -> ApiService
                        .callDetailed(ApiService.BASE_URL + "/api/users/logout", null, ApiService.Method.POST, r -> {
                            userSession.clear();
                            it.unical.ea_project_javafx.util.TokenStorage.clear();
                            it.unical.ea_project_javafx.util.NotificationPollingService.getInstance()
                                    .clearSessionState();
                            it.unical.ea_project_javafx.util.SceneNavigator.getInstance()
                                    .loadScene("/it/unical/ea_project_javafx/fxml/login/login.fxml", null);
                        }, this::showError, null));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void showError(String message) {
        if (statusMessageLabel != null) {
            statusMessageLabel.setText(message);
            statusMessageLabel.getStyleClass().setAll("status-message", "status-error");
            statusMessageLabel.setVisible(true);
            statusMessageLabel.setManaged(true);
        }
    }

    private void showSuccess(String message) {
        if (statusMessageLabel != null) {
            statusMessageLabel.setText(message);
            statusMessageLabel.getStyleClass().setAll("status-message", "status-success");
            statusMessageLabel.setVisible(true);
            statusMessageLabel.setManaged(true);
        }
    }

    private void clearMessage() {
        if (statusMessageLabel != null) {
            statusMessageLabel.setText("");
            statusMessageLabel.setVisible(false);
            statusMessageLabel.setManaged(false);
        }
    }
}