package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Gestisce il popup in-scene per aggiungere, modificare e revocare condivisioni. */
public class ListSharingPopupController {
    private static final Pattern ID_PATTERN = Pattern.compile("#(\\d+)\\s*$");

    @FXML private Label popupTitleLabel;
    @FXML private Label sharingStatusLabel;
    @FXML private ListView<String> sharingListView;
    @FXML private TextField userIdField;
    @FXML private ComboBox<String> permissionCombo;
    @FXML private Button updatePermissionButton;
    @FXML private Button revokeAccessButton;

    private Long listId;
    private final Map<Long, JsonObject> sharingByUserId = new LinkedHashMap<>();
    private Runnable closeAction = () -> {};

    @FXML
    private void initialize() {
        permissionCombo.setItems(FXCollections.observableArrayList("VIEW", "EDIT"));
        permissionCombo.getSelectionModel().select("VIEW");
        sharingListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> {
            boolean selected = value != null;
            updatePermissionButton.setDisable(!selected);
            revokeAccessButton.setDisable(!selected);
            if (selected) {
                Long userId = extractId(value);
                JsonObject sharing = userId == null ? null : sharingByUserId.get(userId);
                if (sharing != null) permissionCombo.setValue(stringValue(sharing, "permission", "VIEW"));
            }
        });
    }

    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction == null ? () -> {} : closeAction;
    }

    public void open(Long listId, String listName) {
        this.listId = listId;
        popupTitleLabel.setText("Condivisioni · " + (listName == null ? "Lista" : listName));
        userIdField.clear();
        permissionCombo.setValue("VIEW");
        sharingStatusLabel.setText("Caricamento utenti autorizzati…");
        loadSharings();
    }

    @FXML private void handleClose() { closeAction.run(); }

    @FXML
    private void handleShare() {
        if (listId == null) return;
        String rawId = userIdField.getText();
        long userId;
        try {
            userId = Long.parseLong(rawId == null ? "" : rawId.trim());
            if (userId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            setStatus("Inserisci un ID utente numerico positivo.", true);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listId", listId);
        payload.put("userId", userId);
        payload.put("permission", permissionCombo.getValue() == null ? "VIEW" : permissionCombo.getValue());
        setStatus("Condivisione in corso…", false);
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings", ApiService.toJson(payload), ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    userIdField.clear();
                    setStatus("Lista condivisa con l'utente #" + userId + ".", false);
                    loadSharings();
                }),
                () -> Platform.runLater(() -> setStatus("Condivisione non riuscita. Verifica l'ID e che l'utente non abbia già accesso.", true)), null);
    }

    @FXML
    private void handleUpdatePermission() {
        Long userId = extractId(sharingListView.getSelectionModel().getSelectedItem());
        if (listId == null || userId == null) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listId", listId);
        payload.put("userId", userId);
        payload.put("permission", permissionCombo.getValue() == null ? "VIEW" : permissionCombo.getValue());
        setStatus("Aggiornamento permesso…", false);
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/permission", ApiService.toJson(payload), ApiService.Method.PUT,
                response -> Platform.runLater(() -> { setStatus("Permesso aggiornato.", false); loadSharings(); }),
                () -> Platform.runLater(() -> setStatus("Aggiornamento del permesso non riuscito.", true)), null);
    }

    @FXML
    private void handleRevokeAccess() {
        Long userId = extractId(sharingListView.getSelectionModel().getSelectedItem());
        if (listId == null || userId == null) return;
        setStatus("Revoca dell'accesso in corso…", false);
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/list/" + listId + "/user/" + userId, null, ApiService.Method.DELETE,
                response -> Platform.runLater(() -> { setStatus("Accesso revocato.", false); loadSharings(); }),
                () -> Platform.runLater(() -> setStatus("Revoca dell'accesso non riuscita.", true)), null);
    }

    private void loadSharings() {
        if (listId == null) return;
        Long requestedListId = listId;
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/list/" + requestedListId, null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    if (!requestedListId.equals(listId)) return;
                    try {
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        sharingListView.getItems().clear();
                        sharingByUserId.clear();
                        for (JsonElement element : array) {
                            JsonObject item = element.getAsJsonObject();
                            Long userId = item.get("userId").getAsLong();
                            sharingByUserId.put(userId, item);
                            String username = stringValue(item, "username", "Utente");
                            String email = stringValue(item, "email", "");
                            String permission = stringValue(item, "permission", "VIEW");
                            sharingListView.getItems().add(username + (email.isBlank() ? "" : " (" + email + ")")
                                    + " — " + permission + " #" + userId);
                        }
                        setStatus(array.size() == 0 ? "Nessun utente ha accesso a questa lista." : "Utenti con accesso: " + array.size(), false);
                    } catch (Exception ex) {
                        setStatus("Impossibile interpretare la risposta del server.", true);
                    }
                }),
                () -> Platform.runLater(() -> setStatus("Impossibile caricare le condivisioni.", true)), null);
    }

    private void setStatus(String message, boolean error) {
        sharingStatusLabel.setText(message);
        sharingStatusLabel.getStyleClass().removeAll("sharing-status-error", "sharing-status-success");
        sharingStatusLabel.getStyleClass().add(error ? "sharing-status-error" : "sharing-status-success");
    }

    private Long extractId(String value) {
        if (value == null) return null;
        Matcher matcher = ID_PATTERN.matcher(value);
        if (!matcher.find()) return null;
        try { return Long.parseLong(matcher.group(1)); }
        catch (NumberFormatException ex) { return null; }
    }

    private String stringValue(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }
}
