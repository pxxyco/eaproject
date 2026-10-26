package it.unical.ea_project_javafx.controller.organizer;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.home.ExperienceCardController;
import it.unical.ea_project_javafx.controller.profile.ProfileDashboardController;
import it.unical.ea_project_javafx.dto.activity.ActivityDTO;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.dto.trip.TripDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

public class OrganizerDashboardController implements ProfileDashboardController {

    private static final Logger LOGGER = Logger.getLogger(OrganizerDashboardController.class.getName());

    @FXML
    private StackPane rootStackPane;

    @FXML
    private TextField offerSearchField;
    @FXML
    private ComboBox<String> offerStatusFilter;
    @FXML
    private Label dashboardErrorLabel;
    @FXML
    private Label publishedCountLabel;
    @FXML
    private Label totalSubscribersLabel;
    @FXML
    private Label totalRevenueLabel;

    @FXML
    private FlowPane activityCardsContainer;
    @FXML
    private Label activitiesPlaceholderLabel;
    @FXML
    private FlowPane tripCardsContainer;
    @FXML
    private Label tripsPlaceholderLabel;

    @FXML
    private TableView<SubscriberRow> subscribersTable;
    @FXML
    private TableColumn<SubscriberRow, String> participantColumn;
    @FXML
    private TableColumn<SubscriberRow, String> emailColumn;
    @FXML
    private TableColumn<SubscriberRow, String> activityColumn;
    @FXML
    private TableColumn<SubscriberRow, String> dateColumn;

