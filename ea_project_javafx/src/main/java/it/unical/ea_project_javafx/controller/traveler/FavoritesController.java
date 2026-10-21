package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavoritesController {

    @FXML private ImageView bgImageView;
    
    @FXML private StackPane navbarContainer;
    
    @FXML private ComboBox<String> newListVisibilityCombo;
    
    @FXML private FlowPane favoritesCardsContainer;
    
    @FXML private Label statusMessageLabel;
    @FXML private Label selectedListNameLabel;
    @FXML private Label selectedListVisibilityLabel;
    
    @FXML private TextField newListNameField;
    @FXML private TextField editListNameField;
    @FXML private ComboBox<String> editListVisibilityCombo;
    @FXML private Button saveListChangesButton;
    @FXML private Button deleteListButton;
    @FXML private TextField tripSearchField;
    @FXML private TextField activitySearchField;
    
    @FXML private ListView<String> listsListView;
    @FXML private ListView<String> tripSearchResults;
    @FXML private ListView<String> savedTripsListView;
    @FXML private ListView<String> activitySearchResults;
    @FXML private ListView<String> savedActivitiesListView;
    @FXML private TableView<SharedListRow> sharedListsTable;
    @FXML private TableColumn<SharedListRow, String> sharedListNameColumn;
    @FXML private TableColumn<SharedListRow, String> sharedListOrganizerColumn;
    @FXML private TableColumn<SharedListRow, String> sharedListPermissionColumn;
    @FXML private TableColumn<SharedListRow, String> sharedListSharedAtColumn;
    @FXML private ListView<String> sharedListDetailsListView;
    @FXML private Label sharedListsStatusLabel;

    @FXML private Button shareListButton;
    @FXML private javafx.scene.layout.StackPane sharingPopup;
    @FXML private StackPane deleteConfirmationPopup;
    @FXML private Label deletePopupTitleLabel;
    @FXML private Button confirmDeleteButton;
    @FXML private ListSharingPopupController sharingPopupController;
    @FXML private Button addTripButton;
    @FXML private Button removeTripButton;
    @FXML private Button addActivityButton;
    @FXML private Button removeActivityButton;

    private static final Pattern ID_PATTERN = Pattern.compile("#(\\d+)\\s*$");
    private Long selectedListId;

    @FXML
    public void initialize() {
    
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        newListVisibilityCombo.getItems().setAll("PRIVATE", "SHARED", "PUBLIC");
        editListVisibilityCombo.getItems().setAll("PRIVATE", "SHARED", "PUBLIC");
        newListVisibilityCombo.getSelectionModel().selectFirst();
        listsListView.setItems(FXCollections.observableArrayList());
        tripSearchResults.setItems(FXCollections.observableArrayList());
        savedTripsListView.setItems(FXCollections.observableArrayList());
        activitySearchResults.setItems(FXCollections.observableArrayList());
        savedActivitiesListView.setItems(FXCollections.observableArrayList());
        sharedListsTable.setItems(FXCollections.observableArrayList());
        sharedListDetailsListView.setItems(FXCollections.observableArrayList());
        sharedListNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().listName));
        sharedListOrganizerColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().organizer));
        sharedListPermissionColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().permission));
        sharedListSharedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().sharedAt));
        sharedListsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) ->
                showSharedListDetails(newRow));
        listsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> {

            selectedListId = extractId(value);
            if (value == null) {
                selectedListNameLabel.setText("Seleziona una lista");
                selectedListVisibilityLabel.setText("-");
                editListNameField.clear();
                editListVisibilityCombo.getSelectionModel().clearSelection();
                saveListChangesButton.setDisable(true);
                if (deleteListButton != null) deleteListButton.setDisable(true);
                shareListButton.setVisible(false);
                savedTripsListView.getItems().clear();
                savedActivitiesListView.getItems().clear();
                setContentControlsEnabled(false);
                return;
            }

            selectedListNameLabel.setText(displayName(value));
            String visibility = extractVisibility(value);
            selectedListVisibilityLabel.setText("Visibilità: " + visibility);
            editListNameField.setText(displayName(value));
            editListVisibilityCombo.setValue(visibility);
            saveListChangesButton.setDisable(false);
            if (deleteListButton != null) deleteListButton.setDisable(false);
            shareListButton.setVisible("SHARED".equals(visibility));
            setContentControlsEnabled(true);
            loadSavedContents();

        });
        setContentControlsEnabled(false);
        saveListChangesButton.setDisable(true);
        if (deleteListButton != null) deleteListButton.setDisable(true);
        sharingPopup.setVisible(false);
        sharingPopup.setManaged(false);
        deleteConfirmationPopup.setVisible(false);
        deleteConfirmationPopup.setManaged(false);
        sharingPopupController.setCloseAction(this::closeSharingPopup);
        loadLists();
        loadSharedLists();

    }

    private void setContentControlsEnabled(boolean enabled) {

        addTripButton.setDisable(!enabled);
        removeTripButton.setDisable(!enabled);
        addActivityButton.setDisable(!enabled);
        removeActivityButton.setDisable(!enabled);
    
    }

    private void loadNavbar() {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loadLists() {

        ApiService.call(ApiService.BASE_URL + "/api/favorite-lists/mine", null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    try {

                        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
                        listsListView.getItems().clear();
                        for (JsonElement el : arr) {
                            JsonObject obj = el.getAsJsonObject();
                            String id = obj.get("id").getAsString();
                            String name = stringValue(obj, "name", "Lista #" + id);
                            String visibility = stringValue(obj, "visibility", "PRIVATE");
                            listsListView.getItems().add(name + " [" + visibility + "] #" + id);
                        }

                        if (!listsListView.getItems().isEmpty()) {
                            listsListView.getSelectionModel().selectFirst();
                        } else {
                            selectedListId = null;
                            selectedListNameLabel.setText("Nessuna lista");
                            selectedListVisibilityLabel.setText("");
                            shareListButton.setVisible(false);
                            savedTripsListView.getItems().clear();
                            savedActivitiesListView.getItems().clear();
                            setContentControlsEnabled(false);
                        }

                    } catch (Exception ex) {
                        showError("Risposta non valida dal server durante il caricamento delle liste.");
                    }
                }),
                () -> Platform.runLater(() -> showError("Impossibile caricare le liste preferite.")), null);

    }

    @FXML
    private void handleRefreshSharedLists() {
        loadSharedLists();
    }

    private void loadSharedLists() {
        if (sharedListsStatusLabel != null) sharedListsStatusLabel.setText("Caricamento liste condivise…");
        ApiService.call(ApiService.BASE_URL + "/api/list-sharings/shared-with-me", null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    try {
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        sharedListsTable.getItems().clear();
                        sharedListDetailsListView.getItems().clear();
                        for (JsonElement element : array) {
                            JsonObject obj = element.getAsJsonObject();
                            String listName = stringValue(obj, "listName", "Lista condivisa");
                            String organizer = stringValue(obj, "organizerUsername", "Non disponibile");
                            String permission = stringValue(obj, "permission", "VIEW");
                            String sharedAt = stringValue(obj, "sharedAt", "");
                            if (sharedAt.length() >= 16) sharedAt = sharedAt.replace('T', ' ');
                            sharedListsTable.getItems().add(new SharedListRow(listName, organizer, permission, sharedAt, obj));
                        }
                        if (sharedListsStatusLabel != null) {
                            sharedListsStatusLabel.setText(array.size() == 0
                                    ? "Non hai ancora liste condivise con te. Gli inviti accettati appariranno qui."
                                    : "Liste condivise con te: " + array.size());
                        }
                        if (!sharedListsTable.getItems().isEmpty()) sharedListsTable.getSelectionModel().selectFirst();
                    } catch (Exception ex) {
                        if (sharedListsStatusLabel != null) sharedListsStatusLabel.setText("Risposta non valida dal server durante il caricamento delle liste condivise.");
                    }
                }),
                () -> Platform.runLater(() -> {
                    if (sharedListsStatusLabel != null) sharedListsStatusLabel.setText("Impossibile caricare le liste condivise. Controlla il backend.");
                }), null);
    }

    private void showSharedListDetails(SharedListRow row) {
        sharedListDetailsListView.getItems().clear();
        if (row == null || row.data == null) return;
        JsonArray trips = row.data.has("trips") && row.data.get("trips").isJsonArray()
                ? row.data.getAsJsonArray("trips") : new JsonArray();
        if (trips.size() == 0) {
            sharedListDetailsListView.getItems().add("Questa lista non contiene ancora viaggi.");
            return;
        }
        for (JsonElement tripElement : trips) {
            if (!tripElement.isJsonObject()) continue;
            JsonObject trip = tripElement.getAsJsonObject();
            String title = stringValue(trip, "title", "Viaggio senza titolo");
            String city = stringValue(trip, "destinationCity", "");
            String country = stringValue(trip, "destinationCountry", "");
            String destination = (city + (city.isBlank() || country.isBlank() ? "" : ", ") + country).trim();
            sharedListDetailsListView.getItems().add("✈ " + title + (destination.isBlank() ? "" : " — " + destination));
            sharedListDetailsListView.getItems().add("  Organizzatore del viaggio: @" + stringValue(trip, "organizerUsername", "non disponibile"));
            String start = stringValue(trip, "startDate", "");
            String end = stringValue(trip, "endDate", "");
            if (!start.isBlank() || !end.isBlank()) sharedListDetailsListView.getItems().add("  Date: " + (start.isBlank() ? "?" : start) + " → " + (end.isBlank() ? "?" : end));
            JsonArray stages = trip.has("stages") && trip.get("stages").isJsonArray()
                    ? trip.getAsJsonArray("stages") : new JsonArray();
            if (stages.size() == 0) {
                sharedListDetailsListView.getItems().add("  Nessuna tappa inserita.");
            } else {
                for (JsonElement stageElement : stages) {
                    if (!stageElement.isJsonObject()) continue;
                    JsonObject stage = stageElement.getAsJsonObject();
                    String day = stringValue(stage, "day", "?");
                    String order = stringValue(stage, "orderInDay", "?");
                    String stageTitle = stringValue(stage, "title", "Tappa");
                    String location = firstNonBlank(stringValue(stage, "locationName", ""), stringValue(stage, "city", ""));
                    String countryStage = stringValue(stage, "country", "");
                    if (!location.isBlank() && !countryStage.isBlank() && !location.equals(countryStage)) location += ", " + countryStage;
                    String startTime = stringValue(stage, "startTime", "");
                    String endTime = stringValue(stage, "endTime", "");
                    String time = startTime.isBlank() && endTime.isBlank() ? "" : " · " + (startTime.isBlank() ? "--:--" : startTime) + "–" + (endTime.isBlank() ? "--:--" : endTime);
                    String category = stringValue(stage, "category", "");
                    sharedListDetailsListView.getItems().add("    Giorno " + day + " · tappa " + order + ": " + stageTitle
                            + (category.isBlank() ? "" : " [" + category + "]")
                            + (location.isBlank() ? "" : " — " + location) + time);
                    String description = stringValue(stage, "description", "");
                    if (!description.isBlank()) sharedListDetailsListView.getItems().add("      " + description);
                }
            }
            sharedListDetailsListView.getItems().add(" ");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static final class SharedListRow {
        private final String listName;
        private final String organizer;
        private final String permission;
        private final String sharedAt;
        private final JsonObject data;
        private SharedListRow(String listName, String organizer, String permission, String sharedAt, JsonObject data) {
            this.listName = listName;
            this.organizer = organizer == null ? "" : "@" + organizer;
            this.permission = permission;
            this.sharedAt = sharedAt;
            this.data = data;
        }
    }

    @FXML
    private void handleSaveListChanges() {

        if (selectedListId == null) {
            showError("Seleziona prima una lista.");
            return;
        }

        String name = editListNameField.getText();
        String visibility = editListVisibilityCombo.getValue();
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            showError("Il nome deve contenere da 1 a 80 caratteri.");
            return;
        }

        if (visibility == null) {
            showError("Seleziona la visibilità della lista.");
            return;
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("name", name.trim());
        payload.put("visibility", visibility);
        Long idToSelect = selectedListId;
        saveListChangesButton.setDisable(true);
        ApiService.call(ApiService.BASE_URL + "/api/favorite-lists/" + idToSelect,
                ApiService.toJson(payload), ApiService.Method.PUT,
                response -> Platform.runLater(() -> {
                    showSuccess("Lista aggiornata correttamente.");
                    loadListsAndSelect(idToSelect);
                }),
                () -> Platform.runLater(() -> {
                    saveListChangesButton.setDisable(false);
                    showError("Modifica non riuscita. Verifica i dati e i permessi sulla lista.");
                }), null);
    }

    private void loadListsAndSelect(Long idToSelect) {

        ApiService.call(ApiService.BASE_URL + "/api/favorite-lists/mine", null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {

                    try {

                        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
                        listsListView.getItems().clear();
                        int targetIndex = -1;
                        for (JsonElement el : arr) {
                            JsonObject obj = el.getAsJsonObject();
                            String id = obj.get("id").getAsString();
                            String name = stringValue(obj, "name", "Lista #" + id);
                            String visibility = stringValue(obj, "visibility", "PRIVATE");
                            listsListView.getItems().add(name + " [" + visibility + "] #" + id);
                            if (Long.parseLong(id) == idToSelect) {
                                targetIndex = listsListView.getItems().size() - 1;
                            }

                        }

                        if (targetIndex >= 0) {
                            listsListView.getSelectionModel().select(targetIndex);
                        } else if (!listsListView.getItems().isEmpty()) {
                            listsListView.getSelectionModel().selectFirst();
                        }

                    } catch (Exception ex) {
                        showError("Lista aggiornata, ma non è stato possibile ricaricare l'elenco.");
                    }

                }),
                () -> Platform.runLater(() -> {
                    saveListChangesButton.setDisable(false);
                    showError("Lista aggiornata, ma non è stato possibile ricaricare l'elenco.");
                }),
                null);

    }

    @FXML
    private void handleDeleteList() {
        if (selectedListId == null) {
            showError("Seleziona prima una lista da eliminare.");
            return;
        }
        deletePopupTitleLabel.setText("Vuoi eliminare la lista \"" + selectedListNameLabel.getText() + "\"?");
        deleteConfirmationPopup.setManaged(true);
        deleteConfirmationPopup.setVisible(true);
        deleteConfirmationPopup.toFront();
    }

    @FXML
    private void cancelDeleteList() {
        deleteConfirmationPopup.setVisible(false);
        deleteConfirmationPopup.setManaged(false);
    }

    @FXML
    private void confirmDeleteList() {
        if (selectedListId == null) {
            cancelDeleteList();
            showError("Seleziona prima una lista da eliminare.");
            return;
        }
        final Long idToDelete = selectedListId;
        final String listName = selectedListNameLabel.getText();
        confirmDeleteButton.setDisable(true);
        deleteListButton.setDisable(true);
        saveListChangesButton.setDisable(true);
        ApiService.delete(ApiService.BASE_URL + "/api/favorite-lists/" + idToDelete,
                response -> Platform.runLater(() -> {
                    cancelDeleteList();
                    confirmDeleteButton.setDisable(false);
                    selectedListId = null;
                    showSuccess("La lista \"" + listName + "\" è stata eliminata.");
                    loadLists();
                    loadSharedLists();
                }),
                () -> Platform.runLater(() -> {
                    confirmDeleteButton.setDisable(false);
                    deleteListButton.setDisable(selectedListId == null);
                    saveListChangesButton.setDisable(selectedListId == null);
                    showError("Impossibile eliminare la lista. Verifica di esserne il proprietario e riprova.");
                }), null);
    }

    @FXML private void handleCreateList() {

        String name = newListNameField.getText();
        String visibility = newListVisibilityCombo.getValue();
        if (name == null || name.isBlank()) { 
            showError("Inserisci un nome per la lista.");
            return; 
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("name", name.trim());
        payload.put("visibility", visibility);
        ApiService.call(ApiService.BASE_URL + "/api/favorite-lists", ApiService.toJson(payload), ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    showSuccess("Lista '" + name.trim() + "' creata.");
                    newListNameField.clear();
                    loadLists();
                }),
                () -> Platform.runLater(() -> showError("Impossibile creare la lista.")),
                null
            );

    }

    @FXML private void handleSearchTrips() {

        String query = tripSearchField.getText() == null ? "" : tripSearchField.getText().trim();
        String url = ApiService.BASE_URL + "/api/trips/search?query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&page=0&size=30";
        ApiService.call(url, null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    try {
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        tripSearchResults.getItems().clear();
                        for (JsonElement el : array) {
                            JsonObject o = el.getAsJsonObject();
                            String id = stringValue(o, "tripId", null);
                            if (id != null) {
                                tripSearchResults.getItems()
                                    .add(stringValue(o, "title", "Viaggio") + " — "
                                       + stringValue(o, "location", "") + " #" + id);
                            }

                        }

                        if (tripSearchResults.getItems().isEmpty()) {
                            showError("Nessun viaggio trovato.");
                        }

                    } catch (Exception ex) { showError("Impossibile leggere i risultati dei viaggi."); }
                }),
                () -> Platform.runLater(() -> showError("Ricerca viaggi non riuscita.")),
                null);

    }

    @FXML private void handleAddTrip() {

        Long tripId = extractId(tripSearchResults.getSelectionModel().getSelectedItem());
        if (selectedListId == null) { 
            showError("Seleziona prima una lista.");
            return; 
        }

        if (tripId == null) { 
            showError("Seleziona un viaggio dai risultati.");
            return; 
        }

        Map<String, Long> payload = Map.of("listId", selectedListId, "tripId", tripId);
        ApiService.call(ApiService.BASE_URL + "/api/list-trips", ApiService.toJson(payload), ApiService.Method.POST,
                response -> Platform.runLater(() -> { 
                    showSuccess("Viaggio aggiunto alla lista."); loadSavedContents(); 
                }),
                () -> Platform.runLater(() -> showError("Impossibile aggiungere il viaggio. Potrebbe essere già presente o non essere pubblicato.")), null
            );
    
    }

    @FXML private void handleRemoveTrip() {

        Long tripId = extractId(savedTripsListView.getSelectionModel().getSelectedItem());
        if (selectedListId == null || tripId == null) { 
            showError("Seleziona un viaggio salvato da rimuovere.");
            return;
        }

        ApiService.call(ApiService.BASE_URL + "/api/list-trips/list/" + selectedListId + "/trip/" + tripId,
                null, ApiService.Method.DELETE,
                response -> Platform.runLater(() -> { 
                    showSuccess("Viaggio rimosso dalla lista."); loadSavedContents(); 
                }),
                () -> Platform.runLater(() -> showError("Non è stato possibile rimuovere il viaggio: verifica i permessi.")),
                null);

    }

    @FXML private void handleSearchActivities() {

        String query = activitySearchField.getText() == null ? "" : activitySearchField.getText().trim();
        String url = ApiService.BASE_URL + "/api/activities/search?query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&page=0&size=30";
        ApiService.call(url, null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    try {

                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        activitySearchResults.getItems().clear();
                        for (JsonElement el : array) {
                            JsonObject o = el.getAsJsonObject();
                            String id = stringValue(o, "activityId", null);
                            if (id != null) {
                                activitySearchResults.getItems()
                                    .add(stringValue(o, "title", "Attività") + " — "
                                       + stringValue(o, "city", "") + " #" + id);
                            }

                        }

                        if (activitySearchResults.getItems().isEmpty()) {
                            showError("Nessuna attività trovata.");
                        }

                    } catch (Exception ex) { showError("Impossibile leggere i risultati delle attività."); }
                }),
                () -> Platform.runLater(() -> showError("Ricerca attività non riuscita.")), null);

    }

    @FXML private void handleAddActivity() {

        Long activityId = extractId(activitySearchResults.getSelectionModel().getSelectedItem());
        if (selectedListId == null) { 
            showError("Seleziona prima una lista.");
            return;
        }

        if (activityId == null) { 
            showError("Seleziona un'attività dai risultati.");
            return;
        }

        Map<String, Long> payload = Map.of("listId", selectedListId, "activityId", activityId);
        ApiService.call(ApiService.BASE_URL + "/api/list-activities", ApiService.toJson(payload), ApiService.Method.POST,
                response -> Platform.runLater(() -> { 
                    showSuccess("Attività aggiunta alla lista.");
                    loadSavedContents();
                }),
                () -> Platform.runLater(() -> showError("Impossibile aggiungere l'attività. Potrebbe essere già presente o non pubblicata.")), null
            );

    }

    @FXML private void handleRemoveActivity() {

        Long activityId = extractId(savedActivitiesListView.getSelectionModel().getSelectedItem());
        if (selectedListId == null || activityId == null) { 
            showError("Seleziona un'attività salvata da rimuovere.");
            return;
        }

        ApiService.call(ApiService.BASE_URL + "/api/list-activities/list/" + selectedListId + "/activity/" + activityId,
                null, ApiService.Method.DELETE,
                response -> Platform.runLater(() -> { 
                    showSuccess("Attività rimossa dalla lista."); loadSavedContents(); 
                }),
                () -> Platform.runLater(() -> showError("Non è stato possibile rimuovere l'attività: verifica i permessi.")), null);

    }

    private void loadSavedContents() {

        if (selectedListId == null) {
            return;
        }

        Long listId = selectedListId;
        ApiService.call(ApiService.BASE_URL + "/api/list-trips/list/" + listId, null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    try {
                        savedTripsListView.getItems().clear();
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        for (JsonElement el : array) {
                            JsonObject o = el.getAsJsonObject();
                            savedTripsListView.getItems().add(stringValue(o, "title", "Viaggio")
                                    + " — " + stringValue(o, "location", "") + " #" + o.get("tripId").getAsString());
                        }
    
                    } catch (Exception ex) { showError("Impossibile leggere i viaggi della lista."); }
                }),
                () -> Platform.runLater(() -> showError("Impossibile caricare i viaggi della lista.")),
                null);
        ApiService.call(ApiService.BASE_URL + "/api/list-activities/list/" + listId, null, ApiService.Method.GET,
                response -> Platform.runLater(() -> {
                    try {
                        savedActivitiesListView.getItems().clear();
                        JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                        for (JsonElement el : array) {
                            JsonObject o = el.getAsJsonObject();
                            savedActivitiesListView.getItems().add(stringValue(o, "title", "Attività")
                                    + " — " + stringValue(o, "city", "") + " #" + o.get("activityId").getAsString());
                        }

                    } catch (Exception ex) { showError("Impossibile leggere le attività della lista."); }
                }),
                () -> Platform.runLater(() -> showError("Impossibile caricare le attività della lista.")),
                null);

    }

    @FXML
    private void handleShareList() {
        if (selectedListId == null) {
            showError("Seleziona prima una lista condivisa.");
            return;
        }
        sharingPopupController.open(selectedListId, selectedListNameLabel.getText());
        sharingPopup.setManaged(true);
        sharingPopup.setVisible(true);
        sharingPopup.toFront();
    }

    @FXML
    private void closeSharingPopup() {
        sharingPopup.setVisible(false);
        sharingPopup.setManaged(false);
    }

    private Long extractId(String value) {

        if (value == null) {
            return null;
        }

        Matcher matcher = ID_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }

        try { 
            return Long.parseLong(matcher.group(1)); 
        } catch (NumberFormatException ex) { 
            return null; 
        }

    }

    private String extractVisibility(String value) {

        if (value == null) {
            return "PRIVATE";
        }

        Matcher matcher = Pattern.compile("\\[(PRIVATE|SHARED|PUBLIC)\\]").matcher(value);
        return matcher.find() ? matcher.group(1) : "PRIVATE";

    }

    private String displayName(String value) {

        int marker = value.lastIndexOf(" [");
        return marker > 0 ? value.substring(0, marker) : value;

    }

    private String stringValue(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }

    private void showError(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-error");
        statusMessageLabel.setVisible(true); statusMessageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-success");
        statusMessageLabel.setVisible(true); statusMessageLabel.setManaged(true);
    }
}