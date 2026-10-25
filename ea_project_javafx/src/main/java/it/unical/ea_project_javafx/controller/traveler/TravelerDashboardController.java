package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.home.ExperienceCardController;
import it.unical.ea_project_javafx.controller.profile.ProfileDashboardController;
import it.unical.ea_project_javafx.dto.BookingDTO;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.PopupFactory;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Traveler dashboard: data-backed discovery, booking summary and the same operational booking table
 * available in the former dedicated bookings page.
 */
public class TravelerDashboardController implements ProfileDashboardController {
    @FXML private VBox dashboardRoot;
    @FXML private HBox recommendedContainer;
    @FXML private HBox upcomingExperienceContainer;
    @FXML private Label welcomeDateLabel;
    @FXML private Label welcomeTitleLabel;
    @FXML private Label welcomeSubtitleLabel;
    @FXML private javafx.scene.control.ScrollPane recommendedScroll;
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private ComboBox<String> searchTypeCombo;
    @FXML private Label bookingValue;
    @FXML private Label bookingDetail;
    @FXML private Label upcomingBookingValue;
    @FXML private Label historicalBookingValue;
    @FXML private Label favoriteListsValue;
    @FXML private Label dashboardStatusLabel;
    @FXML private TextField bookingsSearchField;
    @FXML private ComboBox<String> bookingsPeriodFilter;
    @FXML private ComboBox<String> bookingsStatusFilter;
    @FXML private TableView<BookingDTO> bookingsTable;
    @FXML private TableColumn<BookingDTO, String> itemTitleColumn;
    @FXML private TableColumn<BookingDTO, String> dateColumn;
    @FXML private TableColumn<BookingDTO, String> bookedAtColumn;
    @FXML private TableColumn<BookingDTO, String> locationColumn;
    @FXML private TableColumn<BookingDTO, String> seatsColumn;
    @FXML private TableColumn<BookingDTO, String> priceColumn;
    @FXML private TableColumn<BookingDTO, String> statusColumn;
    @FXML private TableColumn<BookingDTO, Void> actionColumn;

