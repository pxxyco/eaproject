package it.unical.ea_project_javafx.controller.organizer;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.home.ExperienceCardController;
import it.unical.ea_project_javafx.controller.profile.ProfileDashboardController;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.BookingDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrganizerDashboardController implements ProfileDashboardController {

    private static final Logger LOGGER = Logger.getLogger(OrganizerDashboardController.class.getName());

    @FXML private javafx.scene.layout.StackPane rootStackPane;

    @FXML private Label dashboardErrorLabel;
    @FXML private Label publishedCountLabel;
    @FXML private Label totalSubscribersLabel;
    @FXML private Label totalRevenueLabel;

    @FXML private FlowPane activityCardsContainer;
    @FXML private Label activitiesPlaceholderLabel;
    @FXML private FlowPane tripCardsContainer;
    @FXML private Label tripsPlaceholderLabel;

    @FXML private TableView<SubscriberRow> subscribersTable;
    @FXML private TableColumn<SubscriberRow, String> participantColumn;
    @FXML private TableColumn<SubscriberRow, String> emailColumn;
    @FXML private TableColumn<SubscriberRow, String> activityColumn;
    @FXML private TableColumn<SubscriberRow, String> dateColumn;

    private UserSession userSession;
    private List<TripDTO> trips = List.of();
    private List<ActivityDTO> activities = List.of();
    private final Map<String, String> dashboardErrors = new LinkedHashMap<>();

    private record DashboardOffer(
            String type,
            Long id,
            String title,
            String status,
            Integer maxSeats,
            Integer availableSeats,
            BigDecimal price,
            TripDTO trip,
            ActivityDTO activity,
            String location,
            String dates
    ) {}

    @FXML
    private void handleOpenCreateOffer(ActionEvent event) {
        openOfferDialog((Node) event.getSource(), null, null);
    }

    private void openOfferDialog(Node source, TripDTO tripToEdit, ActivityDTO activityToEdit) {
        clearDashboardError("offer-form");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/organizer/create-offer.fxml"));
            Parent popup = loader.load();
            if (tripToEdit != null || activityToEdit != null) {
                CreateOfferController controller = loader.getController();
                if (tripToEdit != null) controller.setTripToEdit(tripToEdit);
                else controller.setActivityToEdit(activityToEdit);
            }

            Stage owner = (Stage) source.getScene().getWindow();
            Stage modal = new Stage(StageStyle.TRANSPARENT);
            modal.initOwner(owner);
            modal.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(popup, 920, 720);
            scene.setFill(Color.TRANSPARENT);
            modal.setScene(scene);
            modal.setOnShown(ignored -> {
                modal.setX(owner.getX() + (owner.getWidth() - modal.getWidth()) / 2.0);
                modal.setY(owner.getY() + (owner.getHeight() - modal.getHeight()) / 2.0);
            });

            Effect previousEffect = owner.getScene().getRoot().getEffect();
            owner.getScene().getRoot().setEffect(new GaussianBlur(8));
            modal.setResizable(false);
            modal.setOnHidden(ignored -> {
                owner.getScene().getRoot().setEffect(previousEffect);
                if (userSession != null) {
                    loadTrips(userSession);
                    loadActivities(userSession);
                    loadSubscribers(userSession);
                }
            });
            modal.showAndWait();
        } catch (IOException | RuntimeException exception) {
            ownerSceneRoot(source).setEffect(null);
            LOGGER.log(Level.WARNING, "Unable to open the create-offer form", exception);
            showDashboardError("offer-form", "Non è stato possibile aprire il modulo dell'offerta.");
        }
    }

    private javafx.scene.Parent ownerSceneRoot(Node source) {
        return source.getScene().getRoot();
    }

    public static class SubscriberRow {
        private final SimpleStringProperty participant;
        private final SimpleStringProperty email;
        private final SimpleStringProperty activity;
        private final SimpleStringProperty date;

        public SubscriberRow(String participant, String email, String activity, String date) {
            this.participant = new SimpleStringProperty(participant);
            this.email = new SimpleStringProperty(email);
            this.activity = new SimpleStringProperty(activity);
            this.date = new SimpleStringProperty(date);
        }

        public SimpleStringProperty participantProperty() { return participant; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty activityProperty() { return activity; }
        public SimpleStringProperty dateProperty() { return date; }
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        subscribersTable.setItems(FXCollections.observableArrayList());
        subscribersTable.setPlaceholder(new Label("Non ci sono ancora prenotazioni."));
        publishedCountLabel.setText("0");
        totalSubscribersLabel.setText("0");
        totalRevenueLabel.setText("EUR 0.00");
        adjustTableHeight();
    }

    @Override
    public void setUser(UserSession userSession) {
        if (userSession == null || userSession.getId() == null) {
            return;
        }

        this.userSession = userSession;
        loadTrips(userSession);
        loadActivities(userSession);
        loadSubscribers(userSession);
        loadPaidRevenue(userSession);
    }

    private void loadTrips(UserSession session) {
        ApiService.get(
                ApiService.BASE_URL + "/api/trips/organizer/" + session.getId(),
                response -> {
                    Type listType = new TypeToken<List<TripDTO>>() { }.getType();
                    try {
                        List<TripDTO> trips = ApiService.fromJson(response.body(), listType);
                        Platform.runLater(() -> {
                            this.trips = trips == null ? List.of() : trips;
                            clearDashboardError("trips");
                            updateOffers();
                        });
                    } catch (RuntimeException exception) {
                        LOGGER.log(Level.WARNING, "Unable to deserialize organizer trips response", exception);
                        showDashboardError("trips", "Non è stato possibile leggere i viaggi ricevuti dal server.");
                    }
                },
                () -> showDashboardError("trips", "Impossibile caricare i viaggi. Verifica la connessione e riprova."),
                null
        );
    }

    private void loadActivities(UserSession session) {
        ApiService.get(
                ApiService.BASE_URL + "/api/activities/organizer/mine",
                response -> {
                    Type listType = new TypeToken<List<ActivityDTO>>() { }.getType();
                    try {
                        String body = response.body();
                        List<ActivityDTO> loadedActivities = body == null || body.isBlank()
                                || "null".equalsIgnoreCase(body.trim())
                                ? List.of()
                                : ApiService.fromJson(body, listType);
                        Platform.runLater(() -> {
                            activities = loadedActivities == null ? List.of() : loadedActivities;
                            clearDashboardError("activities");
                            updateOffers();
                        });
                    } catch (RuntimeException exception) {
                        LOGGER.log(Level.WARNING, "Unable to deserialize organizer activities response", exception);
                        showDashboardError("activities", "Non è stato possibile leggere le attività ricevute dal server.");
                    }
                },
                () -> showDashboardError("activities", "Impossibile caricare le attività. Verifica la connessione e riprova."),
                null
        );
    }

    private void loadSubscribers(UserSession session) {
        ApiService.get(
                ApiService.BASE_URL + "/api/bookings/organizer/mine",
                response -> {
                    Type listType = new TypeToken<List<BookingDTO>>() { }.getType();
                    try {
                        List<BookingDTO> bookings = ApiService.fromJson(response.body(), listType);
                        Platform.runLater(() -> {
                            clearDashboardError("bookings");
                            updateSubscribers(bookings);
                        });
                    } catch (RuntimeException exception) {
                        LOGGER.log(Level.WARNING, "Unable to deserialize organizer bookings response", exception);
                        showDashboardError("bookings", "Non è stato possibile leggere le prenotazioni ricevute dal server.");
                    }
                },
                () -> showDashboardError("bookings", "Impossibile caricare le prenotazioni. Verifica la connessione e riprova."),
                null
        );
    }

    private void loadPaidRevenue(UserSession session) {
        ApiService.get(
                ApiService.BASE_URL + "/api/payments/organizer/revenue",
                response -> {
                    try {
                        BigDecimal paidRevenue = ApiService.fromJson(response.body(), BigDecimal.class);
                        Platform.runLater(() -> {
                            totalRevenueLabel.setText(String.format("EUR %.2f",
                                    paidRevenue == null ? BigDecimal.ZERO : paidRevenue));
                            clearDashboardError("revenue");
                        });
                    } catch (RuntimeException exception) {
                        LOGGER.log(Level.WARNING, "Unable to deserialize organizer paid revenue response", exception);
                        showDashboardError("revenue", "Non è stato possibile leggere il totale dei pagamenti dal server.");
                    }
                },
                () -> showDashboardError("revenue", "Impossibile caricare il totale dei pagamenti."),
                null
        );
    }

    private void updateOffers() {
        List<DashboardOffer> tripOffers = trips.stream()
                .map(trip -> new DashboardOffer("TRIP", trip.getTripId(), trip.getTitle(), trip.getStatus(),
                        trip.getMaxSeats(), trip.getAvailableSeats(), trip.getTotalPrice(), trip, null,
                        trip.getDestinationCity(), formatTripDates(trip)))
                .toList();
        renderTripCards();
        renderActivityCards();
        long publishedTrips = tripOffers.stream()
                .filter(offer -> "PUBLISHED".equalsIgnoreCase(offer.status()))
                .count();
        long publishedActivities = activities.stream()
                .filter(activity -> "PUBLISHED".equalsIgnoreCase(activity.getStatus()))
                .count();
        publishedCountLabel.setText(String.valueOf(publishedTrips + publishedActivities));
    }

    private void renderActivityCards() {
        activityCardsContainer.getChildren().clear();
        List<ActivityDTO> safeActivities = activities == null ? List.of() : activities;
        activitiesPlaceholderLabel.setVisible(safeActivities.isEmpty());
        activitiesPlaceholderLabel.setManaged(safeActivities.isEmpty());
        for (ActivityDTO activity : safeActivities) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/it/unical/ea_project_javafx/fxml/home/experience-card.fxml"));
                Node card = loader.load();
                ExperienceCardController controller = loader.getController();
                String imageUrl = activity.getImages() == null || activity.getImages().isEmpty()
                        ? "/photo/default/experience.jpg"
                        : activity.getImages().getFirst().getImageUrl();
                controller.setData(new ActivityHomeDTO(
                        activity.getActivityId(),
                        activity.getTitle(),
                        activity.getCity(),
                        activity.getCategory(),
                        activity.getPrice(),
                        activity.getAverageRating(),
                        imageUrl,
                        activity.getStartDate() == null ? null : activity.getStartDate().toString(),
                        activity.getEndDate() == null ? null : activity.getEndDate().toString()
                ));
                controller.setEditAction(() -> openOfferDialog(rootStackPane, null, activity));
                DashboardOffer activityOffer = new DashboardOffer(
                        "ACTIVITY", activity.getActivityId(), activity.getTitle(), activity.getStatus(),
                        activity.getMaxSeats(), activity.getAvailableSeats(),
                        activity.getPrice() == null ? null : BigDecimal.valueOf(activity.getPrice()),
                        null, activity, activity.getCity(), ""
                );
                controller.setDeleteAction(() -> handleDeleteOffer(activityOffer));
                activityCardsContainer.getChildren().add(card);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to render organizer activity card", exception);
                showDashboardError("activity-cards", "Non è stato possibile visualizzare una delle tue attività.");
                return;
            }
        }
        if (!safeActivities.isEmpty()) clearDashboardError("activity-cards");
    }

    private void renderTripCards() {
        tripCardsContainer.getChildren().clear();
        List<TripDTO> safeTrips = trips == null ? List.of() : trips;
        tripsPlaceholderLabel.setVisible(safeTrips.isEmpty());
        tripsPlaceholderLabel.setManaged(safeTrips.isEmpty());
        for (TripDTO trip : safeTrips) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/it/unical/ea_project_javafx/fxml/home/experience-card.fxml"));
                Node card = loader.load();
                ExperienceCardController controller = loader.getController();
                controller.setData(new TripHomeDTO(
                        trip.getTripId(),
                        trip.getTitle(),
                        trip.getDestinationCity(),
                        trip.getTotalPrice(),
                        trip.getAverageRating(),
                        trip.getCoverPhotoUrl(),
                        trip.getStartDate() == null ? null : trip.getStartDate().toString(),
                        trip.getEndDate() == null ? null : trip.getEndDate().toString()
                ));
                controller.setEditAction(() -> openOfferDialog(rootStackPane, trip, null));
                DashboardOffer tripOffer = new DashboardOffer(
                        "TRIP", trip.getTripId(), trip.getTitle(), trip.getStatus(),
                        trip.getMaxSeats(), trip.getAvailableSeats(), trip.getTotalPrice(),
                        trip, null, trip.getDestinationCity(), formatTripDates(trip)
                );
                controller.setDeleteAction(() -> handleDeleteOffer(tripOffer));
                tripCardsContainer.getChildren().add(card);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to render organizer trip card", exception);
                showDashboardError("trip-cards", "Non è stato possibile visualizzare uno dei tuoi viaggi.");
                return;
            }
        }
        if (!safeTrips.isEmpty()) clearDashboardError("trip-cards");
    }

    private String formatTripDates(TripDTO trip) {
        LocalDate start = trip.getStartDate();
        LocalDate end = trip.getEndDate();
        if (start == null) return "";
        return end == null || start.equals(end)
                ? start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - "
                + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void updateSubscribers(List<BookingDTO> bookings) {
        List<BookingDTO> safeBookings = bookings == null ? List.of() : bookings;
        List<SubscriberRow> rows = safeBookings.stream()
                .filter(booking -> !"CANCELLED".equalsIgnoreCase(booking.getBookingStatus()))
                .map(booking -> new SubscriberRow(
                        booking.getParticipantName(),
                        booking.getParticipantEmail(),
                        booking.getItemTitle(),
                        booking.getCreatedAt() == null ? "" : booking.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                ))
                .toList();
        subscribersTable.setItems(FXCollections.observableArrayList(rows));
        int subscribers = safeBookings.stream()
                .filter(booking -> !"CANCELLED".equalsIgnoreCase(booking.getBookingStatus()))
                .map(BookingDTO::getSeats)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        totalSubscribersLabel.setText(String.valueOf(subscribers));
    }

    private void showDashboardError(String key, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showDashboardError(key, message));
            return;
        }
        dashboardErrors.put(key, message);
        refreshDashboardError();
    }

    private void clearDashboardError(String key) {
        dashboardErrors.remove(key);
        refreshDashboardError();
    }

    private void refreshDashboardError() {
        if (dashboardErrorLabel == null) return;
        boolean hasErrors = !dashboardErrors.isEmpty();
        dashboardErrorLabel.setText(String.join("\n", dashboardErrors.values()));
        dashboardErrorLabel.setVisible(hasErrors);
        dashboardErrorLabel.setManaged(hasErrors);
    }

    private void setupTableColumns() {
        participantColumn.setCellValueFactory(cell -> cell.getValue().participantProperty());
        emailColumn.setCellValueFactory(cell -> cell.getValue().emailProperty());
        activityColumn.setCellValueFactory(cell -> cell.getValue().activityProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
    }

    private void adjustTableHeight() {
        subscribersTable.fixedCellSizeProperty().set(40);
        subscribersTable.prefHeightProperty().bind(
                Bindings.size(subscribersTable.getItems())
                        .multiply(subscribersTable.fixedCellSizeProperty())
                        .add(45)
        );
    }

    @FXML
    private void handleNewActivity(ActionEvent event) {
        handleOpenCreateOffer(event);
    }

    private void handleDeleteOffer(DashboardOffer offer) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi davvero cancellare \"" + offer.title() + "\"?",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText(offer.type().equals("TRIP") ? "Cancella viaggio" : "Cancella attività");
        confirmation.showAndWait().filter(button -> button == ButtonType.OK).ifPresent(button ->
                ApiService.delete(
                        ApiService.BASE_URL + "/api/" + (offer.type().equals("TRIP") ? "trips/" : "activities/") + offer.id(),
                        response -> {
                            clearDashboardError("delete");
                            if (userSession != null) {
                                loadTrips(userSession);
                                loadActivities(userSession);
                                loadSubscribers(userSession);
                                loadPaidRevenue(userSession);
                            }
                        },
                        () -> showDashboardError("delete", "Impossibile cancellare l'offerta. Verifica i permessi e riprova."),
                        null
                ));
    }
}