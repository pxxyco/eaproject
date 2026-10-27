package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.util.JsonCodec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Gestisce il popup in-scene per aggiungere, modificare e revocare
 * condivisioni.
 */
public class ListSharingPopupController {
    private static final Pattern ID_PATTERN = Pattern.compile("#(\\d+)\\s*$");

    @FXML
    private Label popupTitleLabel;
    @FXML
    private Label sharingStatusLabel;
    @FXML
    private ListView<String> sharingListView;
    @FXML
    private TextField userIdentifierField;
    @FXML
    private ListView<UserSearchRow> userSearchResultsListView;
    @FXML
    private ComboBox<String> permissionCombo;
    @FXML
    private ComboBox<String> newPermissionCombo;
    @FXML
    private Button updatePermissionButton;
    @FXML
    private Button revokeAccessButton;

    private Long listId;
    private final Map<Long, JsonObject> sharingByUserId = new LinkedHashMap<>();
    private final Map<String, Long> sharingIdByDisplay = new LinkedHashMap<>();
    private UserSearchRow selectedRecipient;
    private boolean selectingSuggestion;
    private long searchRequestVersion;
    private PauseTransition searchDebounce;
    private Runnable closeAction = () -> {
    };

    @FXML
    private void initialize() {
        permissionCombo.setItems(FXCollections.observableArrayList("VIEW", "EDIT"));
        permissionCombo.getSelectionModel().select("VIEW");
        if (newPermissionCombo != null) {
            newPermissionCombo.setItems(FXCollections.observableArrayList("VIEW", "EDIT"));
            newPermissionCombo.getSelectionModel().select("VIEW");
        }
        if (userSearchResultsListView != null) {
            userSearchResultsListView.setItems(FXCollections.observableArrayList());
            userSearchResultsListView
                    .setPlaceholder(new Label("Digita almeno 2 caratteri per cercare per username o email."));
            userSearchResultsListView.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(UserSearchRow row, boolean empty) {
                    super.updateItem(row, empty);
                    setText(empty || row == null ? null : row.displayText());
                }
            });
            userSearchResultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> {
                if (value == null)
                    return;
                selectedRecipient = value;
                selectingSuggestion = true;
                userIdentifierField.setText(value.username());
                selectingSuggestion = false;
                if (searchDebounce != null)
                    searchDebounce.stop();
                setStatus("Destinatario selezionato: @" + value.username() + ".", false);
            });
        }
        searchDebounce = new PauseTransition(Duration.millis(320));
        searchDebounce.setOnFinished(event -> searchUsers(userIdentifierField.getText()));
        userIdentifierField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (selectingSuggestion)
                return;
            selectedRecipient = null;
            searchRequestVersion++;
            if (searchDebounce != null)
                searchDebounce.stop();
            if (userSearchResultsListView != null)
                userSearchResultsListView.getItems().clear();
            if (newValue != null && newValue.trim().length() >= 2 && listId != null)
                searchDebounce.playFromStart();
        });
        sharingListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> {
            boolean selected = value != null;
            updatePermissionButton.setDisable(!selected);
            revokeAccessButton.setDisable(!selected);
            if (selected) {
                Long userId = sharingIdByDisplay.get(value);
                if (userId == null)
                    userId = extractId(value);
                JsonObject sharing = userId == null ? null : sharingByUserId.get(userId);
                if (sharing != null)
                    permissionCombo.setValue(JsonCodec.string(sharing, "permission", "VIEW"));
            }
        });
    }

    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction == null ? () -> {
        } : closeAction;
    }

    public void open(Long listId, String listName) {
        this.listId = listId;
        popupTitleLabel.setText("Condivisioni · " + (listName == null ? "Lista" : listName));
        selectedRecipient = null;
        searchRequestVersion++;
        if (searchDebounce != null)
            searchDebounce.stop();
        userIdentifierField.clear();
        if (userSearchResultsListView != null)
            userSearchResultsListView.getItems().clear();
        permissionCombo.setValue("VIEW");
        if (newPermissionCombo != null)
            newPermissionCombo.setValue("VIEW");
        sharingStatusLabel.setText("Caricamento utenti autorizzati…");
        loadSharings();
    }

    @FXML
    private void handleClose() {
        closeAction.run();
    }

    @FXML
    private void handleShare() {
        if (listId == null)
            return;
        String identifier = selectedRecipient != null ? selectedRecipient.username()
                : (userIdentifierField.getText() == null ? "" : userIdentifierField.getText().trim());
        if (identifier.isBlank() || identifier.length() < 2) {
            setStatus(
                    "Inserisci uno username o un indirizzo email valido; puoi selezionare una corrispondenza dalla ricerca.",
                    true);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listId", listId);
        payload.put("identifier", identifier);
        payload.put("permission", newPermissionCombo == null || newPermissionCombo.getValue() == null ? "VIEW"
                : newPermissionCombo.getValue());
        setStatus("Invio dell'invito in corso…", false);
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings", ApiService.toJson(payload), ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    userIdentifierField.clear();
                    selectedRecipient = null;
                    if (userSearchResultsListView != null)
                        userSearchResultsListView.getItems().clear();
                    setStatus("Invito inviato. L'utente dovrà accettare la notifica per accedere alla lista.", false);
                    loadSharings();
                }),
                () -> Platform.runLater(() -> setStatus(
                        "Invito non riuscito. Verifica che username/email sia corretto e che non esista già un invito pendente.",
                        true)),
                null);
    }

    private void searchUsers(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (listId == null || query.length() < 2 || userSearchResultsListView == null)
            return;
        long requestVersion = searchRequestVersion;
        Long requestedListId = listId;
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        userSearchResultsListView.setPlaceholder(new Label("Ricerca utenti…"));
        ApiService.get(
                ApiService.BASE_URL + "/api/list-sharings/search-users?listId=" + requestedListId + "&q=" + encoded,
                response -> Platform.runLater(() -> {
                    if (requestVersion != searchRequestVersion || !requestedListId.equals(listId)
                            || !query.equalsIgnoreCase(
                                    userIdentifierField.getText() == null ? "" : userIdentifierField.getText().trim()))
                        return;
                    try {
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        List<UserSearchRow> rows = new ArrayList<>();
                        for (JsonElement element : array) {
                            if (!element.isJsonObject())
                                continue;
                            JsonObject user = element.getAsJsonObject();
                            rows.add(new UserSearchRow(JsonCodec.string(user, "username", "Utente"),
                                    JsonCodec.string(user, "email", "")));
                        }
                        userSearchResultsListView.getItems().setAll(rows);
                        userSearchResultsListView.setPlaceholder(new Label(rows.isEmpty()
                                ? "Nessun utente disponibile. Puoi comunque inserire username o email esatti."
                                : "Seleziona il destinatario da invitare."));
                        if (rows.isEmpty())
                            setStatus("Nessuna corrispondenza disponibile per questa lista.", false);
                    } catch (Exception ex) {
                        userSearchResultsListView
                                .setPlaceholder(new Label("Impossibile interpretare i risultati della ricerca."));
                    }
                }),
                () -> Platform.runLater(() -> {
                    if (requestVersion == searchRequestVersion) {
                        userSearchResultsListView.setPlaceholder(
                                new Label("Ricerca non disponibile. Puoi digitare username o email esatti."));
                    }
                }), null);
    }

    @FXML
    private void handleUpdatePermission() {
        String selected = sharingListView.getSelectionModel().getSelectedItem();
        Long userId = selected == null ? null : sharingIdByDisplay.get(selected);
        if (userId == null)
            userId = extractId(selected);
        if (listId == null || userId == null)
            return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listId", listId);
        payload.put("userId", userId);
        payload.put("permission", permissionCombo.getValue() == null ? "VIEW" : permissionCombo.getValue());
        setStatus("Aggiornamento permesso…", false);
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/permission", ApiService.toJson(payload),
                ApiService.Method.PUT,
                response -> Platform.runLater(() -> {
                    setStatus("Permesso aggiornato.", false);
                    loadSharings();
                }),
                () -> Platform.runLater(() -> setStatus("Aggiornamento del permesso non riuscito.", true)), null);
    }

    @FXML
    private void handleRevokeAccess() {
        String selected = sharingListView.getSelectionModel().getSelectedItem();
        Long userId = selected == null ? null : sharingIdByDisplay.get(selected);
        if (userId == null)
            userId = extractId(selected);
        if (listId == null || userId == null)
            return;
        setStatus("Revoca dell'accesso in corso…", false);
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/list/" + listId + "/user/" + userId, null,
                ApiService.Method.DELETE,
                response -> Platform.runLater(() -> {
                    setStatus("Accesso revocato.", false);
                    loadSharings();
                }),
                () -> Platform.runLater(() -> setStatus("Revoca dell'accesso non riuscita.", true)), null);
    }

    private void loadSharings() {
        if (listId == null)
            return;
        Long requestedListId = listId;
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/list/" + requestedListId, null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    if (!requestedListId.equals(listId))
                        return;
                    try {
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        sharingListView.getItems().clear();
                        sharingByUserId.clear();
                        sharingIdByDisplay.clear();
                        for (JsonElement element : array) {
                            JsonObject item = element.getAsJsonObject();
                            Long userId = item.get("userId").getAsLong();
                            sharingByUserId.put(userId, item);
                            String username = JsonCodec.string(item, "username", "Utente");
                            String email = JsonCodec.string(item, "email", "");
                            String permission = JsonCodec.string(item, "permission", "VIEW");
                            String display = "@" + username + (email.isBlank() ? "" : "  ·  " + email)
                                    + " — " + permission;
                            sharingIdByDisplay.put(display, userId);
                            sharingListView.getItems().add(display);
                        }
                        setStatus(array.size() == 0 ? "Nessun utente ha accesso a questa lista."
                                : "Utenti con accesso: " + array.size(), false);
                    } catch (Exception ex) {
                        setStatus("Impossibile interpretare la risposta del server.", true);
                    }
                }),
                () -> Platform.runLater(() -> setStatus("Impossibile caricare le condivisioni.", true)), null);
    }

    private record UserSearchRow(String username, String email) {
        private String displayText() {
            return "@" + username + (email == null || email.isBlank() ? "" : "  ·  " + email);
        }
    }

    private void setStatus(String message, boolean error) {
        sharingStatusLabel.setText(message);
        sharingStatusLabel.getStyleClass().removeAll("sharing-status-error", "sharing-status-success");
        sharingStatusLabel.getStyleClass().add(error ? "sharing-status-error" : "sharing-status-success");
    }

    private Long extractId(String value) {
        if (value == null)
            return null;
        Matcher matcher = ID_PATTERN.matcher(value);
        if (!matcher.find())
            return null;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
