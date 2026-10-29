package it.unical.ea_project_javafx.controller.activity;

import it.unical.ea_project_javafx.controller.activity.photo.PhotoContainerController;
import it.unical.ea_project_javafx.controller.activity.photo.PhotoGalleryController;
import it.unical.ea_project_javafx.controller.activity.reviews.ReviewController;
import it.unical.ea_project_javafx.controller.activity.reviews.ReviewsContainerController;
import it.unical.ea_project_javafx.controller.chat.ChatController;
import it.unical.ea_project_javafx.controller.common.LoadingOverlayController;
import it.unical.ea_project_javafx.dto.activity.ActivityDTO;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.ui.BookingPresentation;
import it.unical.ea_project_javafx.ui.payment.MockCheckoutDialog;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Comparator;
import it.unical.ea_project_javafx.dto.review.ReviewDTO;
import it.unical.ea_project_javafx.dto.trip.TripDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.service.favorites.FavoriteService;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.UiText;
import javafx.geometry.Insets;
import java.math.RoundingMode;
import it.unical.ea_project_javafx.ui.animation.PageTransitions;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.ui.dialog.FullscreenPhotoViewer;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ActivityDetailsController implements AutoCloseable {
    private boolean closed;
    private boolean bookingsLoaded;
    private boolean bookingBusy;
    private List<BookingDTO> ownBookings = List.of();
    private BookingDTO currentBooking;
    @FXML
    private Button btnCancelBooking;
    @FXML
    private Button btnPayBooking;
    @FXML
    private Button btnManageBookings;
    @FXML
    private Label bookingTotalCaption;

    @Override
    public void close() {
        closed = true;
        closeInAppModal();
        bodyContainer.getChildren().clear();
        itinerarioNode = null;
    }

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
    private Label bookingAvailabilityLabel;
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
    @FXML
    private Button btnFavorite;
    @FXML
    private Label experienceSummary;

    private ReviewsContainerController recensioniController;
    private PopupFactory.Handle activeInAppModal;

    private List<Button> sectionButtons;

    private boolean isFavorite;
    private Node itinerarioNode;
    private Node descrizioneNode;
    private Node fotoNode;
    private Node recensioniNode;
    private ActivityDTO activity;
    private TripDTO trip;
    private String id;
    private Type myType;

    public enum Type {
        ACTIVITY, TRIP
    }

    final String path = "/it/unical/ea_project_javafx/fxml/activity";

    @FXML
    void initialize() {

        BackgroundHelper.setupBackground(bgImageView);

        ViewSupport.loadNavbar(navbarContainer);

        loadingOverlay.setVisible(true);
        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        if (btnContactOrganizer != null) {
            btnContactOrganizer.setVisible(!isOrganizer);
            btnContactOrganizer.setManaged(!isOrganizer);
        }
        if (isOrganizer) {
            bookingCard.setManaged(false);
            bookingCard.setVisible(false);
        }

        btnInitialize();

    }

    @FXML
    private void handleBackToExperiences() {
        SceneNavigator.getInstance().goBack();
    }

    public void loadData(Type type, String id) {

        myType = type;
        this.id = id;
        refreshOwnBookings();

        switch (type) {
            case ACTIVITY -> {
                ApiService.get(
                        ApiService.BASE_URL + "/api/activities/" + id,
                        response -> {
                            if (closed)
                                return;
                            activity = ApiService.fromJson(response.body(), ActivityDTO.class);

                            setActivityData();
                            setupFavorite();
                            handleDescrizione(null);
                            Platform.runLater(this::prepareItinerary);
                            contentScroll.setManaged(true);
                            contentScroll.setVisible(true);
                            PageTransitions.appear(contentScroll);
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
                            if (closed)
                                return;
                            trip = ApiService.fromJson(response.body(), TripDTO.class);

                            setTripData();
                            setupFavorite();
                            handleDescrizione(null);
                            Platform.runLater(this::prepareItinerary);
                            loadingOverlay.setVisible(false);
                            contentScroll.setManaged(true);
                            contentScroll.setVisible(true);
                            PageTransitions.appear(contentScroll);
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
                    if (type == ChatController.class)
                        return new ChatController(true);
                    try {
                        return type.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                });
                Parent view = loader.load();
                var chat = (ChatController) loader.getController();
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
        lblPrezzo.setText(UiText.money(prezzo));
        lblTotale.setText(UiText.money(prezzo));
        updateBookingAvailability();
        updateExperienceSummary();

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

        lblCategoria.setText("Viaggio");
        lblLocalita.setText(trip.getDestinationCity());
        lblTitolo.setText(trip.getTitle());

        lblRating.setText("★ " + formatRating(trip.getAverageRating()));
        lblPrezzo.setText(UiText.money(trip.getTotalPrice()));
        lblTotale.setText(UiText.money(trip.getTotalPrice()));
        updateBookingAvailability();
        updateExperienceSummary();

    }

    private void setHeroImage(String imageUrl) {
        if (!heroImage.fitWidthProperty().isBound()) {
            heroImage.setPreserveRatio(false);
            heroImage.fitWidthProperty().bind(hero.widthProperty());
            hero.widthProperty().addListener((o, a, b) -> updateHeroViewport());
            hero.heightProperty().addListener((o, a, b) -> updateHeroViewport());
            heroImage.imageProperty().addListener((o, a, image) -> {
                if (image != null)
                    image.progressProperty().addListener((v, x, y) -> updateHeroViewport());
                updateHeroViewport();
            });
            heroImage.fitHeightProperty().bind(hero.heightProperty());
            Rectangle clip = new Rectangle();
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

    private void updateExperienceSummary() {
        String period;
        if (myType == Type.TRIP) {
            var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALIAN);
            period = (trip.getStartDate() == null ? "Data da definire" : trip.getStartDate().format(formatter))
                    + (trip.getEndDate() == null ? "" : " → " + trip.getEndDate().format(formatter));
        } else {
            var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ITALIAN);
            period = (activity.getStartDate() == null ? "Data da definire" : activity.getStartDate().format(formatter))
                    + (activity.getDurationMinutes() == null ? "" : " · " + activity.getDurationMinutes() + " minuti");
        }
        Integer available = myType == Type.TRIP ? trip.getAvailableSeats() : activity.getAvailableSeats();
        experienceSummary.setText(period + (available == null ? "" : " · " + available + " posti disponibili"));
    }

    private void updateHeroViewport() {
        Image image = heroImage.getImage();
        if (image == null || image.getWidth() <= 0 || hero.getWidth() <= 0 || hero.getHeight() <= 0)
            return;
        double target = hero.getWidth() / hero.getHeight();
        double width = image.getWidth(), height = image.getHeight();
        if (width / height > target)
            width = height * target;
        else
            height = width / target;
        heroImage.setViewport(
                new Rectangle2D((image.getWidth() - width) / 2, (image.getHeight() - height) / 2, width, height));
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
                        trip = ApiService.fromJson(response.body(), TripDTO.class);
                        lblRating.setText("★ " + formatRating(trip.getAverageRating()));
                    } else {
                        activity = ApiService.fromJson(response.body(), ActivityDTO.class);
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
                updateReviewEligibility();
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
        bodyContainer.getChildren()
                .removeIf(child -> child != node && child != descrizioneNode && child != itinerarioNode);
        for (Node child : bodyContainer.getChildren()) {
            child.setVisible(false);
            child.setManaged(false);
        }
        if (!bodyContainer.getChildren().contains(node))
            bodyContainer.getChildren().add(node);
        node.setVisible(true);
        node.setManaged(true);
        node.setOpacity(1);
        PageTransitions.appear(node);
    }

    private void prepareItinerary() {
        if (closed || itinerarioNode != null)
            return;
        itinerarioNode = loadSection("itinerary.fxml");
        itinerarioNode.setManaged(false);
        itinerarioNode.setOpacity(0);
        bodyContainer.getChildren().add(0, itinerarioNode);
        if (itinerarioNode instanceof Region region) {
            region.resize(Math.max(600, bodyContainer.getWidth()), 400);
            region.applyCss();
            region.layout();
        }
        itinerarioNode.setVisible(false);
    }

    @FXML
    private void handleDescrizione(ActionEvent event) {
        setActiveButton(btnDescrizione);
        if (descrizioneNode == null)
            descrizioneNode = loadSection("activity-description.fxml");
        showSection(descrizioneNode);
    }

    @FXML
    private void handleItinerario(ActionEvent event) {
        setActiveButton(btnItinerario);
        if (itinerarioNode == null)
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

    private String bookingUnavailableReason() {
        String status = myType == Type.TRIP ? (trip == null ? null : trip.getStatus())
                : (activity == null ? null : activity.getStatus());
        if ("CANCELLED".equals(status))
            return "Questa esperienza è stata annullata.";
        if ("COMPLETED".equals(status))
            return "Questa esperienza si è già conclusa.";
        if (!"PUBLISHED".equals(status))
            return "Questa esperienza non è attualmente pubblicata.";
        boolean future = myType == Type.TRIP
                ? trip.getStartDate() != null && !trip.getStartDate().isBefore(LocalDate.now())
                : activity.getStartDate() != null && !activity.getStartDate().toLocalDate().isBefore(LocalDate.now());
        if (!future)
            return "Il termine di prenotazione è scaduto: le offerte sono prenotabili fino alle 23:59 del giorno di inizio.";
        Integer available = myType == Type.TRIP ? trip.getAvailableSeats() : activity.getAvailableSeats();
        if (available == null || available < 1)
            return "Non ci sono più posti disponibili per questa esperienza.";
        return null;
    }

    private void updateBookingAvailability() {
        if (myType == null || myType == Type.TRIP && trip == null || myType == Type.ACTIVITY && activity == null)
            return;
        boolean organizer = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        String reason = organizer ? "Le prenotazioni sono riservate agli account Traveler."
                : bookingUnavailableReason();
        btnPrenota.setDisable(bookingBusy || !bookingsLoaded || currentBooking != null || reason != null);
        btnPrenota.setText(!bookingsLoaded ? "Verifica prenotazioni…"
                : currentBooking != null ? "Già prenotata" : reason == null ? "Prenota ora" : "Non prenotabile");
        bookingTotalCaption.setText(currentBooking == null ? "Totale" : "Totale prenotato");
        long count = matchingBookings().stream().filter(b -> !BookingPresentation.isCancelled(b)).count();
        String summary = currentBooking == null
                ? reason == null ? "Prenota per la data indicata nel programma." : reason
                : "La tua prenotazione: " + BookingPresentation.displayStatus(currentBooking, LocalDateTime.now())
                        + " · " + currentBooking.getSeats() + " partecipanti."
                        + (count > 1 ? " Sono presenti " + count
                                + " prenotazioni precedenti per questa esperienza: puoi gestirle dalle tue prenotazioni."
                                : "");
        bookingAvailabilityLabel.setText(bookingsLoaded ? summary : "Verifica delle tue prenotazioni in corso.");
        spinnerPartecipanti.setDisable(bookingBusy || !bookingsLoaded || currentBooking != null || reason != null);
        if (currentBooking != null && currentBooking.getSeats() != null) {
            int seats = currentBooking.getSeats();
            spinnerPartecipanti.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, seats), Math.max(1, seats)));
            if (currentBooking.getPrice() != null && seats > 0) {
                lblPrezzo.setText(UiText
                        .money(currentBooking.getPrice().divide(BigDecimal.valueOf(seats), 2, RoundingMode.HALF_UP)));
                lblTotale.setText(UiText.money(currentBooking.getPrice()));
            }
        } else if (reason == null) {
            int available = myType == Type.TRIP ? trip.getAvailableSeats() : activity.getAvailableSeats();
            int selected = spinnerPartecipanti.getValue() == null ? 1 : spinnerPartecipanti.getValue();
            spinnerPartecipanti.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, available, Math.min(selected, available)));
        }
        boolean cancel = currentBooking != null && BookingPresentation.canCancel(currentBooking, LocalDateTime.now());
        btnManageBookings.setVisible(count > 1);
        btnManageBookings.setManaged(count > 1);
        btnManageBookings.setDisable(bookingBusy);
        btnCancelBooking.setVisible(cancel);
        btnCancelBooking.setManaged(cancel);
        btnCancelBooking.setDisable(bookingBusy);
        boolean pay = MockCheckoutDialog.eligible(currentBooking);
        btnPayBooking.setVisible(pay);
        btnPayBooking.setManaged(pay);
        btnPayBooking.setDisable(bookingBusy);
        datePicker.setDisable(true);
        datePicker.setTooltip(new Tooltip("La data è stabilita dal programma dell'esperienza."));
        updateReviewEligibility();
    }

    private List<BookingDTO> matchingBookings() {
        return ownBookings.stream().filter(b -> myType == Type.TRIP
                ? b.getTripId() != null && b.getTripId().toString().equals(id)
                : b.getActivityId() != null && b.getActivityId().toString().equals(id)).toList();
    }

    private void updateReviewEligibility() {
        if (recensioniController != null)
            recensioniController.setReviewEligible(bookingsLoaded && matchingBookings().stream()
                    .anyMatch(b -> BookingPresentation.canReview(b, LocalDateTime.now())));
    }

    private void refreshOwnBookings() {
        var session = UserSession.getInstance();
        if (!session.isLoggedIn() || !"TRAVELER".equalsIgnoreCase(session.getRole())) {
            ownBookings = List.of();
            currentBooking = null;
            bookingsLoaded = true;
            updateBookingAvailability();
            return;
        }
        bookingsLoaded = false;
        ApiService.callDetailed(
                ApiService.BASE_URL + "/api/bookings/mine/experience?"
                        + (myType == Type.TRIP ? "tripId=" : "activityId=") + id,
                null, ApiService.Method.GET, response -> {
                    if (closed)
                        return;
                    List<BookingDTO> loaded = ApiService.fromJson(response.body(), new TypeToken<List<BookingDTO>>() {
                    }.getType());
                    ownBookings = loaded == null ? List.of() : loaded;
                    currentBooking = matchingBookings().stream().filter(b -> !BookingPresentation.isCancelled(b))
                            .sorted(Comparator
                                    .comparingInt((BookingDTO b) -> "CONFIRMED".equals(b.getPaymentStatus())
                                            || "COMPLETED".equals(b.getPaymentStatus()) ? 0 : 1)
                                    .thenComparing(b -> b.getBookingId() == null ? Long.MAX_VALUE : b.getBookingId()))
                            .findFirst().orElse(null);
                    bookingsLoaded = true;
                    bookingBusy = false;
                    updateBookingAvailability();
                }, message -> {
                    if (!closed) {
                        bookingBusy = false;
                        bookingAvailabilityLabel
                                .setText("Impossibile verificare le prenotazioni. Riapri la scheda per riprovare.");
                        btnPrenota.setDisable(true);
                    }
                }, null);
    }

    private void refreshBookingState() {
        bookingBusy = true;
        updateBookingAvailability();
        String endpoint = myType == Type.TRIP ? "/api/trips/" : "/api/activities/";
        ApiService.callDetailed(ApiService.BASE_URL + endpoint + id, null, ApiService.Method.GET, response -> {
            if (closed)
                return;
            if (myType == Type.TRIP) {
                trip = ApiService.fromJson(response.body(), TripDTO.class);
                setTripData();
            } else {
                activity = ApiService.fromJson(response.body(), ActivityDTO.class);
                setActivityData();
            }
            refreshOwnBookings();
        }, message -> {
            if (!closed) {
                bookingBusy = false;
                updateBookingAvailability();
                showBookingFailure(message);
            }
        }, null);
    }

    @FXML
    private void handlePayBooking() {
        MockCheckoutDialog.show(root, currentBooking, this::refreshBookingState);
    }

    @FXML
    private void handleManageBookings() {
        VBox panel = new VBox(14);
        panel.setMaxWidth(580);
        Label heading = new Label("Prenotazioni per questa esperienza");
        heading.getStyleClass().add("app-modal-title");
        Label explanation = new Label(
                "Qui trovi le prenotazioni precedenti per la stessa data. Puoi annullare le copie non desiderate: i pagamenti registrati restano nello storico.");
        explanation.setWrapText(true);
        VBox entries = new VBox(12);
        for (BookingDTO booking : matchingBookings()) {
            if (BookingPresentation.isCancelled(booking))
                continue;
            VBox entry = new VBox(6);
            entry.getStyleClass().add("surface-card");
            entry.setPadding(new Insets(14));
            Label detail = new Label(BookingPresentation.displayStatus(booking, LocalDateTime.now()) + " · "
                    + booking.getSeats() + " partecipanti · " + UiText.money(booking.getPrice()));
            detail.setWrapText(true);
            Label payment = new Label("Pagamento: " + BookingPresentation.paymentStatus(booking.getPaymentStatus()));
            entry.getChildren().addAll(detail, payment);
            if (BookingPresentation.canCancel(booking, LocalDateTime.now())) {
                Button cancel = new Button("Annulla questa prenotazione");
                cancel.getStyleClass().add("btn-secondary");
                cancel.setOnAction(event -> {
                    closeInAppModal();
                    requestCancellation(booking);
                });
                entry.getChildren().add(cancel);
            }
            entries.getChildren().add(entry);
        }
        ScrollPane scroll = new ScrollPane(entries);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(320);
        Button close = new Button("Chiudi");
        close.getStyleClass().add("btn-secondary");
        close.setOnAction(event -> closeInAppModal());
        panel.getChildren().addAll(heading, explanation, scroll, close);
        closeInAppModal();
        activeInAppModal = PopupFactory.show(root, panel, PopupFactory.Size.COMPACT);
    }

    @FXML
    private void handleCancelBooking() {
        requestCancellation(currentBooking);
    }

    private void requestCancellation(BookingDTO booking) {
        if (booking == null || bookingBusy || !BookingPresentation.canCancel(booking, LocalDateTime.now()))
            return;
        PopupFactory.showConfirmation(root, "Annulla prenotazione",
                "Vuoi annullare la prenotazione per questa esperienza? I posti torneranno disponibili.",
                PopupFactory.Size.COMPACT, () -> {
                    bookingBusy = true;
                    updateBookingAvailability();
                    ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/" + booking.getBookingId(), null,
                            ApiService.Method.DELETE,
                            response -> {
                                if (!closed) {
                                    booking.setBookingStatus("CANCELLED");
                                    refreshBookingState();
                                }
                            },
                            message -> {
                                if (!closed) {
                                    bookingBusy = false;
                                    updateBookingAvailability();
                                    showBookingFailure(message);
                                }
                            }, null);
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

        if (!bookingsLoaded || bookingBusy || currentBooking != null) {
            updateBookingAvailability();
            return;
        }
        String unavailable = bookingUnavailableReason();
        if (unavailable != null) {
            updateBookingAvailability();
            showBookingFailure(unavailable);
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
        HBox.setHgrow(labels, Priority.ALWAYS);
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
        experience.getStyleClass().add("checkout-experience");
        Label date = new Label("Data: " + (datePicker.getValue() == null ? "secondo il programma dell'esperienza"
                : datePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        date.getStyleClass().add("app-modal-subtitle");
        HBox seatsRow = new HBox(8, new Label("Partecipanti"), new Region(), new Label(String.valueOf(seats)));
        seatsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(seatsRow.getChildren().get(1), Priority.ALWAYS);
        HBox totalRow = new HBox(8, new Label("Totale stimato"), new Region(), new Label(UiText.money(total)));
        totalRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(totalRow.getChildren().get(1), Priority.ALWAYS);
        totalRow.getChildren().get(2).getStyleClass().add("checkout-total");
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
        if (bookingBusy)
            return;
        bookingBusy = true;
        confirmButton.setDisable(true);
        confirmButton.setText("Registrazione in corso…");
        String endpoint = myType == Type.TRIP
                ? "/api/bookings/trips/" + trip.getTripId()
                : "/api/bookings/activities/" + activity.getActivityId();
        ApiService.callDetailed(ApiService.BASE_URL + endpoint + "?seats=" + seats, null, ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    if (closed)
                        return;
                    bookingBusy = false;
                    currentBooking = ApiService.fromJson(response.body(), BookingDTO.class);
                    ownBookings = new ArrayList<>(ownBookings);
                    ownBookings.add(0, currentBooking);
                    bookingsLoaded = true;
                    updateBookingAvailability();
                    closeInAppModal();
                    refreshBookingState();
                    showAlert(Alert.AlertType.INFORMATION, "Prenotazione registrata",
                            currentBooking != null && "PENDING".equals(currentBooking.getBookingStatus())
                                    ? "Prenotazione registrata. Completa il pagamento dalla scheda o dalle tue prenotazioni."
                                    : "La prenotazione è stata confermata.");
                }),
                message -> Platform.runLater(() -> {
                    if (!closed) {
                        refreshBookingState();
                        showBookingFailure(message);
                    }
                }),
                null);
    }

    private void showBookingFailure(String message) {
        closeInAppModal();
        if (root == null)
            return;
        VBox panel = new VBox(14);
        panel.setMaxWidth(480);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        Label title = new Label("Impossibile prenotare");
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
        if (currentBooking != null) {
            lblTotale.setText(UiText.money(currentBooking.getPrice()));
            return;
        }
        Integer partecipanti = spinnerPartecipanti.getValue();
        BigDecimal prezzo = (myType == Type.ACTIVITY)
                ? (activity.getPrice() != null ? BigDecimal.valueOf(activity.getPrice()) : BigDecimal.ZERO)
                : trip.getTotalPrice();

        BigDecimal totale = (prezzo != null) ? (prezzo.multiply(BigDecimal.valueOf(partecipanti))) : BigDecimal.ZERO;

        lblTotale.setText(UiText.money(totale));
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

    private void setupFavorite() {
        boolean traveler = UserSession.getInstance().isLoggedIn()
                && "TRAVELER".equalsIgnoreCase(UserSession.getInstance().getRole());
        btnFavorite.setVisible(traveler);
        btnFavorite.setManaged(traveler);
        if (!traveler)
            return;
        FavoriteService.state(FavoriteService.Target.valueOf(myType.name()), id, this::updateFavoriteButton,
                message -> updateFavoriteButton(false), btnFavorite::setDisable);
    }

    @FXML
    private void handleFavorite() {
        FavoriteService.set(FavoriteService.Target.valueOf(myType.name()), id, !isFavorite,
                this::updateFavoriteButton,
                message -> showAlert(Alert.AlertType.ERROR, "Preferiti", message), btnFavorite::setDisable);
    }

    private void updateFavoriteButton(boolean favorite) {
        isFavorite = favorite;
        btnFavorite.setText(favorite ? "♥" : "♡");
        btnFavorite.getStyleClass().remove("active");
        if (favorite)
            btnFavorite.getStyleClass().add("active");
    }

}
