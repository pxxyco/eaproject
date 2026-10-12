package it.unical.ea_project_javafx.controller.profile;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.BookingDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Effect;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @FXML private ListView<DashboardOffer> activitiesListView;

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
        openOfferDialog(event, null, null);
    }

    private void openOfferDialog(ActionEvent event, TripDTO tripToEdit, ActivityDTO activityToEdit) {
        clearDashboardError("offer-form");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/profile/create_offer.fxml"));
            Parent popup = loader.load();
            if (tripToEdit != null || activityToEdit != null) {
                CreateOfferController controller = loader.getController();
                if (tripToEdit != null) controller.setTripToEdit(tripToEdit);
                else controller.setActivityToEdit(activityToEdit);
            }

            Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
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
            ownerSceneRoot(event).setEffect(null);
            LOGGER.log(Level.WARNING, "Unable to open the create-offer form", exception);
            showDashboardError("offer-form", "Non è stato possibile aprire il modulo dell'offerta.");
        }
    }

    private javafx.scene.Parent ownerSceneRoot(ActionEvent event) {
        return ((Node) event.getSource()).getScene().getRoot();
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
        setupActivityCellFactory();
        activitiesListView.setItems(FXCollections.observableArrayList());
        activitiesListView.setPlaceholder(new Label("Non hai ancora pubblicato viaggi o attività."));
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
        List<DashboardOffer> offers = new java.util.ArrayList<>();
        trips.stream()
                .map(trip -> new DashboardOffer("TRIP", trip.getTripId(), trip.getTitle(), trip.getStatus(),
                        trip.getMaxSeats(), trip.getAvailableSeats(), trip.getTotalPrice(), trip, null,
                        trip.getDestinationCity(), formatTripDates(trip)))
                .forEach(offers::add);
        activities.stream()
                .map(activity -> new DashboardOffer("ACTIVITY", activity.getActivityId(), activity.getTitle(),
                        activity.getStatus(), activity.getMaxSeats(), activity.getAvailableSeats(),
                        activity.getPrice() == null ? null : BigDecimal.valueOf(activity.getPrice()), null, activity,
                        activity.getCity(), formatActivityDates(activity)))
                .forEach(offers::add);
        activitiesListView.setItems(FXCollections.observableArrayList(offers));
        publishedCountLabel.setText(String.valueOf(offers.stream()
                .filter(offer -> "PUBLISHED".equalsIgnoreCase(offer.status()))
                .count()));
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

    private String formatActivityDates(ActivityDTO activity) {
        LocalDateTime start = activity.getStartDate();
        LocalDateTime end = activity.getEndDate();
        if (start == null) return "";
        String startText = start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (end == null) return startText;
        String endText = end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return start.toLocalDate().equals(end.toLocalDate())
                ? startText + " - " + end.format(DateTimeFormatter.ofPattern("HH:mm"))
                : startText + " - " + endText;
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
        if (dashboardErrorLabel == null) {
            return;
        }
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

    private void setupActivityCellFactory() {
        activitiesListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(DashboardOffer offer, boolean empty) {
                super.updateItem(offer, empty);

                if (empty || offer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox card = new VBox(8);
                    card.getStyleClass().add("activity-card");

                    HBox topBox = new HBox(10);
                    topBox.setAlignment(Pos.CENTER_LEFT);

                    Label typeLabel = new Label(offer.type().equals("TRIP") ? "VIAGGIO" : "ATTIVITÀ");
                    typeLabel.getStyleClass().add("activity-card-type");
                    Label titleLabel = new Label(offer.title());
                    titleLabel.getStyleClass().add("activity-card-title");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label statusBadge = new Label(offer.status() != null ? offer.status() : "N/D");
                    statusBadge.getStyleClass().add("badge-published");

                    topBox.getChildren().addAll(typeLabel, spacer, statusBadge);

                    int total = offer.maxSeats() != null ? offer.maxSeats() : 0;
                    int available = offer.availableSeats() != null ? offer.availableSeats() : total;
                    int sold = Math.max(0, total - available);
                    Label seatsLabel = new Label("Posti Venduti: " + sold + " / " + total);
                    seatsLabel.getStyleClass().add("activity-card-seats");

                    String details = offer.location() == null || offer.location().isBlank()
                            ? valueOrEmpty(offer.dates())
                            : offer.dates() == null || offer.dates().isBlank()
                            ? offer.location()
                            : offer.location() + "  •  " + offer.dates();
                    Label detailsLabel = new Label(details);
                    detailsLabel.getStyleClass().add("activity-card-details");
                    detailsLabel.setWrapText(true);
                    titleLabel.setWrapText(true);

                    HBox bottomBox = new HBox(8);
                    bottomBox.setAlignment(Pos.CENTER_LEFT);

                        double price = offer.price() != null
                            ? offer.price().doubleValue()
                            : 0.0;
                    Label priceLabel = new Label(String.format("€ %.2f", price));
                    priceLabel.getStyleClass().add("activity-card-price");

                    Region bottomSpacer = new Region();
                    HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

                    Button editBtn = new Button("Modifica");
                    editBtn.getStyleClass().add("btn-card-action");
                    editBtn.setOnAction(e -> openOfferDialog(e, offer.trip(), offer.activity()));

                    Button deleteBtn = new Button("Cancella");
                    deleteBtn.getStyleClass().add("btn-card-delete");
                    deleteBtn.setOnAction(e -> handleDeleteOffer(offer));

                    bottomBox.getChildren().addAll(priceLabel, bottomSpacer, editBtn, deleteBtn);

                    card.getChildren().addAll(topBox, titleLabel, detailsLabel, seatsLabel, bottomBox);
                    setGraphic(card);
                }
            }
        });
    }

    private void adjustTableHeight() {
        subscribersTable.fixedCellSizeProperty().set(40);
        subscribersTable.prefHeightProperty().bind(
                Bindings.size(subscribersTable.getItems())
                        .multiply(subscribersTable.fixedCellSizeProperty())
                        .add(45) // offset header
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}