    private UserSession userSession;
    private List<TripDTO> trips = List.of();
    private List<ActivityDTO> activities = List.of();
    private final Map<String, String> dashboardErrors = new LinkedHashMap<>();
    private PopupFactory.Handle activeOfferPopup;

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
            String dates) {
    }

    @FXML
    private void handleRefresh() {
        if (userSession != null)
            setUser(userSession);
    }

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
            CreateOfferController controller = loader.getController();
            if (tripToEdit != null)
                controller.setTripToEdit(tripToEdit);
            else if (activityToEdit != null)
                controller.setActivityToEdit(activityToEdit);
            controller.setCloseAction(() -> {
                PopupFactory.Handle handle = activeOfferPopup;
                activeOfferPopup = null;
                if (handle != null)
                    handle.close();
                refreshOrganizerDashboard();
            });
            if (popup instanceof StackPane stackPane
                    && !stackPane.getStyleClass().contains("popup-factory-transparent-surface")) {
                stackPane.getStyleClass().add("popup-factory-transparent-surface");
            }
            activeOfferPopup = PopupFactory.show(rootStackPane, popup, PopupFactory.Size.LARGE);
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Unable to open the create-offer form", exception);
            showDashboardError("offer-form", "Non è stato possibile aprire il modulo dell'offerta.");
        }
    }

    private void refreshOrganizerDashboard() {
        if (userSession != null) {
            loadTrips(userSession);
            loadActivities(userSession);
            loadSubscribers(userSession);
            loadPaidRevenue(userSession);
        }
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

        public SimpleStringProperty participantProperty() {
            return participant;
        }

        public SimpleStringProperty emailProperty() {
            return email;
        }

        public SimpleStringProperty activityProperty() {
            return activity;
        }

        public SimpleStringProperty dateProperty() {
            return date;
        }
    }

    @FXML
    public void initialize() {
        offerStatusFilter.getItems().setAll("Tutti gli stati", "Pubblicata", "Bozza", "Annullata", "Conclusa");
        offerStatusFilter.getSelectionModel().selectFirst();
        offerSearchField.textProperty().addListener((observable, before, after) -> refreshFilteredOffers());
        offerStatusFilter.valueProperty().addListener((observable, before, after) -> refreshFilteredOffers());
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
                    Type listType = new TypeToken<List<TripDTO>>() {
                    }.getType();
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
                null);
    }

    private void loadActivities(UserSession session) {
        ApiService.get(
                ApiService.BASE_URL + "/api/activities/organizer/mine",
                response -> {
                    Type listType = new TypeToken<List<ActivityDTO>>() {
                    }.getType();
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
                        showDashboardError("activities",
                                "Non è stato possibile leggere le attività ricevute dal server.");
                    }
                },
                () -> showDashboardError("activities",
                        "Impossibile caricare le attività. Verifica la connessione e riprova."),
                null);
    }

    private void loadSubscribers(UserSession session) {
        ApiService.get(
                ApiService.BASE_URL + "/api/bookings/organizer/mine",
                response -> {
                    Type listType = new TypeToken<List<BookingDTO>>() {
                    }.getType();
                    try {
                        List<BookingDTO> bookings = ApiService.fromJson(response.body(), listType);
                        Platform.runLater(() -> {
                            clearDashboardError("bookings");
                            updateSubscribers(bookings);
                        });
                    } catch (RuntimeException exception) {
                        LOGGER.log(Level.WARNING, "Unable to deserialize organizer bookings response", exception);
                        showDashboardError("bookings",
                                "Non è stato possibile leggere le prenotazioni ricevute dal server.");
                    }
                },
                () -> showDashboardError("bookings",
                        "Impossibile caricare le prenotazioni. Verifica la connessione e riprova."),
                null);
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
                        showDashboardError("revenue",
                                "Non è stato possibile leggere il totale dei pagamenti dal server.");
                    }
                },
                () -> showDashboardError("revenue", "Impossibile caricare il totale dei pagamenti."),
                null);
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

    private void refreshFilteredOffers() {
        renderActivityCards();
        renderTripCards();
    }

    private boolean matchesOffer(String title, String city, String status) {
        String query = offerSearchField.getText() == null ? ""
                : offerSearchField.getText().trim().toLowerCase(Locale.ROOT);
        String selected = offerStatusFilter.getValue();
        String expected = selected == null ? "" : switch (selected) {
            case "Pubblicata" -> "PUBLISHED";
            case "Bozza" -> "DRAFT";
            case "Annullata" -> "CANCELLED";
            case "Conclusa" -> "COMPLETED";
            default -> "";
        };
        return (expected.isEmpty() || expected.equalsIgnoreCase(status))
                && (query.isEmpty() || (title != null && title.toLowerCase(Locale.ROOT).contains(query))
                        || (city != null && city.toLowerCase(Locale.ROOT).contains(query)));
    }

    private void renderActivityCards() {
        activityCardsContainer.getChildren().clear();
        List<ActivityDTO> safeActivities = activities == null ? List.of()
                : activities.stream()
                        .filter(activity -> matchesOffer(activity.getTitle(), activity.getCity(), activity.getStatus()))
                        .toList();
        activitiesPlaceholderLabel.setText(activities == null || activities.isEmpty()
                ? "Non hai ancora creato attività."
                : "Nessuna attività corrisponde ai filtri.");
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
                        activity.getEndDate() == null ? null : activity.getEndDate().toString()));
                controller.setEditAction(() -> openOfferDialog(rootStackPane, null, activity));
                DashboardOffer activityOffer = new DashboardOffer(
                        "ACTIVITY", activity.getActivityId(), activity.getTitle(), activity.getStatus(),
                        activity.getMaxSeats(), activity.getAvailableSeats(),
                        activity.getPrice() == null ? null : BigDecimal.valueOf(activity.getPrice()),
                        null, activity, activity.getCity(), "");
                controller.setDeleteAction(() -> handleDeleteOffer(activityOffer));
                activityCardsContainer.getChildren().add(card);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to render organizer activity card", exception);
                showDashboardError("activity-cards", "Non è stato possibile visualizzare una delle tue attività.");
                return;
            }
        }
        if (!safeActivities.isEmpty())
            clearDashboardError("activity-cards");
    }

    private void renderTripCards() {
        tripCardsContainer.getChildren().clear();
        List<TripDTO> safeTrips = trips == null ? List.of()
                : trips.stream()
                        .filter(trip -> matchesOffer(trip.getTitle(), trip.getDestinationCity(), trip.getStatus()))
                        .toList();
        tripsPlaceholderLabel.setText(trips == null || trips.isEmpty()
                ? "Non hai ancora creato viaggi."
                : "Nessun viaggio corrisponde ai filtri.");
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
                        trip.getEndDate() == null ? null : trip.getEndDate().toString()));
                controller.setEditAction(() -> openOfferDialog(rootStackPane, trip, null));
                DashboardOffer tripOffer = new DashboardOffer(
                        "TRIP", trip.getTripId(), trip.getTitle(), trip.getStatus(),
                        trip.getMaxSeats(), trip.getAvailableSeats(), trip.getTotalPrice(),
                        trip, null, trip.getDestinationCity(), formatTripDates(trip));
                controller.setDeleteAction(() -> handleDeleteOffer(tripOffer));
                tripCardsContainer.getChildren().add(card);
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Unable to render organizer trip card", exception);
                showDashboardError("trip-cards", "Non è stato possibile visualizzare uno dei tuoi viaggi.");
                return;
            }
        }
        if (!safeTrips.isEmpty())
            clearDashboardError("trip-cards");
    }

    private String formatTripDates(TripDTO trip) {
        LocalDate start = trip.getStartDate();
        LocalDate end = trip.getEndDate();
        if (start == null)
            return "";
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
                        booking.getCreatedAt() == null ? ""
                                : booking.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .toList();
        subscribersTable.setItems(FXCollections.observableArrayList(rows));
        int subscribers = safeBookings.stream()
                .filter(booking -> !"CANCELLED".equalsIgnoreCase(booking.getBookingStatus()))
                .map(BookingDTO::getSeats)
                .filter(Objects::nonNull)
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
        if (dashboardErrorLabel == null)
            return;
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
                        .add(45));
    }

    private void handleDeleteOffer(DashboardOffer offer) {
        if (offer == null || offer.id() == null)
            return;
        PopupFactory.showConfirmation(rootStackPane,
                offer.type().equals("TRIP") ? "Cancella viaggio" : "Cancella attività",
                "Vuoi davvero cancellare “" + offer.title() + "”? L'operazione potrebbe non essere reversibile.",
                PopupFactory.Size.COMPACT,
                () -> ApiService.delete(
                        ApiService.BASE_URL + "/api/" + (offer.type().equals("TRIP") ? "trips/" : "activities/")
                                + offer.id(),
                        response -> {
                            clearDashboardError("delete");
                            refreshOrganizerDashboard();
                        },
                        () -> showDashboardError("delete",
                                "Impossibile cancellare l'offerta. Verifica i permessi e riprova."),
                        null));
    }
}