    private final List<BookingDTO> allBookings = new ArrayList<>();
    private LocalDateTime evaluationTime = LocalDateTime.now();
    private PopupFactory.Handle activeModal;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALY);

    @FXML
    public void initialize() {
        searchTypeCombo.setItems(FXCollections.observableArrayList("Attività", "Viaggi"));
        searchTypeCombo.getSelectionModel().selectFirst();
        setupBookingFilters();
        setupBookingTable();
        PauseTransition searchDebounce = new PauseTransition(Duration.millis(280));
        searchDebounce.setOnFinished(event -> handleSearch());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchField.isVisible()) searchDebounce.playFromStart();
        });
        searchTypeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (searchField.isVisible() && searchField.getText() != null && !searchField.getText().isBlank()) {
                searchDebounce.playFromStart();
            }
        });
    }

    @Override
    public void setUser(UserSession userSession) {
        renderWelcome(userSession == null ? UserSession.getInstance() : userSession);
        loadRecommendations();
        loadBookings();
        loadFavoriteListCount();
    }

    private void renderWelcome(UserSession session) {
        String name = session == null ? null : session.getFullName();
        if (name == null || name.isBlank()) name = session == null ? null : session.getUsername();
        if (name == null || name.isBlank()) name = "di nuovo";
        else name = name.trim().split("\\s+")[0];

        setLabelText(welcomeTitleLabel, "welcomeTitleLabel", "È bello rivederti, " + name + "!");
        setLabelText(welcomeDateLabel, "welcomeDateLabel", LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN)).toUpperCase(Locale.ITALIAN));
        setLabelText(welcomeSubtitleLabel, "welcomeSubtitleLabel", "Il prossimo ricordo potrebbe cominciare da qui.");

        if (resolveLabel(welcomeTitleLabel, "welcomeTitleLabel") == null && !missingWelcomeBindingsReported) {
            missingWelcomeBindingsReported = true;
            System.err.println("[Dashboard] Il FXML caricato non contiene welcomeTitleLabel. " +
                    "Le risorse compilate sono probabilmente obsolete: eseguire una build clean e riavviare l'applicazione.");
        }
    }

    private boolean missingWelcomeBindingsReported;

    /** Evita che un FXML rimasto obsoleto in target/classes faccia fallire tutta la navigazione. */
    private void setLabelText(Label injectedLabel, String fxId, String value) {
        Label target = resolveLabel(injectedLabel, fxId);
        if (target != null) target.setText(value);
    }

    private Label resolveLabel(Label injectedLabel, String fxId) {
        if (injectedLabel != null) return injectedLabel;
        if (dashboardRoot != null) {
            Node candidate = dashboardRoot.lookup("#" + fxId);
            if (candidate instanceof Label label) return label;
        }
        return null;
    }

    private void loadRecommendations() {
        ApiService.get(ApiService.BASE_URL + "/api/activities/top/cards?page=0&size=6",
                response -> {
                    Type type = new TypeToken<List<ActivityHomeDTO>>() {}.getType();
                    List<ActivityHomeDTO> activities = ApiService.fromJson(response.body(), type);
                    Platform.runLater(() -> renderActivityCards(activities));
                },
                () -> Platform.runLater(() -> {
                    recommendedContainer.getChildren().clear();
                    dashboardStatus("Non è stato possibile caricare le proposte. Puoi comunque usare la ricerca.", true);
                }), null);
    }

    @FXML private void toggleSearch() {
        boolean show = !searchField.isVisible();
        searchField.setVisible(show);
        searchField.setManaged(show);
        searchTypeCombo.setVisible(show);
        searchTypeCombo.setManaged(show);
        if (show) searchField.requestFocus();
        else {
            searchField.clear();
            loadRecommendations();
        }
    }

    @FXML private void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isBlank()) { loadRecommendations(); return; }
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        boolean trips = "Viaggi".equals(searchTypeCombo.getValue());
        String endpoint = trips ? "/api/trips/search" : "/api/activities/search";
        ApiService.get(ApiService.BASE_URL + endpoint + "?query=" + encodedQuery + "&page=0&size=9",
                response -> {
                    if (trips) {
                        Type type = new TypeToken<List<TripHomeDTO>>() {}.getType();
                        List<TripHomeDTO> result = ApiService.fromJson(response.body(), type);
                        Platform.runLater(() -> renderTripCards(result));
                    } else {
                        Type type = new TypeToken<List<ActivityHomeDTO>>() {}.getType();
                        List<ActivityHomeDTO> result = ApiService.fromJson(response.body(), type);
                        Platform.runLater(() -> renderActivityCards(result));
                    }
                },
                () -> Platform.runLater(() -> {
                    recommendedContainer.getChildren().clear();
                    dashboardStatus("Ricerca non riuscita. Verifica la connessione e riprova.", true);
                }), null);
    }

    private void renderActivityCards(List<ActivityHomeDTO> activities) {
        if (recommendedContainer == null) return;
        recommendedContainer.getChildren().clear();
        if (activities == null || activities.isEmpty()) {
            Label empty = new Label("Nessuna attività da mostrare al momento.");
            empty.getStyleClass().add("dashboard-empty-label");
            recommendedContainer.getChildren().add(empty);
            return;
        }
        activities.stream().filter(java.util.Objects::nonNull).forEach(item -> addCard(item, false));
    }

    private void renderTripCards(List<TripHomeDTO> trips) {
        if (recommendedContainer == null) return;
        recommendedContainer.getChildren().clear();
        if (trips == null || trips.isEmpty()) {
            Label empty = new Label("Nessun viaggio corrisponde alla ricerca.");
            empty.getStyleClass().add("dashboard-empty-label");
            recommendedContainer.getChildren().add(empty);
            return;
        }
        trips.stream().filter(java.util.Objects::nonNull).forEach(item -> addCard(item, true));
    }

    private void addCard(Object item, boolean trip) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/home/experience-card.fxml"));
            VBox card = loader.load();
            ExperienceCardController controller = loader.getController();
            if (trip) controller.setData((TripHomeDTO) item);
            else controller.setData((ActivityHomeDTO) item);
            recommendedContainer.getChildren().add(card);
        } catch (IOException exception) {
            dashboardStatus("Una proposta non è stata visualizzata correttamente.", true);
        }
    }

    @FXML private void handlePreviousRecommendation() {
        recommendedScroll.setHvalue(Math.max(0.0, recommendedScroll.getHvalue() - 0.35));
    }
    @FXML private void handleNextRecommendation() {
        recommendedScroll.setHvalue(Math.min(1.0, recommendedScroll.getHvalue() + 0.35));
    }

    @FXML private void handleOpenHistory() {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/booking-history.fxml", null);
    }
    @FXML private void handleOpenFavorites() {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/favorites.fxml", null);
    }

    @FXML private void handleShowAllBookings() {
        bookingsPeriodFilter.getSelectionModel().select("Tutte");
        bookingsStatusFilter.getSelectionModel().select("Tutti");
        bookingsSearchField.clear();
        filterBookings();
        if (!bookingsTable.getItems().isEmpty()) bookingsTable.scrollTo(0);
        dashboardStatus("Tutte le prenotazioni sono mostrate nella tabella qui sotto.", false);
    }

    private void setupBookingFilters() {
        bookingsPeriodFilter.getItems().setAll("Tutte", "In programma", "Storico");
        bookingsPeriodFilter.getSelectionModel().selectFirst();
        bookingsStatusFilter.getItems().setAll("Tutti", "In attesa", "Confermate", "Completate / terminate", "Cancellate");
        bookingsStatusFilter.getSelectionModel().selectFirst();
        bookingsSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterBookings());
        bookingsPeriodFilter.valueProperty().addListener((obs, oldValue, newValue) -> filterBookings());
        bookingsStatusFilter.valueProperty().addListener((obs, oldValue, newValue) -> filterBookings());
    }

    private void setupBookingTable() {
        itemTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getItemTitle(), "Esperienza non disponibile")));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatEventPeriod(cell.getValue())));
        bookedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatBookedAt(cell.getValue())));
        locationColumn.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getItemLocation(), "—")));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeats() == null ? "—" : String.valueOf(cell.getValue().getSeats())));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getPrice())));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(displayStatus(cell.getValue())));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null); setGraphic(null);
                if (empty || status == null || status.isBlank()) return;
                badge.setText(status);
                badge.getStyleClass().setAll("label", "booking-status-pill", statusStyleClass(status));
                setGraphic(badge);
            }
        });
        bookingsTable.setPlaceholder(new Label("Non hai ancora effettuato prenotazioni."));
        bookingsTable.setRowFactory(table -> new TableRow<>() {
            @Override protected void updateItem(BookingDTO booking, boolean empty) {
                super.updateItem(booking, empty);
                getStyleClass().removeAll("cancelled-booking-row", "historical-booking-row");
                if (empty || booking == null) return;
                if (isCancelled(booking)) getStyleClass().add("cancelled-booking-row");
                else if (isHistorical(booking)) getStyleClass().add("historical-booking-row");
            }
        });
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button export = new Button("📅 .ics");
            private final Button cancel = new Button("Cancella");
            private final HBox box = new HBox(6, export, cancel);
            {
                export.getStyleClass().add("btn-secondary");
                cancel.getStyleClass().add("danger-action-button");
                export.setOnAction(event -> exportIcs(currentBooking()));
                cancel.setOnAction(event -> requestCancel(currentBooking()));
            }
            @Override protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setGraphic(null); return; }
                BookingDTO booking = currentBooking();
                if (booking == null) { setGraphic(null); return; }
                boolean cancelled = isCancelled(booking);
                boolean canCancel = !cancelled && !isHistorical(booking) && !hasEventStarted(booking)
                        && !"COMPLETED".equalsIgnoreCase(booking.getBookingStatus());
                export.setVisible(!cancelled); export.setManaged(!cancelled);
                cancel.setVisible(canCancel); cancel.setManaged(canCancel);
                setGraphic(box);
            }
            private BookingDTO currentBooking() {
                int index = getIndex();
                return index < 0 || index >= getTableView().getItems().size() ? null : getTableView().getItems().get(index);
            }
        });
    }

    private void loadBookings() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/mine", response -> {
            try {
                Type type = new TypeToken<List<BookingDTO>>() {}.getType();
                List<BookingDTO> result = ApiService.fromJson(response.body(), type);
                Platform.runLater(() -> {
                    allBookings.clear();
                    if (result != null) allBookings.addAll(result);
                    evaluationTime = LocalDateTime.now();
                    updateBookingCounters();
                    filterBookings();
                    dashboardStatus(null, false);
                });
            } catch (Exception exception) {
                Platform.runLater(() -> dashboardStatus("Il server ha restituito un elenco di prenotazioni non valido.", true));
            }
        }, () -> Platform.runLater(() -> dashboardStatus("Non è stato possibile caricare le prenotazioni. Verifica il backend.", true)), null);
    }

    private void loadFavoriteListCount() {
        ApiService.get(ApiService.BASE_URL + "/api/favorite-lists/mine", response -> {
            try {
                JsonArray lists = JsonParser.parseString(response.body()).getAsJsonArray();
                Platform.runLater(() -> {
                    String count = String.valueOf(lists.size());
                    setLabelText(favoriteListsValue, "favoriteListsValue", count);
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    setLabelText(favoriteListsValue, "favoriteListsValue", "—");
                });
            }
        }, () -> Platform.runLater(() -> {
            setLabelText(favoriteListsValue, "favoriteListsValue", "—");
        }), null);
    }

    private void updateBookingCounters() {
        evaluationTime = LocalDateTime.now();
        setLabelText(bookingValue, "bookingValue", String.valueOf(allBookings.size()));
        setLabelText(bookingDetail, "bookingDetail", allBookings.size() == 1 ? "prenotazione totale" : "prenotazioni totali");
        long upcoming = allBookings.stream().filter(b -> !isCancelled(b) && !isHistorical(b))
                .filter(b -> {
                    LocalDateTime start = eventStartDateTime(b);
                    return start != null && start.isAfter(evaluationTime);
                }).count();
        setLabelText(upcomingBookingValue, "upcomingBookingValue", String.valueOf(upcoming));
        setLabelText(historicalBookingValue, "historicalBookingValue", String.valueOf(allBookings.stream().filter(this::isHistorical).count()));
        renderUpcomingExperiences();
    }

    private void renderUpcomingExperiences() {
        // Un FXML ancora presente in target/classes potrebbe non avere questa sezione nuova.
        // La schermata deve rimanere navigabile e la build clean aggiornerà la risorsa.
        if (upcomingExperienceContainer == null) return;
        upcomingExperienceContainer.getChildren().clear();
        List<BookingDTO> upcoming = allBookings.stream()
                .filter(java.util.Objects::nonNull)
                .filter(b -> b.getBookingId() != null && !isCancelled(b) && !isHistorical(b))
                .filter(b -> {
                    LocalDateTime start = eventStartDateTime(b);
                    return start != null && start.isAfter(evaluationTime);
                })
                .sorted((a, b) -> {
                    LocalDateTime aStart = eventStartDateTime(a), bStart = eventStartDateTime(b);
                    if (aStart == null && bStart == null) return 0;
                    if (aStart == null) return 1;
                    if (bStart == null) return -1;
                    return aStart.compareTo(bStart);
                }).limit(3).toList();
        if (upcoming.isEmpty()) {
            VBox empty = new VBox(5);
            empty.getStyleClass().add("dashboard-upcoming-empty");
            Label title = new Label("Nessuna partenza in vista");
            title.getStyleClass().add("dashboard-upcoming-empty-title");
            Label subtitle = new Label("Le tue prossime esperienze appariranno qui.");
            subtitle.getStyleClass().add("dashboard-upcoming-empty-subtitle");
            empty.getChildren().addAll(title, subtitle);
            HBox.setHgrow(empty, Priority.ALWAYS);
            upcomingExperienceContainer.getChildren().add(empty);
            return;
        }
        for (BookingDTO booking : upcoming) {
            VBox card = new VBox(9);
            card.getStyleClass().add("dashboard-upcoming-card");
            Label type = new Label(isTrip(booking) ? "VIAGGIO" : "ATTIVITÀ");
            type.getStyleClass().add("dashboard-upcoming-type");
            Label title = new Label(safe(booking.getItemTitle(), "Esperienza"));
            title.getStyleClass().add("dashboard-upcoming-title");
            title.setWrapText(true);
            Label date = new Label(formatEventPeriod(booking));
            date.getStyleClass().add("dashboard-upcoming-date");
            Label location = new Label(safe(booking.getItemLocation(), "Luogo da confermare"));
            location.getStyleClass().add("dashboard-upcoming-location");
            location.setWrapText(true);
            Region spacer = new Region();
            VBox.setVgrow(spacer, Priority.ALWAYS);
            Button open = new Button("Vai alla prenotazione →");
            open.getStyleClass().add("dashboard-upcoming-action");
            open.setMaxWidth(Double.MAX_VALUE);
            open.setOnAction(event -> openBookingFromPreview(booking));
            card.getChildren().addAll(type, title, date, location, spacer, open);
            HBox.setHgrow(card, Priority.ALWAYS);
            upcomingExperienceContainer.getChildren().add(card);
        }
    }

    private void openBookingFromPreview(BookingDTO booking) {
        if (booking == null || booking.getBookingId() == null) return;
        bookingsPeriodFilter.getSelectionModel().select("Tutte");
        bookingsStatusFilter.getSelectionModel().select("Tutti");
        bookingsSearchField.setText(String.valueOf(booking.getBookingId()));
        filterBookings();
        bookingsTable.getSelectionModel().selectFirst();
        bookingsTable.scrollTo(0);
        dashboardStatus("Prenotazione #" + booking.getBookingId() + " selezionata nella tabella.", false);
    }

    private void filterBookings() {
        evaluationTime = LocalDateTime.now();
        String query = bookingsSearchField.getText() == null ? "" : bookingsSearchField.getText().trim().toLowerCase(Locale.ITALY);
        String selectedPeriod = bookingsPeriodFilter.getValue();
        String selectedStatus = bookingsStatusFilter.getValue();
        List<BookingDTO> filtered = allBookings.stream()
                .filter(b -> matchesQuery(b, query))
                .filter(b -> matchesPeriod(b, selectedPeriod))
                .filter(b -> matchesStatus(b, selectedStatus))
                .sorted(this::compareBookings)
                .toList();
        bookingsTable.setItems(FXCollections.observableArrayList(filtered));
        bookingsTable.setPlaceholder(new Label(allBookings.isEmpty()
                ? "Non hai ancora effettuato prenotazioni. Quando ne farai una, apparirà qui."
                : "Nessuna prenotazione corrisponde ai filtri selezionati."));
    }

    private boolean matchesQuery(BookingDTO b, String query) {
        return query.isBlank() || contains(b.getItemTitle(), query) || contains(b.getItemLocation(), query)
                || contains(formatEventPeriod(b), query) || contains(b.getBookingType(), query)
                || contains(String.valueOf(b.getBookingId()), query) || contains(formatBookedAt(b), query);
    }
    private boolean matchesPeriod(BookingDTO b, String period) {
        if (period == null || "Tutte".equalsIgnoreCase(period)) return true;
        if ("Storico".equalsIgnoreCase(period)) return isHistorical(b);
        if ("In programma".equalsIgnoreCase(period)) return !isCancelled(b) && !isHistorical(b);
        return true;
    }
    private boolean matchesStatus(BookingDTO b, String selected) {
        if (selected == null || "Tutti".equalsIgnoreCase(selected)) return true;
        String status = safe(b.getBookingStatus(), "").toUpperCase(Locale.ROOT);
        return switch (selected) {
            case "In attesa" -> "PENDING".equals(status) && !isCancelled(b) && !isHistorical(b);
            case "Confermate" -> "CONFIRMED".equals(status) && !isCancelled(b) && !isHistorical(b);
            case "Completate / terminate" -> !isCancelled(b) && ("COMPLETED".equals(status)
                    || "COMPLETED".equalsIgnoreCase(b.getItemStatus()) || isEventEnded(b));
            case "Cancellate" -> isCancelled(b);
            default -> true;
        };
    }
    private int compareBookings(BookingDTO a, BookingDTO b) {
        boolean ac = isCancelled(a), bc = isCancelled(b);
        if (ac != bc) return ac ? 1 : -1;
        boolean ah = isHistorical(a), bh = isHistorical(b);
        if (ah != bh) return ah ? 1 : -1;
        LocalDateTime ad = ah ? eventEndDateTime(a) : eventStartDateTime(a);
        LocalDateTime bd = bh ? eventEndDateTime(b) : eventStartDateTime(b);
        if (ad != null && bd != null) {
            int compare = ah ? bd.compareTo(ad) : ad.compareTo(bd);
            if (compare != 0) return compare;
        } else if (ad != null) return -1;
        else if (bd != null) return 1;
        LocalDateTime acd = a.getCreatedAt(), bcd = b.getCreatedAt();
        if (acd != null && bcd != null) return bcd.compareTo(acd);
        if (acd != null) return -1;
        if (bcd != null) return 1;
        return Long.compare(b.getBookingId() == null ? 0 : b.getBookingId(), a.getBookingId() == null ? 0 : a.getBookingId());
    }

    private boolean isHistorical(BookingDTO b) {
        return b != null && (isCancelled(b) || "COMPLETED".equalsIgnoreCase(b.getBookingStatus())
                || "COMPLETED".equalsIgnoreCase(b.getItemStatus()) || isEventEnded(b));
    }
    private boolean isEventEnded(BookingDTO b) { LocalDateTime end = eventEndDateTime(b); return end != null && !end.isAfter(evaluationTime); }
    private boolean hasEventStarted(BookingDTO b) { LocalDateTime start = eventStartDateTime(b); return start != null && !start.isAfter(evaluationTime); }
    private boolean isCancelled(BookingDTO b) { return b != null && ("CANCELLED".equalsIgnoreCase(b.getBookingStatus()) || "CANCELLED".equalsIgnoreCase(b.getItemStatus())); }
    private boolean isTrip(BookingDTO b) { return b != null && "TRIP".equalsIgnoreCase(b.getBookingType()); }
    private LocalDateTime eventStartDateTime(BookingDTO b) { return parseEventDate(b == null ? null : b.getItemDate(), b, false); }
    private LocalDateTime eventEndDateTime(BookingDTO b) {
        if (b == null) return null;
        LocalDateTime end = parseEventDate(b.getItemEndDate(), b, true);
        if (end != null) return end;
        LocalDateTime start = eventStartDateTime(b);
        return start == null ? null : isTrip(b) ? start.toLocalDate().atTime(LocalTime.MAX) : start.plusHours(1);
    }
    private LocalDateTime parseEventDate(String value, BookingDTO b, boolean end) {
        if (value == null || value.isBlank()) return null;
        try {
            if (isTrip(b) || !value.contains("T")) return LocalDate.parse(value).atTime(end ? LocalTime.MAX : LocalTime.MIN);
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) { return null; }
    }
    private String formatEventPeriod(BookingDTO b) {
        LocalDateTime start = eventStartDateTime(b), end = eventEndDateTime(b);
        if (start == null) return "Data non disponibile";
        if (isTrip(b)) return end == null || end.toLocalDate().equals(start.toLocalDate())
                ? start.toLocalDate().format(DATE_FORMAT) : start.toLocalDate().format(DATE_FORMAT) + " – " + end.toLocalDate().format(DATE_FORMAT);
        String formatted = start.format(DATE_TIME_FORMAT);
        if (end == null || end.equals(start)) return formatted;
        return end.toLocalDate().equals(start.toLocalDate()) ? formatted + " – " + end.format(TIME_FORMAT)
                : formatted + " – " + end.format(DATE_TIME_FORMAT);
    }
    private String formatBookedAt(BookingDTO b) { return b == null || b.getCreatedAt() == null ? "—" : b.getCreatedAt().format(DATE_TIME_FORMAT); }
    private String displayStatus(BookingDTO b) {
        if (b == null) return "—";
        String status = safe(b.getBookingStatus(), "").toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status)) return "Prenotazione cancellata";
        if ("CANCELLED".equalsIgnoreCase(b.getItemStatus())) return "Esperienza annullata";
        if ("COMPLETED".equals(status) || "COMPLETED".equalsIgnoreCase(b.getItemStatus())) return "Completata";
        if (isEventEnded(b)) return "Terminata";
        if ("CONFIRMED".equals(status)) return "Confermata";
        if ("PENDING".equals(status)) return "In attesa";
        return status.isBlank() ? "Stato non disponibile" : status;
    }
    private String statusStyleClass(String status) {
        if (status.startsWith("Prenotazione cancellata") || status.startsWith("Esperienza annullata")) return "booking-status-cancelled";
        return switch (status) {
            case "Completata", "Terminata" -> "booking-status-completed";
            case "Confermata" -> "booking-status-confirmed";
            case "In attesa" -> "booking-status-pending";
            default -> "booking-status-neutral";
        };
    }
    private String formatPrice(BigDecimal amount) { return amount == null ? "€ 0,00" : "€ " + String.format(Locale.ITALY, "%,.2f", amount); }
    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ITALY).contains(query); }
    private String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private void requestCancel(BookingDTO booking) {
        if (booking == null || booking.getBookingId() == null || isCancelled(booking) || isHistorical(booking) || hasEventStarted(booking)) {
            dashboardStatus("Questa prenotazione non può più essere cancellata.", true);
            return;
        }
        StackPane root = sceneRoot();
        if (root == null) { dashboardStatus("Impossibile aprire il popup di conferma.", true); return; }
        VBox panel = new VBox(14);
        panel.setMaxWidth(470);
        Label title = new Label("Cancella prenotazione?"); title.getStyleClass().add("app-modal-title");
        Label message = new Label("Vuoi cancellare «" + safe(booking.getItemTitle(), "questa esperienza") + "»? L'operazione non può essere annullata e i posti torneranno disponibili.");
        message.setWrapText(true);
        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER_RIGHT);
        Button back = new Button("Mantieni prenotazione"); back.getStyleClass().add("btn-secondary");
        Button confirm = new Button("Cancella prenotazione"); confirm.getStyleClass().add("danger-action-button");
        back.setOnAction(e -> closeModal());
        confirm.setOnAction(e -> {
            confirm.setDisable(true);
            ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/" + booking.getBookingId(), null,
                    ApiService.Method.DELETE,
                    response -> Platform.runLater(() -> {
                        closeModal(); dashboardStatus("Prenotazione cancellata correttamente.", false); loadBookings();
                    }),
                    messageText -> Platform.runLater(() -> { closeModal(); dashboardStatus(messageText, true); loadBookings(); }), null);
        });
        actions.getChildren().addAll(back, confirm); panel.getChildren().addAll(title, message, actions);
        closeModal(); activeModal = PopupFactory.show(root, panel, PopupFactory.Size.COMPACT);
    }

    private void exportIcs(BookingDTO booking) {
        if (booking == null || booking.getBookingId() == null || isCancelled(booking)) {
            dashboardStatus("Questa prenotazione non può essere esportata.", true); return;
        }
        ApiService.get(ApiService.BASE_URL + "/api/bookings/" + booking.getBookingId() + "/export/ics", response -> Platform.runLater(() -> {
            FileChooser chooser = new FileChooser(); chooser.setTitle("Salva calendario della prenotazione");
            String name = safe(booking.getItemTitle(), "prenotazione").replaceAll("[\\\\/:*?\"<>|]", "_") + ".ics";
            chooser.setInitialFileName(name);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics"));
            Window owner = bookingsTable.getScene() == null ? null : bookingsTable.getScene().getWindow();
            File file = chooser.showSaveDialog(owner);
            if (file == null) return;
            try {
                Files.writeString(file.toPath(), response.body(), StandardCharsets.UTF_8);
                dashboardStatus("Calendario salvato in " + file.getAbsolutePath(), false);
            } catch (IOException ex) { dashboardStatus("Non è stato possibile salvare il file .ics.", true); }
        }), () -> Platform.runLater(() -> dashboardStatus("Esportazione non riuscita. Riprova tra poco.", true)), null);
    }

    private StackPane sceneRoot() {
        return bookingsTable.getScene() != null && bookingsTable.getScene().getRoot() instanceof StackPane root ? root : null;
    }
    private void closeModal() { if (activeModal != null) { activeModal.close(); activeModal = null; } }
    private void dashboardStatus(String text, boolean error) {
        if (dashboardStatusLabel == null) return;
        if (text == null || text.isBlank()) { dashboardStatusLabel.setText(""); dashboardStatusLabel.setVisible(false); dashboardStatusLabel.setManaged(false); return; }
        dashboardStatusLabel.setText(text);
        dashboardStatusLabel.getStyleClass().setAll("dashboard-status", error ? "dashboard-status-error" : "dashboard-status-success");
        dashboardStatusLabel.setVisible(true); dashboardStatusLabel.setManaged(true);
    }
}
