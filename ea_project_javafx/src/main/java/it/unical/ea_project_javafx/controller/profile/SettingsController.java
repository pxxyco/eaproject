package it.unical.ea_project_javafx.controller.profile;

import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.security.PasswordPolicy;
import it.unical.ea_project_javafx.security.TokenStorage;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.service.notification.NotificationPollingService;
import it.unical.ea_project_javafx.ui.UiText;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
        ViewSupport.loadNavbar(navbarContainer);
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
            var data = JsonParser.parseString(r.body()).getAsJsonObject();
            emailBookingNotificationCheck.setSelected(data.get("bookingAlerts").getAsBoolean());
            emailMarketingCheck.setSelected(data.get("chatAlerts").getAsBoolean());
            publicProfileCheck.setSelected(data.get("personalized").getAsBoolean());
            if (data.has("brand") && !data.get("brand").isJsonNull())
                organizerBrandNameField.setText(data.get("brand").getAsString());
            if (data.has("bio") && !data.get("bio").isJsonNull())
                organizerBioArea.setText(data.get("bio").getAsString());
            settingsLoaded = true;
            preferencesButton.setDisable(false);
        }, message -> ViewSupport.error(statusMessageLabel, message), null);

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

        ViewSupport.clear(statusMessageLabel);

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (UiText.isBlank(currentPassword) || UiText.isBlank(newPassword) || UiText.isBlank(confirmPassword)) {
            ViewSupport.error(statusMessageLabel, "Compila tutti i campi della password.");
            return;
        }

        if (!PasswordPolicy.isValid(newPassword)) {
            ViewSupport.error(statusMessageLabel,
                    "La password deve contenere almeno 8 caratteri, una lettera, un numero e un carattere speciale.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            ViewSupport.error(statusMessageLabel, "La nuova password e la conferma non coincidono.");
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
                    TokenStorage.clear();
                    NotificationPollingService.getInstance().clearSessionState();
                    ViewSupport.success(statusMessageLabel,
                            "Password aggiornata. Le sessioni sono state chiuse: accedi di nuovo dal menu.");
                    ViewSupport.loadNavbar(navbarContainer);
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                }),
                message -> Platform.runLater(() -> ViewSupport.error(statusMessageLabel, message)),
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
                ApiService.Method.PUT, r -> ViewSupport.success(statusMessageLabel, "Preferenze salvate."),
                message -> ViewSupport.error(statusMessageLabel, message),
                preferencesButton::setDisable);
    }

    @FXML
    private void handleRevokeSessions() {
        var root = PopupFactory.resolveRoot(navbarContainer);
        PopupFactory.showConfirmation(root, "Chiudere tutte le sessioni?",
                "Dovrai accedere nuovamente anche su questo dispositivo.",
                PopupFactory.Size.COMPACT, () -> ApiService
                        .callDetailed(ApiService.BASE_URL + "/api/users/logout", null, ApiService.Method.POST, r -> {
                            userSession.clear();
                            TokenStorage.clear();
                            NotificationPollingService.getInstance()
                                    .clearSessionState();
                            SceneNavigator.getInstance()
                                    .loadScene("/it/unical/ea_project_javafx/fxml/login/login.fxml", null);
                        }, message -> ViewSupport.error(statusMessageLabel, message), null));
    }

}
