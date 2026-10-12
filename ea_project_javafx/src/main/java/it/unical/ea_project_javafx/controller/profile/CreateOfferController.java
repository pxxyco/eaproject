package it.unical.ea_project_javafx.controller.profile;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import it.unical.ea_project_javafx.util.WallpaperService;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.TripDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateOfferController {

    @FXML private StackPane rootPane;
    @FXML private ImageView bgImageView;
    @FXML private VBox modalCard;
    @FXML private Label headerTitleLabel;
    @FXML private VBox stepChoose;
    @FXML private VBox stepTrip;
    @FXML private VBox stepActivity;
    @FXML private VBox stagesContainer;
    @FXML private Label offerErrorLabel;

    @FXML private TextField tripTitleField;
    @FXML private TextField tripCountryField;
    @FXML private TextField tripCityField;
    @FXML private DatePicker tripStartDatePicker;
    @FXML private DatePicker tripEndDatePicker;
    @FXML private TextField tripPriceField;
    @FXML private TextField tripMaxSeatsField;
    @FXML private TextField tripCoverUrlField;
    @FXML private TextArea tripDescriptionArea;

    @FXML private Button saveTripButton;
    @FXML private Button saveActivityButton;

    @FXML private TextField actTitleField;
    @FXML private ComboBox<String> actCategoryCombo;
    @FXML private TextField actPlaceField;
    @FXML private TextField actCityField;
    @FXML private TextField actDurationField;
    @FXML private TextField actPriceField;
    @FXML private TextField actMaxSeatsField;
    @FXML private DatePicker actStartDatePicker;
    @FXML private DatePicker actEndDatePicker;
    @FXML private TextArea actDescriptionArea;
    @FXML private TextArea actNotesArea;

    private TripDTO tripToEdit;
    private ActivityDTO activityToEdit;

    @FXML
    public void initialize() {
        Rectangle rootClip = new Rectangle();
        rootClip.setArcWidth(36);
        rootClip.setArcHeight(36);
        rootClip.widthProperty().bind(rootPane.widthProperty());
        rootClip.heightProperty().bind(rootPane.heightProperty());
        rootPane.setClip(rootClip);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(modalCard.widthProperty());
        clip.heightProperty().bind(modalCard.heightProperty());
        modalCard.setClip(clip);

        bgImageView.setEffect(new GaussianBlur(18));
        bgImageView.fitWidthProperty().bind(rootPane.widthProperty());
        bgImageView.fitHeightProperty().bind(rootPane.heightProperty());
        loadBackground();

        if (actCategoryCombo != null) {
            actCategoryCombo.getItems().setAll("Escursione", "Visita guidata", "Trasporto", "Hotel", "Pasto", "Altro");
        }
    }

    private void loadBackground() {
        new Thread(() -> {
            String imageUrl = WallpaperService.getDailyWallpaperUrl();
            Platform.runLater(() -> bgImageView.setImage(new Image(
                    imageUrl, 1920, 1080, true, true, true)));
        }).start();
    }

    @FXML
    private void handleClose(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }

    @FXML
    private void handleSelectTrip() {
        stepChoose.setVisible(false);
        stepChoose.setManaged(false);
        stepTrip.setVisible(true);
        stepTrip.setManaged(true);
        headerTitleLabel.setText("Nuovo Viaggio (Itinerario)");
    }

    @FXML
    private void handleSelectActivity() {
        stepChoose.setVisible(false);
        stepChoose.setManaged(false);
        stepActivity.setVisible(true);
        stepActivity.setManaged(true);
        headerTitleLabel.setText("Nuova Attività Singola");
    }

    @FXML
    private void handleBackToChoose() {
        stepTrip.setVisible(false);
        stepTrip.setManaged(false);
        stepActivity.setVisible(false);
        stepActivity.setManaged(false);
        stepChoose.setVisible(true);
        stepChoose.setManaged(true);
        headerTitleLabel.setText("Crea Nuova Offerta");
    }

    @FXML
    private void handleAddStageRow() {
        VBox row = new VBox(8);
        row.getStyleClass().add("stage-section-container");
        HBox fields = new HBox(8);
        TextField title = new TextField();
        title.setPromptText("Titolo tappa *");
        title.getStyleClass().add("custom-input");
        TextField place = new TextField();
        place.setPromptText("Luogo");
        place.getStyleClass().add("custom-input");
        TextField city = new TextField();
        city.setPromptText("Città");
        city.getStyleClass().add("custom-input");
        TextField day = new TextField(String.valueOf(stagesContainer.getChildren().size() + 1));
        day.setPromptText("Giorno *");
        day.getStyleClass().add("custom-input");
        ComboBox<String> category = new ComboBox<>();
        category.getItems().setAll("EXCURSION", "VISIT", "HOTEL", "MEAL", "TRANSPORT", "OTHER");
        category.setValue("OTHER");
        category.setPromptText("Categoria *");
        category.getStyleClass().add("custom-combo");
        Button remove = new Button("Rimuovi");
        remove.getStyleClass().add("btn-remove-stage");
        remove.setMinWidth(88);
        remove.setOnAction(event -> stagesContainer.getChildren().remove(row));
        HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(place, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(city, javafx.scene.layout.Priority.ALWAYS);
        fields.getChildren().addAll(title, place, city, day, category, remove);
        TextArea description = new TextArea();
        description.setPromptText("Descrizione tappa");
        description.setPrefRowCount(2);
        description.getStyleClass().add("custom-textarea");
        row.getChildren().addAll(fields, description);
        row.setUserData(new StageFields(title, place, city, day, category, description));
        stagesContainer.getChildren().add(row);
    }

    public void setTripToEdit(TripDTO trip) {
        tripToEdit = trip;
        stepChoose.setVisible(false);
        stepChoose.setManaged(false);
        stepTrip.setVisible(true);
        stepTrip.setManaged(true);
        headerTitleLabel.setText("Modifica Viaggio");
        tripTitleField.setText(valueOrEmpty(trip.getTitle()));
        tripCountryField.setText(valueOrEmpty(trip.getDestinationCountry()));
        tripCityField.setText(valueOrEmpty(trip.getDestinationCity()));
        tripStartDatePicker.setValue(trip.getStartDate());
        tripEndDatePicker.setValue(trip.getEndDate());
        tripPriceField.setText(trip.getTotalPrice() == null ? "" : trip.getTotalPrice().toPlainString());
        tripMaxSeatsField.setText(trip.getMaxSeats() == null ? "" : String.valueOf(trip.getMaxSeats()));
        tripCoverUrlField.setText(valueOrEmpty(trip.getCoverPhotoUrl()));
        tripDescriptionArea.setText(valueOrEmpty(trip.getDescription()));
        if (saveTripButton != null) saveTripButton.setText("Salva modifiche");
    }

    public void setActivityToEdit(ActivityDTO activity) {
        activityToEdit = activity;
        stepChoose.setVisible(false);
        stepChoose.setManaged(false);
        stepActivity.setVisible(true);
        stepActivity.setManaged(true);
        headerTitleLabel.setText("Modifica attività");
        actTitleField.setText(valueOrEmpty(activity.getTitle()));
        actCategoryCombo.setValue(activityCategoryLabel(activity.getCategory()));
        actPlaceField.setText(valueOrEmpty(activity.getPlaceName()));
        actCityField.setText(valueOrEmpty(activity.getCity()));
        actDurationField.setText(activity.getDurationMinutes() == null ? "" : String.valueOf(activity.getDurationMinutes()));
        actPriceField.setText(activity.getPrice() == null ? "" : String.valueOf(activity.getPrice()));
        actMaxSeatsField.setText(activity.getMaxSeats() == null ? "" : String.valueOf(activity.getMaxSeats()));
        actStartDatePicker.setValue(activity.getStartDate() == null ? null : activity.getStartDate().toLocalDate());
        actEndDatePicker.setValue(activity.getEndDate() == null ? null : activity.getEndDate().toLocalDate());
        actDescriptionArea.setText(valueOrEmpty(activity.getDescription()));
        actNotesArea.setText(valueOrEmpty(activity.getNotes()));
        if (saveActivityButton != null) saveActivityButton.setText("Salva modifiche");
    }

    @FXML
    private void handleSaveTrip() {
        clearOfferError();
        try {
            LocalDate start = tripStartDatePicker.getValue();
            LocalDate end = tripEndDatePicker.getValue();
            BigDecimal price = new BigDecimal(tripPriceField.getText().trim());
            int maxSeats = Integer.parseInt(tripMaxSeatsField.getText().trim());
            if (blank(tripTitleField.getText()) || blank(tripCountryField.getText())
                    || blank(tripCityField.getText()) || start == null || end == null
                    || start.isAfter(end) || price.signum() <= 0 || maxSeats < 1) {
                showValidationError("Compila i campi obbligatori con valori validi e controlla l'intervallo delle date.");
                return;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", tripTitleField.getText().trim());
            payload.put("destinationCountry", tripCountryField.getText().trim());
            payload.put("destinationCity", tripCityField.getText().trim());
            payload.put("startDate", start.toString());
            payload.put("endDate", end.toString());
            payload.put("totalPrice", price);
            payload.put("maxSeats", maxSeats);
            payload.put("availableSeats", tripToEdit != null && tripToEdit.getAvailableSeats() != null
                    ? tripToEdit.getAvailableSeats()
                    : maxSeats);
            payload.put("status", tripToEdit != null && tripToEdit.getStatus() != null
                    ? tripToEdit.getStatus()
                    : "PUBLISHED");
            payload.put("coverPhotoUrl", emptyToNull(tripCoverUrlField.getText()));
            payload.put("description", emptyToNull(tripDescriptionArea.getText()));
            payload.put("stages", collectStages());

            if (saveTripButton != null) saveTripButton.setDisable(true);
            ApiService.call(
                    ApiService.BASE_URL + (tripToEdit == null ? "/api/trips" : "/api/trips/" + tripToEdit.getTripId()),
                    ApiService.toJson(payload),
                    tripToEdit == null ? ApiService.Method.POST : ApiService.Method.PUT,
                    response -> closeAfterSave(),
                    () -> {
                        if (saveTripButton != null) saveTripButton.setDisable(false);
                        showSaveError("Impossibile pubblicare il viaggio. Verifica i dati e riprova.");
                    },
                    null
            );
        } catch (IllegalArgumentException exception) {
            showValidationError(exception.getMessage() == null
                    ? "Inserisci un prezzo e un numero di posti validi."
                    : exception.getMessage());
        }
    }

    @FXML
    private void handleSaveActivity() {
        clearOfferError();
        try {
            LocalDate start = actStartDatePicker.getValue();
            LocalDate end = actEndDatePicker.getValue();
            double price = Double.parseDouble(actPriceField.getText().trim().replace(',', '.'));
            int duration = Integer.parseInt(actDurationField.getText().trim());
            int maxSeats = Integer.parseInt(actMaxSeatsField.getText().trim());
            if (blank(actTitleField.getText()) || actCategoryCombo.getValue() == null
                    || blank(actPlaceField.getText()) || blank(actCityField.getText())
                    || start == null || end == null || start.isAfter(end)
                    || !Double.isFinite(price) || price <= 0 || duration < 1 || maxSeats < 1) {
                showValidationError("Compila i campi obbligatori con valori validi e controlla l'intervallo delle date.");
                return;
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", actTitleField.getText().trim());
            payload.put("category", activityCategoryValue(actCategoryCombo.getValue()));
            payload.put("placeName", actPlaceField.getText().trim());
            payload.put("city", actCityField.getText().trim());
            payload.put("startDate", start.atStartOfDay().toString());
            payload.put("endDate", end.atStartOfDay().toString());
            payload.put("durationMinutes", duration);
            payload.put("price", price);
            payload.put("maxSeats", maxSeats);
            payload.put("description", emptyToNull(actDescriptionArea.getText()));
            payload.put("notes", emptyToNull(actNotesArea.getText()));

            if (saveActivityButton != null) saveActivityButton.setDisable(true);
            payload.put("availableSeats", activityToEdit != null && activityToEdit.getAvailableSeats() != null
                    ? activityToEdit.getAvailableSeats()
                    : maxSeats);
            payload.put("status", activityToEdit != null && activityToEdit.getStatus() != null
                    ? activityToEdit.getStatus()
                    : "PUBLISHED");
            ApiService.call(
                    ApiService.BASE_URL + (activityToEdit == null
                            ? "/api/activities"
                            : "/api/activities/" + activityToEdit.getActivityId()),
                    ApiService.toJson(payload),
                    activityToEdit == null ? ApiService.Method.POST : ApiService.Method.PUT,
                    response -> closeAfterSave(),
                    () -> {
                        if (saveActivityButton != null) saveActivityButton.setDisable(false);
                        showSaveError("Impossibile pubblicare l'attività. Verifica i dati e riprova.");
                    },
                    null
            );
        } catch (NumberFormatException exception) {
            showValidationError("Inserisci prezzo, durata e posti con valori validi.");
        }
    }

    private List<Map<String, Object>> collectStages() {
        List<Map<String, Object>> stages = new ArrayList<>();
        int order = 1;
        for (javafx.scene.Node node : stagesContainer.getChildren()) {
            StageFields fields = (StageFields) node.getUserData();
            if (blank(fields.title().getText())) {
                throw new IllegalArgumentException("Ogni tappa deve avere un titolo.");
            }
            int day = Integer.parseInt(fields.day().getText().trim());
            if (day < 1) {
                throw new IllegalArgumentException("Il giorno della tappa deve essere positivo.");
            }
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("title", fields.title().getText().trim());
            stage.put("locationName", emptyToNull(fields.place().getText()));
            stage.put("city", emptyToNull(fields.city().getText()));
            stage.put("category", fields.category().getValue());
            stage.put("day", day);
            stage.put("orderInDay", order++);
            stage.put("description", emptyToNull(fields.description().getText()));
            stages.add(stage);
        }
        return stages;
    }

    private String activityCategoryValue(String label) {
        return switch (label) {
            case "Escursione" -> "EXCURSION";
            case "Visita guidata" -> "VISIT";
            case "Trasporto" -> "TRANSPORT";
            case "Hotel" -> "HOTEL";
            case "Pasto" -> "MEAL";
            default -> "OTHER";
        };
    }

    private String activityCategoryLabel(String category) {
        if (category == null) return null;
        return switch (category) {
            case "EXCURSION" -> "Escursione";
            case "VISIT" -> "Visita guidata";
            case "TRANSPORT" -> "Trasporto";
            case "HOTEL" -> "Hotel";
            case "MEAL" -> "Pasto";
            default -> "Altro";
        };
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String emptyToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void showValidationError(String message) {
        showOfferError(message);
    }

    private void showSaveError(String message) {
        showOfferError(message);
    }

    private void showOfferError(String message) {
        if (offerErrorLabel != null) {
            offerErrorLabel.setText(message);
            offerErrorLabel.setVisible(true);
            offerErrorLabel.setManaged(true);
        }
    }

    private void clearOfferError() {
        if (offerErrorLabel != null) {
            offerErrorLabel.setText("");
            offerErrorLabel.setVisible(false);
            offerErrorLabel.setManaged(false);
        }
    }

    private void closeAfterSave() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    private record StageFields(
            TextField title,
            TextField place,
            TextField city,
            TextField day,
            ComboBox<String> category,
            TextArea description
    ) {
    }
}