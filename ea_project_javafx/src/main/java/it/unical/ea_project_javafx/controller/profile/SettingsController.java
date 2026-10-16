package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
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

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private CheckBox emailBookingNotificationCheck;
    @FXML private CheckBox emailMarketingCheck;

    @FXML private VBox organizerSettingsSection;
    @FXML private TextField organizerBrandNameField;
    @FXML private TextArea organizerBioArea;

    @FXML private VBox travelerSettingsSection;
    @FXML private CheckBox publicProfileCheck;

    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupRoleSpecificSections();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml")
            );
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

        if (newPassword.length() < 6) {
            showError("La nuova password deve contenere almeno 6 caratteri.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("La nuova password e la conferma non coincidono.");
            return;
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("currentPassword", currentPassword);
        payload.put("newPassword", newPassword);

        ApiService.call(
                ApiService.BASE_URL + "/api/users/change-password",
                ApiService.toJson(payload),
                ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    showSuccess("Password aggiornata con successo!");
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                }),
                () -> Platform.runLater(() -> showError("Impossibile modificare la password. Verifica la password attuale.")),
                null
        );
    }

    @FXML
    private void handleSaveOrganizerInfo(ActionEvent event) {
        clearMessage();
        String brand = organizerBrandNameField.getText();
        String bio = organizerBioArea.getText();

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("brandName", brand);
        payload.put("bio", bio);

        ApiService.call(
                ApiService.BASE_URL + "/api/organizers/profile",
                ApiService.toJson(payload),
                ApiService.Method.PUT,
                response -> Platform.runLater(() -> showSuccess("Dati organizzatore salvati con successo!")),
                () -> Platform.runLater(() -> showError("Errore durante il salvataggio dei dati organizzatore.")),
                null
        );
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