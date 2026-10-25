package it.unical.ea_project_javafx.controller.organizer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.ActivityImageDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import it.unical.ea_project_javafx.dto.TripImageDTO;
import it.unical.ea_project_javafx.util.ImageUtils;

import java.math.BigDecimal;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    @FXML private Label tripCoverFileLabel;
    @FXML private Label tripSecondaryPhotosLabel;
    @FXML private ImageView tripCoverPreview;
    @FXML private FlowPane tripSecondaryPhotoPreview;
    @FXML private TextArea tripDescriptionArea;

    @FXML private Button saveTripButton;
    @FXML private Button saveActivityButton;
    @FXML private Button activityBackButton;

    @FXML private TextField actTitleField;
    @FXML private ComboBox<String> actCategoryCombo;
    @FXML private TextField actPlaceField;
    @FXML private TextField actCityField;
    @FXML private TextField actDurationField;
    @FXML private TextField actPriceField;
    @FXML private TextField actMaxSeatsField;
    @FXML private Label actCoverFileLabel;
    @FXML private Label actSecondaryPhotosLabel;
    @FXML private ImageView actCoverPreview;
    @FXML private FlowPane actSecondaryPhotoPreview;
    @FXML private DatePicker actStartDatePicker;
    @FXML private DatePicker actEndDatePicker;
    @FXML private TextArea actDescriptionArea;
    @FXML private TextArea actNotesArea;

    private TripDTO tripToEdit;
    private ActivityDTO activityToEdit;
    private Runnable closeAction;
    private File tripCoverFile;
    private List<File> tripSecondaryPhotoFiles = new ArrayList<>();
    private File activityCoverFile;
    private List<File> activitySecondaryPhotoFiles = new ArrayList<>();

    @FXML
    public void initialize() {

        BackgroundHelper.setupBackground(bgImageView);

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

        if (actCategoryCombo != null) {
            actCategoryCombo.getItems().setAll("Escursione", "Visita guidata", "Trasporto", "Hotel", "Pasto", "Altro");
        }
        tripCoverFileLabel.setText("Nessuna nuova copertina selezionata");
        tripSecondaryPhotosLabel.setText("Nessuna foto secondaria selezionata");
        tripCoverPreview.setVisible(false);
        tripCoverPreview.setManaged(false);
        actCoverFileLabel.setText("Nessuna nuova copertina selezionata");
        actSecondaryPhotosLabel.setText("Nessuna foto secondaria selezionata");
        actCoverPreview.setVisible(false);
        actCoverPreview.setManaged(false);

    }

    /** Consente al form di essere ospitato dalla PopupFactory senza creare un nuovo Stage. */
    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
        if (closeAction != null) {
            if (bgImageView != null) { bgImageView.setVisible(false); bgImageView.setManaged(false); }
            if (rootPane != null) {
                rootPane.getChildren().stream()
                        .filter(node -> node.getStyleClass().contains("glass-overlay"))
                        .forEach(node -> { node.setVisible(false); node.setManaged(false); });
                rootPane.getStyleClass().add("popup-factory-transparent-surface");
            }
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        closeView();
    }

    private void closeView() {
        if (closeAction != null) {
            closeAction.run();
            return;
        }
        if (rootPane != null && rootPane.getScene() != null
                && rootPane.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
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
    private void handleChooseTripCover() {
        tripCoverFile = chooseCover();
        if (tripCoverFile != null) {
            tripCoverFileLabel.setText(tripCoverFile.getName());
            showLocalPreview(tripCoverPreview, tripCoverFile);
        }
    }

    @FXML
    private void handleChooseActivityCover() {
        activityCoverFile = chooseCover();
        if (activityCoverFile != null) {
            actCoverFileLabel.setText(activityCoverFile.getName());
            showLocalPreview(actCoverPreview, activityCoverFile);
        }
    }

    @FXML
    private void handleAddTripPhotos() {
        List<File> selectedFiles = choosePhotos();
        tripSecondaryPhotoFiles.addAll(selectedFiles);
        updateSecondaryPhotosLabel(tripSecondaryPhotosLabel, tripSecondaryPhotoFiles);
        selectedFiles.forEach(file -> addPhotoPreview(tripSecondaryPhotoPreview, file.toURI().toString(), "Nuova foto"));
    }

    @FXML
    private void handleAddActivityPhotos() {
        List<File> selectedFiles = choosePhotos();
        activitySecondaryPhotoFiles.addAll(selectedFiles);
        updateSecondaryPhotosLabel(actSecondaryPhotosLabel, activitySecondaryPhotoFiles);
        selectedFiles.forEach(file -> addPhotoPreview(actSecondaryPhotoPreview, file.toURI().toString(), "Nuova foto"));
    }

    private File chooseCover() {
        FileChooser chooser = createImageChooser("Seleziona la foto di copertina");
        return chooser.showOpenDialog(rootPane.getScene().getWindow());
    }

    private List<File> choosePhotos() {
        FileChooser chooser = createImageChooser("Seleziona una o più foto secondarie");
        List<File> selectedFiles = chooser.showOpenMultipleDialog(rootPane.getScene().getWindow());
        return selectedFiles == null ? List.of() : List.copyOf(selectedFiles);
    }

    private FileChooser createImageChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Immagini", "*.jpg", "*.jpeg", "*.png")
        );
        return chooser;
    }

    private void updateSecondaryPhotosLabel(Label label, List<File> files) {
        if (files.isEmpty()) {
            label.setText("Nessuna foto secondaria selezionata");
            return;
        }
        label.setText(files.size() + (files.size() == 1 ? " foto secondaria" : " foto secondarie"));
    }

    private void loadExistingTripPhotoPreviews(TripDTO trip) {
        if (trip.getTripId() == null) return;
        ApiService.get(ApiService.BASE_URL + "/api/trip-images/trip/" + trip.getTripId(),
                response -> {
                    TripImageDTO[] images = ApiService.fromJson(response.body(), TripImageDTO[].class);
                    if (images == null) return;
                    List<TripImageDTO> orderedImages = Arrays.stream(images)
                            .filter(image -> image != null && image.getImageUrl() != null)
                            .sorted(Comparator.comparing(TripImageDTO::getOrderIndex,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .toList();
                    javafx.application.Platform.runLater(() -> {
                        if (tripCoverFile == null && (trip.getCoverPhotoUrl() == null || trip.getCoverPhotoUrl().isBlank())
                                && !orderedImages.isEmpty()) {
                            showRemotePreview(tripCoverPreview, orderedImages.getFirst().getImageUrl());
                        }
                        orderedImages.stream().skip(1)
                                .forEach(image -> addPhotoPreview(tripSecondaryPhotoPreview, image.getImageUrl(), "Foto attuale"));
                    });
                },
                () -> javafx.application.Platform.runLater(() ->
                        showOfferError("Anteprima delle foto del viaggio non disponibile; puoi comunque modificarlo.")),
                null);
    }

    private void showRemotePreview(ImageView preview, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            preview.setImage(null);
            preview.setVisible(false);
            preview.setManaged(false);
            return;
        }
        preview.setImage(new Image(ImageUtils.buildFullUrl(imageUrl), 96, 64, true, true, true));
        preview.setVisible(true);
        preview.setManaged(true);
    }

    private void showLocalPreview(ImageView preview, File file) {
        preview.setImage(new Image(file.toURI().toString(), 96, 64, true, true, true));
        preview.setVisible(true);
        preview.setManaged(true);
    }

    private void addPhotoPreview(FlowPane container, String imageUrl, String caption) {
        ImageView thumbnail = new ImageView();
        thumbnail.setFitWidth(72);
        thumbnail.setFitHeight(48);
        thumbnail.setPreserveRatio(true);
        thumbnail.setImage(new Image(ImageUtils.buildFullUrl(imageUrl), 72, 48, true, true, true));
        Label label = new Label(caption);
        label.getStyleClass().add("form-label");
        VBox tile = new VBox(3, thumbnail, label);
        container.getChildren().add(tile);
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
        tripCoverFile = null;
        tripSecondaryPhotoFiles.clear();
        tripCoverFileLabel.setText(trip.getCoverPhotoUrl() == null || trip.getCoverPhotoUrl().isBlank()
                ? "Nessuna copertina attuale"
                : "Copertina attuale presente");
        showRemotePreview(tripCoverPreview, trip.getCoverPhotoUrl());
        tripSecondaryPhotoPreview.getChildren().clear();
        loadExistingTripPhotoPreviews(trip);
        updateSecondaryPhotosLabel(tripSecondaryPhotosLabel, tripSecondaryPhotoFiles);
        tripDescriptionArea.setText(valueOrEmpty(trip.getDescription()));
        if (saveTripButton != null) saveTripButton.setText("Salva modifiche");
    }

    public void setActivityToEdit(ActivityDTO activity) {
        activityToEdit = activity;
        activityBackButton.setVisible(false);
        activityBackButton.setManaged(false);
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
        activityCoverFile = null;
        activitySecondaryPhotoFiles.clear();
        actCoverFileLabel.setText(activity.getImages() == null || activity.getImages().isEmpty()
                ? "Nessuna copertina attuale"
                : "Copertina attuale presente");
        List<ActivityImageDTO> images = activity.getImages() == null
                ? List.of()
                : activity.getImages().stream()
                .sorted(Comparator.comparing(ActivityImageDTO::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        showRemotePreview(actCoverPreview, images.isEmpty() ? null : images.getFirst().getImageUrl());
        actSecondaryPhotoPreview.getChildren().clear();
        images.stream().skip(1)
                .forEach(image -> addPhotoPreview(actSecondaryPhotoPreview, image.getImageUrl(), "Foto attuale"));
        updateSecondaryPhotosLabel(actSecondaryPhotosLabel, activitySecondaryPhotoFiles);
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
            payload.put("description", emptyToNull(tripDescriptionArea.getText()));
            payload.put("stages", collectStages());

            if (saveTripButton != null) saveTripButton.setDisable(true);
            ApiService.call(
                    ApiService.BASE_URL + (tripToEdit == null ? "/api/trips" : "/api/trips/" + tripToEdit.getTripId()),
                    ApiService.toJson(payload),
                    tripToEdit == null ? ApiService.Method.POST : ApiService.Method.PUT,
                    this::handleTripSaved,
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
                    this::handleActivitySaved,
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

    private void handleTripSaved(java.net.http.HttpResponse<String> response) {
        if (tripCoverFile == null && tripSecondaryPhotoFiles.isEmpty()) {
            closeAfterSave();
            return;
        }
        TripDTO savedTrip = ApiService.fromJson(response.body(), TripDTO.class);
        if (savedTrip == null || savedTrip.getTripId() == null) {
            if (saveTripButton != null) saveTripButton.setDisable(false);
            showSaveError("Il viaggio è stato salvato, ma non è stato possibile recuperare l'ID per caricare la copertina.");
            return;
        }
        boolean isNewTrip = tripToEdit == null;
        tripToEdit = savedTrip;
        uploadTripPhotos(savedTrip, isNewTrip);
    }

    private void handleActivitySaved(java.net.http.HttpResponse<String> response) {
        if (activityCoverFile == null && activitySecondaryPhotoFiles.isEmpty()) {
            closeAfterSave();
            return;
        }
        ActivityDTO savedActivity = ApiService.fromJson(response.body(), ActivityDTO.class);
        if (savedActivity == null || savedActivity.getActivityId() == null) {
            if (saveActivityButton != null) saveActivityButton.setDisable(false);
            showSaveError("L'attività è stata salvata, ma non è stato possibile recuperare l'ID per caricare la copertina.");
            return;
        }
        boolean isNewActivity = activityToEdit == null;
        activityToEdit = savedActivity;
        uploadActivityPhotos(savedActivity, isNewActivity);
    }

    private void uploadTripPhotos(TripDTO trip, boolean isNewTrip) {
        String imageBaseUrl = ApiService.BASE_URL + "/api/trip-images/trip/" + trip.getTripId();
        Runnable onFailure = () -> {
            if (saveTripButton != null) saveTripButton.setDisable(false);
            showSaveError("Il viaggio è stato salvato, ma non è stato possibile caricare tutte le foto. Riprova.");
        };
        Runnable uploadSecondaryPhotos = () -> {
            if (tripSecondaryPhotoFiles.isEmpty()) {
                closeAfterSave();
                return;
            }
            ApiService.uploadFilesDetailed(imageBaseUrl + "/photos",
                    tripSecondaryPhotoFiles.stream().map(File::toPath).toList(),
                    ignored -> closeAfterSave(), onFailure,
                    details -> showPhotoUploadError(true, "foto", details), null);
        };
        if (tripCoverFile == null) {
            if (isNewTrip && !tripSecondaryPhotoFiles.isEmpty()) {
                List<File> initialPhotos = new ArrayList<>(tripSecondaryPhotoFiles);
                ApiService.uploadFilesDetailed(imageBaseUrl + "/photos", initialPhotos.stream().map(File::toPath).toList(),
                        ignored -> closeAfterSave(), onFailure,
                        details -> showPhotoUploadError(true, "foto", details), null);
            } else {
                uploadSecondaryPhotos.run();
            }
            return;
        }
        if (isNewTrip) {
            List<File> initialPhotos = new ArrayList<>();
            initialPhotos.add(tripCoverFile);
            initialPhotos.addAll(tripSecondaryPhotoFiles);
            ApiService.uploadFilesDetailed(imageBaseUrl + "/photos", initialPhotos.stream().map(File::toPath).toList(),
                    ignored -> closeAfterSave(), onFailure,
                    details -> showPhotoUploadError(true, "foto", details), null);
            return;
        }
        ApiService.uploadFileDetailed(imageBaseUrl + "/cover", tripCoverFile.toPath(),
                ignored -> uploadSecondaryPhotos.run(), onFailure,
                details -> showPhotoUploadError(true, "copertina", details), null);
    }

    private void uploadActivityPhotos(ActivityDTO activity, boolean isNewActivity) {
        String imageBaseUrl = ApiService.BASE_URL + "/api/activity-images/activity/" + activity.getActivityId();
        Runnable onFailure = () -> {
            if (saveActivityButton != null) saveActivityButton.setDisable(false);
            showSaveError("L'attività è stata salvata, ma non è stato possibile caricare tutte le foto. Riprova.");
        };
        Runnable uploadSecondaryPhotos = () -> {
            if (activitySecondaryPhotoFiles.isEmpty()) {
                closeAfterSave();
                return;
            }
            ApiService.uploadFilesDetailed(imageBaseUrl + "/photos",
                    activitySecondaryPhotoFiles.stream().map(File::toPath).toList(),
                    ignored -> closeAfterSave(), onFailure,
                    details -> showPhotoUploadError(false, "foto", details), null);
        };
        if (activityCoverFile == null) {
            if (isNewActivity && !activitySecondaryPhotoFiles.isEmpty()) {
                ApiService.uploadFilesDetailed(imageBaseUrl + "/photos",
                        activitySecondaryPhotoFiles.stream().map(File::toPath).toList(),
                        ignored -> closeAfterSave(), onFailure,
                        details -> showPhotoUploadError(false, "foto", details), null);
            } else {
                uploadSecondaryPhotos.run();
            }
            return;
        }
        if (isNewActivity) {
            List<File> initialPhotos = new ArrayList<>();
            initialPhotos.add(activityCoverFile);
            initialPhotos.addAll(activitySecondaryPhotoFiles);
            ApiService.uploadFilesDetailed(imageBaseUrl + "/photos", initialPhotos.stream().map(File::toPath).toList(),
                    ignored -> closeAfterSave(), onFailure,
                    details -> showPhotoUploadError(false, "foto", details), null);
            return;
        }
        ApiService.uploadFileDetailed(imageBaseUrl + "/cover", activityCoverFile.toPath(),
                ignored -> uploadSecondaryPhotos.run(), onFailure,
                details -> showPhotoUploadError(false, "copertina", details), null);
    }

    private void showPhotoUploadError(boolean isTrip, String photoType, String details) {
        if (isTrip) {
            if (saveTripButton != null) saveTripButton.setDisable(false);
            showSaveError("Viaggio salvato, ma caricamento " + photoType + " fallito: " + details);
        } else {
            if (saveActivityButton != null) saveActivityButton.setDisable(false);
            showSaveError("Attività salvata, ma caricamento " + photoType + " fallito: " + details);
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
        closeView();
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