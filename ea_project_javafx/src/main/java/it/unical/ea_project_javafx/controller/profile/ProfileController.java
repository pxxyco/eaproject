package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.dto.UserDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.controller.common.NavbarController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ProfileController {

    private static final long MAX_PROFILE_PHOTO_BYTES = 5L * 1024L * 1024L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private StackPane avatarContainer;
    @FXML private ImageView profilePhotoImageView;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label roleValueLabel;
    @FXML private Label statusLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private Button savePhotoButton;
    @FXML private Button cancelPhotoButton;
    @FXML private Button removePhotoButton;

    private final UserSession userSession = UserSession.getInstance();
    private Path pendingPhotoPath;
    private NavbarController navbarController;
    private boolean profileFormDirty;
    private boolean updatingFormFields;
    private long photoLoadRequestId;

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        configureAvatar();
        loadNavbar();
        populateUserData(true);
        fullNameField.textProperty().addListener((observable, oldValue, newValue) -> markProfileFormDirty());
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> markProfileFormDirty());
        emailField.textProperty().addListener((observable, oldValue, newValue) -> markProfileFormDirty());
        refreshProfileFromServer();
    }

    private void configureAvatar() {
        if (profilePhotoImageView != null) {
            profilePhotoImageView.setFitWidth(104);
            profilePhotoImageView.setFitHeight(104);
            profilePhotoImageView.setPreserveRatio(true);
            profilePhotoImageView.setSmooth(true);
            profilePhotoImageView.setClip(new Circle(52, 52, 52));
            profilePhotoImageView.setVisible(false);
            profilePhotoImageView.setManaged(false);
        }
        updatePhotoButtons();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml")
            );
            Parent navbar = loader.load();
            navbarController = loader.getController();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Impossibile caricare la barra di navigazione.");
        }
    }

    private void populateUserData() {
        populateUserData(true);
    }

    private void populateUserData(boolean updateFormFields) {
        String fullName = nonBlankOr(userSession.getFullName(), userSession.getUsername());
        fullName = nonBlankOr(fullName, "Utente");
        fullNameLabel.setText(fullName);
        if (updateFormFields) {
            updatingFormFields = true;
            try {
                fullNameField.setText(nonBlankOr(userSession.getFullName(), ""));
                usernameField.setText(nonBlankOr(userSession.getUsername(), ""));
                emailField.setText(nonBlankOr(userSession.getEmail(), ""));
            } finally {
                updatingFormFields = false;
            }
            profileFormDirty = false;
        }

        String role = userSession.getRole() != null ? userSession.getRole().toUpperCase() : "UTENTE";
        roleBadgeLabel.setText(role);
        roleValueLabel.setText("ORGANIZER".equals(role) ? "Organizzatore / Partner" : "Viaggiatore (Traveler)");
        avatarInitialsLabel.setText(createInitials(fullName));
        displaySavedPhoto(userSession.getProfilePhotoUrl());
        if (navbarController != null) navbarController.updateNavbarState();
    }

    private void refreshProfileFromServer() {
        ApiService.get(
                ApiService.BASE_URL + "/api/users/me",
                response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            UserDTO currentUser = ApiService.fromJson(response.body(), UserDTO.class);
                            if (currentUser != null) {
                                userSession.setSession(currentUser);
                                populateUserData(!profileFormDirty);
                            }
                        } catch (Exception e) {
                            showError("I dati del profilo non sono stati aggiornati dal server.");
                        }
                    }
                }),
                () -> { /* Conserva i dati di sessione già disponibili se il server non risponde. */ },
                null
        );
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        clearMessage();
        String fullName = value(fullNameField);
        String username = value(usernameField);
        String email = value(emailField);

        if (fullName.isBlank() || fullName.length() > 120) {
            showError("Il nome completo è obbligatorio e non può superare 120 caratteri.");
            fullNameField.requestFocus();
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            showError("Lo username deve contenere 3–30 caratteri: lettere, numeri, punto, trattino o underscore.");
            usernameField.requestFocus();
            return;
        }
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            showError("Inserisci un indirizzo email valido.");
            emailField.requestFocus();
            return;
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("fullName", fullName);
        payload.put("username", username);
        payload.put("email", email);

        ApiService.callDetailed(
                ApiService.BASE_URL + "/api/users/me",
                ApiService.toJson(payload),
                ApiService.Method.PUT,
                response -> Platform.runLater(() -> {
                    try {
                        UserDTO updated = ApiService.fromJson(response.body(), UserDTO.class);
                        if (updated == null) {
                            showError("Il server non ha restituito il profilo aggiornato.");
                            return;
                        }
                        userSession.setSession(updated);
                        populateUserData(true);
                        showSuccess("Profilo aggiornato correttamente.");
                    } catch (Exception exception) {
                        showError("Il profilo è stato salvato, ma non è stato possibile aggiornare la schermata.");
                        refreshProfileFromServer();
                    }
                }),
                message -> Platform.runLater(() -> showError(message)),
                null
        );
    }

    @FXML
    private void handleChooseProfilePhoto(ActionEvent event) {
        clearMessage();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Scegli la foto del profilo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Immagini supportate", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        Window owner = avatarContainer != null && avatarContainer.getScene() != null
                ? avatarContainer.getScene().getWindow() : null;
        File chosen = chooser.showOpenDialog(owner);
        if (chosen == null) return;

        try {
            Path path = chosen.toPath();
            long size = Files.size(path);
            if (size <= 0) {
                showError("Il file selezionato è vuoto.");
                return;
            }
            if (size > MAX_PROFILE_PHOTO_BYTES) {
                showError("La foto del profilo non può superare 5 MB.");
                return;
            }
            int[] dimensions = readImageDimensions(path);
            if (dimensions == null) {
                showError("Il file selezionato non è un'immagine valida o supportata.");
                return;
            }
            long pixels = (long) dimensions[0] * dimensions[1];
            if (dimensions[0] < 1 || dimensions[1] < 1 || dimensions[0] > 8000
                    || dimensions[1] > 8000 || pixels > 25_000_000L) {
                showError("La foto ha dimensioni eccessive. Riducila prima di caricarla.");
                return;
            }
            // Load a small preview, not a full-resolution bitmap, to limit memory usage.
            Image preview = new Image(path.toUri().toString(), 240, 240, true, true, false);
            if (preview.isError() || preview.getWidth() <= 0 || preview.getHeight() <= 0) {
                showError("Il file selezionato non è un'immagine valida o supportata.");
                return;
            }
            pendingPhotoPath = path;
            showPhoto(preview);
            updatePhotoButtons();
            showSuccess("Anteprima pronta. Premi “Carica foto” per salvare la nuova immagine.");
        } catch (IOException | SecurityException exception) {
            showError("Non è stato possibile leggere il file selezionato.");
        }
    }

    @FXML
    private void handleUploadProfilePhoto(ActionEvent event) {
        clearMessage();
        if (pendingPhotoPath == null || !Files.isRegularFile(pendingPhotoPath)) {
            showError("Scegli prima una foto del profilo.");
            return;
        }
        ApiService.uploadFileDetailed(
                ApiService.BASE_URL + "/api/users/me/profile-photo",
                pendingPhotoPath,
                response -> Platform.runLater(() -> {
                    try {
                        UserDTO updated = ApiService.fromJson(response.body(), UserDTO.class);
                        if (updated == null) {
                            showError("Il server non ha restituito i dati della foto aggiornata.");
                            return;
                        }
                        userSession.setSession(updated);
                        pendingPhotoPath = null;
                        populateUserData(false);
                        updatePhotoButtons();
                        showSuccess("Foto del profilo aggiornata correttamente.");
                    } catch (Exception exception) {
                        refreshProfileFromServer();
                        showError("La foto è stata inviata, ma non è stato possibile aggiornare l'anteprima.");
                    }
                }),
                () -> Platform.runLater(() -> showError("Caricamento della foto non riuscito. Verifica il file e la connessione.")),
                message -> Platform.runLater(() -> showError(translateUploadError(message))),
                null
        );
    }

    @FXML
    private void handleCancelPhotoSelection(ActionEvent event) {
        pendingPhotoPath = null;
        displaySavedPhoto(userSession.getProfilePhotoUrl());
        updatePhotoButtons();
        clearMessage();
    }

    @FXML
    private void handleRemoveProfilePhoto(ActionEvent event) {
        clearMessage();
        if (userSession.getProfilePhotoUrl() == null || userSession.getProfilePhotoUrl().isBlank()) {
            pendingPhotoPath = null;
            displaySavedPhoto(null);
            updatePhotoButtons();
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Rimuovi foto del profilo");
        confirmation.setHeaderText("Vuoi rimuovere la foto del profilo?");
        confirmation.setContentText("La foto salvata verrà eliminata. Potrai caricarne un'altra in qualsiasi momento.");
        if (avatarContainer != null && avatarContainer.getScene() != null) {
            confirmation.initOwner(avatarContainer.getScene().getWindow());
        }
        if (confirmation.showAndWait().filter(button -> button == javafx.scene.control.ButtonType.OK).isEmpty()) return;

        ApiService.callDetailed(
                ApiService.BASE_URL + "/api/users/me/profile-photo",
                null,
                ApiService.Method.DELETE,
                response -> Platform.runLater(() -> {
                    try {
                        UserDTO updated = ApiService.fromJson(response.body(), UserDTO.class);
                        if (updated != null) userSession.setSession(updated);
                        pendingPhotoPath = null;
                        populateUserData(false);
                        updatePhotoButtons();
                        showSuccess("Foto del profilo rimossa.");
                    } catch (Exception exception) {
                        refreshProfileFromServer();
                        showError("La foto è stata rimossa, ma non è stato possibile aggiornare la schermata.");
                    }
                }),
                message -> Platform.runLater(() -> showError(message)),
                null
        );
    }

    private int[] readImageDimensions(Path path) throws IOException {
        try (InputStream stream = Files.newInputStream(path);
             ImageInputStream imageInput = ImageIO.createImageInputStream(stream)) {
            if (imageInput == null) return null;
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    private void displaySavedPhoto(String photoUrl) {
        long requestId = ++photoLoadRequestId;
        if (pendingPhotoPath != null) return;
        if (photoUrl == null || photoUrl.isBlank()) {
            showPhoto(null);
            avatarInitialsLabel.setVisible(true);
            avatarInitialsLabel.setManaged(true);
            return;
        }
        String url = ApiService.resolveUrl(photoUrl) + "?v=" + System.currentTimeMillis();
        Image savedImage = new Image(url, true);
        savedImage.errorProperty().addListener((observable, wasError, isError) -> {
            if (isError) {
                Platform.runLater(() -> {
                    if (pendingPhotoPath == null && requestId == photoLoadRequestId) showPhoto(null);
                });
            }
        });
        savedImage.progressProperty().addListener((observable, oldProgress, progress) -> {
            if (progress.doubleValue() >= 1.0 && !savedImage.isError()) {
                Platform.runLater(() -> {
                    if (pendingPhotoPath == null && requestId == photoLoadRequestId) showPhoto(savedImage);
                });
            }
        });
        if (savedImage.getProgress() >= 1.0 && !savedImage.isError() && requestId == photoLoadRequestId) showPhoto(savedImage);
    }

    private void showPhoto(Image image) {
        boolean hasPhoto = image != null && !image.isError();
        profilePhotoImageView.setImage(hasPhoto ? image : null);
        profilePhotoImageView.setVisible(hasPhoto);
        profilePhotoImageView.setManaged(hasPhoto);
        avatarInitialsLabel.setVisible(!hasPhoto);
        avatarInitialsLabel.setManaged(!hasPhoto);
    }

    private void updatePhotoButtons() {
        boolean hasSavedPhoto = userSession.getProfilePhotoUrl() != null && !userSession.getProfilePhotoUrl().isBlank();
        if (savePhotoButton != null) {
            savePhotoButton.setVisible(pendingPhotoPath != null);
            savePhotoButton.setManaged(pendingPhotoPath != null);
        }
        if (cancelPhotoButton != null) {
            cancelPhotoButton.setVisible(pendingPhotoPath != null);
            cancelPhotoButton.setManaged(pendingPhotoPath != null);
        }
        if (removePhotoButton != null) {
            removePhotoButton.setVisible(hasSavedPhoto);
            removePhotoButton.setManaged(hasSavedPhoto);
        }
    }

    private String createInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] words = fullName.trim().split("\\s+");
        String first = words[0].substring(0, 1).toUpperCase();
        if (words.length == 1) return first;
        return first + words[words.length - 1].substring(0, 1).toUpperCase();
    }

    private void markProfileFormDirty() {
        if (!updatingFormFields) profileFormDirty = true;
    }

    private String value(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String nonBlankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String translateUploadError(String message) {
        if (message == null || message.isBlank()) {
            return "Caricamento della foto non riuscito.";
        }
        java.util.regex.Matcher matcher = Pattern.compile("Errore HTTP (\\d{3})(?::\\s*(.*))?", Pattern.DOTALL).matcher(message);
        if (matcher.find()) {
            int status = Integer.parseInt(matcher.group(1));
            String body = matcher.group(2) == null ? "" : matcher.group(2).trim();
            return ApiService.extractErrorMessage(body, status);
        }
        if (message.startsWith("Errore durante la richiesta di upload:")) {
            return "Impossibile caricare la foto: verifica la connessione al server e riprova.";
        }
        return message;
    }

    private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.getStyleClass().setAll("status-message", "status-error");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void showSuccess(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.getStyleClass().setAll("status-message", "status-success");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void clearMessage() {
        if (statusLabel != null) {
            statusLabel.setText("");
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }
    }

    @FXML
    private void handleGoToSettings(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/settings.fxml", null);
    }
}
