package it.unical.ea_project_javafx.controller.activity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import it.unical.ea_project_javafx.controller.activity.photo.PhotoContainerController;
import it.unical.ea_project_javafx.controller.activity.photo.PhotoGalleryController;
import it.unical.ea_project_javafx.controller.activity.reviews.ReviewController;
import it.unical.ea_project_javafx.controller.activity.reviews.ReviewsContainerController;
import it.unical.ea_project_javafx.controller.common.LoadingOverlayController;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import it.unical.ea_project_javafx.util.FullscreenPhotoViewer;
import it.unical.ea_project_javafx.util.PopupFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ActivityDetailsController {

    @FXML
    private StackPane root;
    @FXML
    private StackPane bodyContainer;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private StackPane newRecCard;
    @FXML
    private StackPane hero;
    @FXML
    private StackPane navbarContainer;

    @FXML
    private Button btnDescrizione;
    @FXML
    private Button btnFoto;
    @FXML
    private Button btnItinerario;
    @FXML
    private Button btnRecensioni;
    @FXML
    private Button btnPrenota;
    @FXML
    private Button btnContactOrganizer;

    @FXML
    private Spinner<Integer> spinnerPartecipanti;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ImageView heroImage;
    @FXML
    private ImageView bgImageView;

    @FXML
    private Label lblCategoria;

    @FXML
    private Label lblLocalita;

    @FXML
    private Label lblPrezzo;

    @FXML
    private Label lblRating;

    @FXML
    private Label lblTitolo;

    @FXML
    private Label lblTotale;

    @FXML
    private VBox bookingCard;

    @FXML
    private LoadingOverlayController loadingCardController;
    @FXML
    private ReviewController newRecCardController;

    @FXML
    private ScrollPane contentScroll;

    private ReviewsContainerController recensioniController;
    private PopupFactory.Handle activeInAppModal;

    private List<Button> sectionButtons;

    private Node itinerarioNode;
    private Node descrizioneNode;
    private Node fotoNode;
    private Node recensioniNode;
    private ActivityDTO activity;
    private TripDTO trip;
    private String id;
    private Type myType;

    private final static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> LocalDateTime.parse(json.getAsString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, type, ctx) -> LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalTime.class,
                    (JsonDeserializer<LocalTime>) (json, type, ctx) -> LocalTime.parse(json.getAsString()))
            .create();

    public enum Type {
        ACTIVITY, TRIP
    }

    final String path = "/it/unical/ea_project_javafx/fxml/activity";

    @FXML
    void initialize() {

        BackgroundHelper.setupBackground(bgImageView);

        setupNavbar();

        loadingOverlay.setVisible(true);
        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        if (btnContactOrganizer != null) {
            btnContactOrganizer.setVisible(!isOrganizer);
            btnContactOrganizer.setManaged(!isOrganizer);
        }
        // bookingCard.setVisible(!isOrganizer);
        // bookingCard.setManaged(!isOrganizer);

        btnInitialize();

        // USE FOR DEBUG AND TEST
        // loadData(Type.ACTIVITY, "1");
    }

    private void setupNavbar() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Node navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);

        } catch (IOException e) {
            System.err.println("Errore nel caricamento della navbar: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void loadData(Type type, String id) {

        myType = type;
        this.id = id;

        switch (type) {
            case ACTIVITY -> {
                ApiService.get(
                        ApiService.BASE_URL + "/api/activities/" + id,
                        response -> {
                            activity = GSON.fromJson(response.body(), ActivityDTO.class);

                            setActivityData();
                            handleDescrizione(null);
                            contentScroll.setManaged(true);
                            contentScroll.setVisible(true);
                            it.unical.ea_project_javafx.util.PageTransitions.appear(contentScroll);
                            loadingOverlay.setVisible(false);
                        },
                        () -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setError("Errore nel caricamento");
                        },
                        loading -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setMessage("Caricamento...");
                        });
            }
            case TRIP -> {
                ApiService.get(
                        ApiService.BASE_URL + "/api/trips/" + id,
                        response -> {
                            trip = GSON.fromJson(response.body(), TripDTO.class);

                            setTripData();
                            handleDescrizione(null);
                            loadingOverlay.setVisible(false);
                            contentScroll.setManaged(true);
                            contentScroll.setVisible(true);
                            it.unical.ea_project_javafx.util.PageTransitions.appear(contentScroll);
                        },
                        () -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setError("Errore nel caricamento");
                        },
                        loading -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setMessage("Caricamento...");
                        });
            }
        }
    }

    @FXML
    private void handleContactOrganizer() {
        if (!UserSession.getInstance().isLoggedIn()) {
            SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/login/login.fxml", null);
            return;
        }
        Long organizer = trip != null ? trip.getCreatedByUserId()
                : activity != null ? activity.getCreatedByUserId() : null;
        if (organizer != null)
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/it/unical/ea_project_javafx/fxml/chat/chat.fxml"));
                loader.setControllerFactory(type -> {
                    if (type == it.unical.ea_project_javafx.controller.chat.ChatController.class)
                        return new it.unical.ea_project_javafx.controller.chat.ChatController(true);
                    try {
                        return type.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                Parent view = loader.load();
                var chat = (it.unical.ea_project_javafx.controller.chat.ChatController) loader.getController();
                chat.compact();
                var popup = PopupFactory.show(PopupFactory.resolveRoot(root), view, PopupFactory.Size.MEDIUM);
                popup.onClosed(chat::close);
                chat.openOrganizer(organizer);
            } catch (IOException ex) {
                PopupFactory.showMessage(PopupFactory.resolveRoot(root), "Messaggi",
                        "Non è stato possibile aprire la chat.", PopupFactory.Size.COMPACT);
            }

    }

    private void setActivityData() {
        String imageUrl = activity.getImages() == null || activity.getImages().isEmpty()
                ? "/photo/default/experience.jpg"
                : activity.getImages().getFirst().getImageUrl();
        setHeroImage(imageUrl);

        if (activity.getStartDate() != null) {
            datePicker.setValue(LocalDate.from(activity.getStartDate()));
        } else {
            datePicker.setValue(LocalDate.now());
        }

        lblLocalita.setText(activity.getCity());
        lblCategoria.setText(activity.getCategory());
        lblTitolo.setText(activity.getTitle());
        lblRating.setText("★ " + formatRating(activity.getAverageRating()));

        BigDecimal prezzo = activity.getPrice() != null ? BigDecimal.valueOf(activity.getPrice()) : null;
        lblPrezzo.setText(setMoneyCurrency(prezzo));
        lblTotale.setText(setMoneyCurrency(prezzo));

    }

    private void setTripData() {
        String imageUrl = trip.getCoverPhotoUrl() == null || trip.getCoverPhotoUrl().isBlank()
                ? "/photo/default/experience.jpg"
                : trip.getCoverPhotoUrl();
        setHeroImage(imageUrl);

        if (trip.getStartDate() != null) {
            datePicker.setValue(trip.getStartDate());
        } else {
            datePicker.setValue(LocalDate.now());
        }

        lblCategoria.setText("TRIP");
        lblLocalita.setText(trip.getDestinationCity());
        lblTitolo.setText(trip.getTitle());

        lblRating.setText("★ " + formatRating(trip.getAverageRating()));
        lblPrezzo.setText(setMoneyCurrency(trip.getTotalPrice()));
        lblTotale.setText(setMoneyCurrency(trip.getTotalPrice()));

    }

    private void setHeroImage(String imageUrl) {
        if (!heroImage.fitWidthProperty().isBound()) {
            heroImage.fitWidthProperty().bind(hero.widthProperty());
            heroImage.fitHeightProperty().bind(hero.heightProperty());
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(hero.widthProperty());
            clip.heightProperty().bind(hero.heightProperty());
            clip.setArcWidth(32);
            clip.setArcHeight(32);
            hero.setClip(clip);
        }

        String defaultImageUrl = "/photo/default/experience.jpg";
        String requestedImageUrl = (imageUrl == null || imageUrl.isBlank()) ? defaultImageUrl : imageUrl;

        String targetUrl = resolveUrl(requestedImageUrl);
        String fallbackUrl = resolveUrl(defaultImageUrl);

        Image image = new Image(targetUrl, true);

        if (!targetUrl.equals(fallbackUrl)) {
            image.errorProperty().addListener((observable, wasError, isError) -> {
                if (isError) {
                    heroImage.setImage(new Image(fallbackUrl, true));
                }
            });
        }

        heroImage.setImage(image);

    }

    private String resolveUrl(String url) {

        if (url == null || url.isBlank()) {
            return ApiService.BASE_URL + "/photo/default/experience.jpg";
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return ApiService.BASE_URL + (url.startsWith("/") ? url : "/" + url);
    }

    private void refreshRating() {
        String endpoint = (myType == Type.TRIP) ? "/api/trips/" + id : "/api/activities/" + id;

        ApiService.get(
                ApiService.BASE_URL + endpoint,
                response -> {
                    if (myType == Type.TRIP) {
                        trip = GSON.fromJson(response.body(), TripDTO.class);
                        lblRating.setText("★ " + formatRating(trip.getAverageRating()));
                    } else {
                        activity = GSON.fromJson(response.body(), ActivityDTO.class);
                        lblRating.setText("★ " + formatRating(activity.getAverageRating()));
                    }
                },
                null,
                null);
    }

    private String formatRating(Number rating) {
        return rating != null ? String.format("%.1f", rating.doubleValue()) : "0";
    }

    private Node loadSection(String fxmlName) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/" + fxmlName));
            Node node = loader.load();
            Object controller = loader.getController();

            if (controller instanceof PhotoContainerController fcc) {
                fcc.setOnGalleryRequested(this::openGallery);
                fcc.setOnFotoRequested(url -> FullscreenPhotoViewer.mostra(root, url));
            }

            if (controller instanceof ReviewsContainerController rcc) {
                rcc.setOnNewReviewRequested(this::openNewReview);
                rcc.setOnEditReviewRequested(this::openEditReview);
                rcc.setOnReviewsChanged(this::refreshRating);
                recensioniController = rcc;
            }

            if (myType == Type.ACTIVITY && activity != null
                    && controller instanceof ActivitySectionController asc) {
                asc.setData(activity);
            } else if (myType == Type.TRIP && trip != null
                    && controller instanceof TripSectionController tsc) {
                tsc.setData(trip);
            }

            return node;

        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile caricare la sezione: " + fxmlName, e);
        }
    }

    private void openGallery(List<String> urls) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/photo/photo-gallery.fxml"));
            Node galleryNode = loader.load();
            PhotoGalleryController controller = loader.getController();

            controller.setImages(urls);

            Stage stage = (Stage) root.getScene().getWindow();
            double width = stage.getScene() != null ? stage.getScene().getWidth() : 1280;
            double height = stage.getScene() != null ? stage.getScene().getHeight() : 720;

            Scene previousScene = root.getScene();
            controller.setOnBackLink(() -> {
                stage.setScene(previousScene);
            });

            Scene newScene = new Scene((Parent) galleryNode, width, height);
            stage.setScene(newScene);
            stage.show();

        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile caricare la galleria", e);
        }

    }

    private void openNewReview() {

        newRecCardController.setOnClose(() -> {
            newRecCard.setManaged(false);
            newRecCard.setVisible(false);
            newRecCard.setDisable(true);
            newRecCardController.reset();
        });

        newRecCardController.setOnReviewCreated(review -> {
            if (recensioniController != null)
                recensioniController.addReview(review);
            refreshRating();
        });

        newRecCardController.setOnReviewUpdated(updated -> {
            if (recensioniController != null)
                recensioniController.updateReview(updated);
            refreshRating();
        });

        Long tripId = (myType == Type.TRIP) ? trip.getTripId() : null;
        Long activityId = (myType == Type.ACTIVITY) ? activity.getActivityId() : null;
        newRecCardController.setType(tripId, activityId);

        newRecCard.setDisable(false);
        newRecCard.setManaged(true);
        newRecCard.setVisible(true);

    }

    private void openEditReview(ReviewDTO review) {
        openNewReview();
        newRecCardController.setEditMode(review);
    }

    private void showSection(Node node) {
        bodyContainer.getChildren().setAll(node);
        it.unical.ea_project_javafx.util.PageTransitions.appear(node);
    }

    @FXML
    private void handleDescrizione(ActionEvent event) {
        setActiveButton(btnDescrizione);
        descrizioneNode = loadSection("activity-description.fxml");
        showSection(descrizioneNode);
    }

    @FXML
    private void handleItinerario(ActionEvent event) {
        setActiveButton(btnItinerario);
        itinerarioNode = loadSection("itinerary.fxml");
        showSection(itinerarioNode);
    }

    @FXML
    protected void handleFoto(ActionEvent event) {
        setActiveButton(btnFoto);
        fotoNode = loadSection("photo/photo-container.fxml");
        showSection(fotoNode);
    }

    @FXML
    protected void handleRecensioni(ActionEvent event) {
        setActiveButton(btnRecensioni);
        recensioniNode = loadSection("reviews/reviews-container.fxml");
        showSection(recensioniNode);
    }

    private void btnInitialize() {
        sectionButtons = List.of(btnDescrizione, btnItinerario, btnFoto, btnRecensioni);

        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 5, 1);
        spinnerPartecipanti.setValueFactory(valueFactory);

        spinnerPartecipanti.valueProperty().addListener((obs, oldValue, newValue) -> {
            aggiornaPrezzoTotale(newValue);
        });
    }

    @FXML
    private void handlePrenota() {
        if ("ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole()))
            return;
        if (!UserSession.getInstance().isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Accesso richiesto", "Devi effettuare il login per prenotare.");
            return;
        }
        if ((myType == Type.ACTIVITY && activity == null) || (myType == Type.TRIP && trip == null)) {
            showAlert(Alert.AlertType.ERROR, "Esperienza non disponibile",
                    "Non è stato possibile caricare i dati necessari alla prenotazione.");
            return;
        }

        int seats = spinnerPartecipanti.getValue() == null ? 1 : spinnerPartecipanti.getValue();
        BigDecimal unitPrice = myType == Type.ACTIVITY
                ? BigDecimal.valueOf(activity.getPrice() == null ? 0.0 : activity.getPrice())
                : (trip.getTotalPrice() == null ? BigDecimal.ZERO : trip.getTotalPrice());
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(seats));

        VBox panel = new VBox(14);
        panel.setMaxWidth(500);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        HBox heading = new HBox(12);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox labels = new VBox(4);
        HBox.setHgrow(labels, javafx.scene.layout.Priority.ALWAYS);
        Label title = new Label("Conferma prenotazione");
        title.getStyleClass().add("app-modal-title");
        Label subtitle = new Label(myType == Type.TRIP ? "Riepilogo del viaggio" : "Riepilogo dell'attività");
        subtitle.getStyleClass().add("app-modal-subtitle");
        labels.getChildren().addAll(title, subtitle);
        Button close = new Button("×");
        close.getStyleClass().add("app-modal-close");
        close.setAccessibleText("Chiudi conferma prenotazione");
        close.setOnAction(event -> closeInAppModal());
        heading.getChildren().addAll(labels, close);

        Label experience = new Label(lblTitolo.getText() == null ? "Esperienza" : lblTitolo.getText());
        experience.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label date = new Label("Data: " + (datePicker.getValue() == null ? "secondo il programma dell'esperienza"
                : datePicker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        date.getStyleClass().add("app-modal-subtitle");
        HBox seatsRow = new HBox(8, new Label("Partecipanti"), new Region(), new Label(String.valueOf(seats)));
        seatsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(seatsRow.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        HBox totalRow = new HBox(8, new Label("Totale stimato"), new Region(), new Label(setMoneyCurrency(total)));
        totalRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(totalRow.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        totalRow.getChildren().get(2).setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e7e5e4;");
        Label note = new Label(
                "Controlla i dati prima di confermare. La prenotazione verrà registrata sul tuo account.");
        note.setWrapText(true);
        note.getStyleClass().add("app-modal-subtitle");
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Annulla");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(event -> closeInAppModal());
        Button confirm = new Button("Conferma prenotazione");
        confirm.getStyleClass().add("btn-primary");
        confirm.setOnAction(event -> submitBooking(seats, confirm));
        actions.getChildren().addAll(cancel, confirm);
        panel.getChildren().addAll(heading, experience, date, seatsRow, totalRow, note, actions);
        closeInAppModal();
        activeInAppModal = PopupFactory.show(root, panel, PopupFactory.Size.COMPACT);
    }

    private void submitBooking(int seats, Button confirmButton) {
        confirmButton.setDisable(true);
        confirmButton.setText("Registrazione in corso…");
        String endpoint = myType == Type.TRIP
                ? "/api/bookings/trips/" + trip.getTripId()
                : "/api/bookings/activities/" + activity.getActivityId();
        ApiService.post(ApiService.BASE_URL + endpoint + "?seats=" + seats, null,
                response -> javafx.application.Platform.runLater(() -> {
                    closeInAppModal();
                    showAlert(Alert.AlertType.INFORMATION, "Prenotazione completata",
                            "La prenotazione è stata registrata correttamente.");
                }),
                () -> javafx.application.Platform.runLater(() -> showBookingFailure(
                        "Non è stato possibile registrare la prenotazione. Verifica la disponibilità e riprova.")),
                null);
    }

    private void showBookingFailure(String message) {
        closeInAppModal();
        if (root == null)
            return;
        VBox panel = new VBox(14);
        panel.setMaxWidth(480);
        Label title = new Label("Prenotazione non completata");
        title.getStyleClass().add("app-modal-title");
        Label body = new Label(message);
        body.setWrapText(true);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button close = new Button("Chiudi");
        close.getStyleClass().add("btn-secondary");
        close.setOnAction(event -> closeInAppModal());
        Button retry = new Button("Riprova");
        retry.getStyleClass().add("btn-primary");
        retry.setOnAction(event -> {
            closeInAppModal();
            handlePrenota();
        });
        actions.getChildren().addAll(close, retry);
        panel.getChildren().addAll(title, body, actions);
        activeInAppModal = PopupFactory.show(root, panel, PopupFactory.Size.COMPACT);
    }

    private void closeInAppModal() {
        if (activeInAppModal != null) {
            activeInAppModal.close();
            activeInAppModal = null;
        }
    }

    private void showAlert(Alert.AlertType type, String titleText, String message) {
        if (root == null)
            return;
        closeInAppModal();
        VBox panel = new VBox(14);
        panel.setMaxWidth(480);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        Label title = new Label(titleText == null ? "Informazione" : titleText);
        title.getStyleClass().add("app-modal-title");
        Label body = new Label(message == null ? "Operazione completata." : message);
        body.setWrapText(true);
        Button close = new Button("Ho capito");
        close.getStyleClass().add("btn-primary");
        close.setOnAction(event -> closeInAppModal());
        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);
        panel.getChildren().addAll(title, body, actions);
        activeInAppModal = PopupFactory.show(root, panel, PopupFactory.Size.COMPACT);
    }

    private void aggiornaPrezzoTotale(Integer newValue) {
        Integer partecipanti = spinnerPartecipanti.getValue();
        BigDecimal prezzo = (myType == Type.ACTIVITY)
                ? (activity.getPrice() != null ? BigDecimal.valueOf(activity.getPrice()) : BigDecimal.ZERO)
                : trip.getTotalPrice();

        BigDecimal totale = (prezzo != null) ? (prezzo.multiply(BigDecimal.valueOf(partecipanti))) : BigDecimal.ZERO;

        lblTotale.setText(setMoneyCurrency(totale));
    }

    private String setMoneyCurrency(BigDecimal amount) {
        return amount + "€";
    }

    private void setActiveButton(Button active) {
        for (Button b : sectionButtons) {
            b.getStyleClass().remove("section-button-active");
            if (!b.getStyleClass().contains("section-button")) {
                b.getStyleClass().add("section-button");
            }
        }
        active.getStyleClass().remove("section-button");
        active.getStyleClass().add("section-button-active");
    }

}
