package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.util.JsonCodec;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class CollaborativeItineraryController {
    private record ListChoice(long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private ComboBox<ListChoice> listPicker;
    @FXML
    private TableView<JsonObject> stops;
    @FXML
    private TableColumn<JsonObject, String> dayColumn, positionColumn, titleColumn, cityColumn;
    @FXML
    private TextField titleField, cityField, startField, endField;
    @FXML
    private TextArea notesField;
    @FXML
    private Spinner<Integer> daySpinner, positionSpinner;
    @FXML
    private Button saveButton, deleteButton, newButton, refreshButton;
    @FXML
    private Label statusLabel, permissionLabel;
    private boolean editable, busy;
    private Long selectedId;
    private long version, generation;
    private static final String API = ApiService.BASE_URL + "/api/itineraries/";

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        ViewSupport.loadNavbar(navbarContainer, message -> ViewSupport.show(statusLabel, message));
        daySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 1));
        positionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 1));
        dayColumn.setCellValueFactory(c -> new SimpleStringProperty(JsonCodec.string(c.getValue(), "day", "")));
        positionColumn
                .setCellValueFactory(c -> new SimpleStringProperty(JsonCodec.string(c.getValue(), "position", "")));
        titleColumn.setCellValueFactory(c -> new SimpleStringProperty(JsonCodec.string(c.getValue(), "title", "")));
        cityColumn.setCellValueFactory(c -> new SimpleStringProperty(JsonCodec.string(c.getValue(), "city", "")));
        listPicker.valueProperty().addListener((o, a, b) -> {
            generation++;
            load();
        });
        stops.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null)
                edit(b);
        });
        setBusy(false);
        loadLists();
    }

    private void loadLists() {
        ApiService.callDetailed(ApiService.BASE_URL + "/api/favorite-lists/mine", null, ApiService.Method.GET, r -> {
            var choices = new LinkedHashMap<Long, ListChoice>();
            for (var e : JsonParser.parseString(r.body()).getAsJsonArray()) {
                var j = e.getAsJsonObject();
                long id = j.get("id").getAsLong();
                choices.put(id, new ListChoice(id, JsonCodec.string(j, "name", "")));
            }
            ApiService.callDetailed(ApiService.BASE_URL + "/api/list-sharings/mine", null, ApiService.Method.GET,
                    shared -> {
                        for (var e : JsonParser.parseString(shared.body()).getAsJsonArray()) {
                            var j = e.getAsJsonObject();
                            long id = j.get("listId").getAsLong();
                            choices.putIfAbsent(id,
                                    new ListChoice(id, JsonCodec.string(j, "listName", "") + " · condivisa"));
                        }
                        listPicker.getItems().setAll(choices.values());
                        if (!choices.isEmpty())
                            listPicker.getSelectionModel().selectFirst();
                        else
                            statusLabel.setText(
                                    "Crea prima una lista nella sezione Preferiti, poi organizza qui le sue tappe.");
                    }, statusLabel::setText, null);
        }, statusLabel::setText, null);
    }

    @FXML
    private void handleRefresh() {
        load();
    }

    private void load() {
        var choice = listPicker.getValue();
        if (choice == null)
            return;
        long request = generation;
        setBusy(true);
        ApiService.callDetailed(API + choice.id, null, ApiService.Method.GET, r -> {
            if (request != generation)
                return;
            var plan = JsonParser.parseString(r.body()).getAsJsonObject();
            editable = plan.get("editable").getAsBoolean();
            permissionLabel.setText(editable ? "Puoi aggiungere, modificare e rimuovere tappe."
                    : "Questo itinerario è condiviso con te in sola lettura.");
            List<JsonObject> values = new ArrayList<>();
            plan.getAsJsonArray("stops").forEach(e -> values.add(e.getAsJsonObject()));
            stops.getItems().setAll(values);
            handleNew();
            setBusy(false);
        }, message -> {
            if (request == generation) {
                setBusy(false);
                statusLabel.setText(message);
            }
        }, null);
    }

    private void edit(JsonObject j) {
        selectedId = j.get("id").getAsLong();
        version = j.get("version").getAsLong();
        titleField.setText(JsonCodec.string(j, "title", ""));
        cityField.setText(JsonCodec.string(j, "city", ""));
        notesField.setText(JsonCodec.string(j, "notes", ""));
        daySpinner.getValueFactory().setValue(j.get("day").getAsInt());
        positionSpinner.getValueFactory().setValue(j.get("position").getAsInt());
        startField.setText(JsonCodec.string(j, "startTime", ""));
        endField.setText(JsonCodec.string(j, "endTime", ""));
        setBusy(busy);
    }

    @FXML
    private void handleNew() {
        selectedId = null;
        version = 0;
        stops.getSelectionModel().clearSelection();
        titleField.clear();
        cityField.clear();
        notesField.clear();
        startField.clear();
        endField.clear();
        daySpinner.getValueFactory().setValue(1);
        positionSpinner.getValueFactory().setValue(stops.getItems().size() + 1);
        setBusy(busy);
    }

    @FXML
    private void handleSave() {
        if (busy || !editable || listPicker.getValue() == null)
            return;
        try {
            String title = titleField.getText().strip();
            if (title.isBlank() || title.length() > 180)
                throw new IllegalArgumentException("Inserisci un titolo di massimo 180 caratteri.");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("title", title);
            body.put("city", cityField.getText().strip());
            body.put("notes", notesField.getText());
            body.put("version", version);
            body.put("day", daySpinner.getValue());
            body.put("position", positionSpinner.getValue());
            body.put("startTime",
                    startField.getText().isBlank() ? null : LocalTime.parse(startField.getText()).toString());
            body.put("endTime", endField.getText().isBlank() ? null : LocalTime.parse(endField.getText()).toString());
            String path = API + listPicker.getValue().id + "/stops" + (selectedId == null ? "" : "/" + selectedId);
            setBusy(true);
            ApiService.callDetailed(path, ApiService.toJson(body),
                    selectedId == null ? ApiService.Method.POST : ApiService.Method.PUT, r -> {
                        statusLabel.setText("Tappa salvata.");
                        load();
                    }, message -> {
                        setBusy(false);
                        statusLabel.setText(
                                message + " Il modulo è conservato; usa Aggiorna per leggere l'ultima versione.");
                    }, null);
        } catch (Exception ex) {
            statusLabel.setText("Controlla titolo e orari (formato HH:mm).");
        }
    }

    @FXML
    private void handleDelete() {
        if (busy || !editable || selectedId == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare questa tappa dall'itinerario condiviso?",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.getDialogPane().getStylesheets()
                .add(getClass().getResource("/it/unical/ea_project_javafx/css/app.css").toExternalForm());
        confirm.initOwner(stops.getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
            return;
        setBusy(true);
        ApiService.callDetailed(API + listPicker.getValue().id + "/stops/" + selectedId + "?version=" + version, null,
                ApiService.Method.DELETE, r -> {
                    statusLabel.setText("Tappa rimossa.");
                    load();
                }, message -> {
                    setBusy(false);
                    statusLabel.setText(message);
                }, null);
    }

    private void setBusy(boolean value) {
        busy = value;
        listPicker.setDisable(value);
        refreshButton.setDisable(value);
        stops.setDisable(value);
        saveButton.setDisable(value || !editable);
        deleteButton.setDisable(value || !editable || selectedId == null);
        newButton.setDisable(value || !editable);
        for (var n : List.of(titleField, cityField, notesField, startField, endField, daySpinner, positionSpinner))
            n.setDisable(value || !editable);
    }
}
