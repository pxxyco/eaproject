package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.home.ExperienceCardController;
import it.unical.ea_project_javafx.controller.profile.ProfileDashboardController;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.UiText;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import it.unical.ea_project_javafx.ui.scroll.ScrollSupport;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import static it.unical.ea_project_javafx.ui.BookingPresentation.bookingRow;
import static it.unical.ea_project_javafx.ui.BookingPresentation.compareBookings;
import static it.unical.ea_project_javafx.ui.BookingPresentation.displayStatus;
import static it.unical.ea_project_javafx.ui.BookingPresentation.eventStartDateTime;
import static it.unical.ea_project_javafx.ui.BookingPresentation.formatBookedAt;
import static it.unical.ea_project_javafx.ui.BookingPresentation.formatEventPeriod;
import static it.unical.ea_project_javafx.ui.BookingPresentation.hasEventStarted;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isCancelled;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isEventEnded;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isHistorical;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isTrip;
import static it.unical.ea_project_javafx.ui.BookingPresentation.statusCell;

/**
 * Traveler dashboard: data-backed discovery, booking summary and the same
 * operational booking table
 * available in the former dedicated bookings page.
 */
public class TravelerDashboardController implements ProfileDashboardController {
    @FXML
    private VBox dashboardRoot;
    @FXML
    private HBox recommendedContainer;
    @FXML
    private HBox upcomingExperienceContainer;
    @FXML
    private Label welcomeDateLabel;
    @FXML
    private Label welcomeTitleLabel;
    @FXML
    private Label welcomeSubtitleLabel;
    @FXML
    private ScrollPane recommendedScroll;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> searchTypeCombo;
    @FXML
    private Label bookingValue;
    @FXML
    private Label bookingDetail;
    @FXML
    private Label upcomingBookingValue;
    @FXML
    private Label historicalBookingValue;
    @FXML
    private Label favoriteListsValue;
    @FXML
    private Label dashboardStatusLabel;
    @FXML
    private Label bookingFeedbackLabel, discoveryTitle, discoverySubtitle;
    @FXML
    private TextField bookingsSearchField;
    @FXML
    private ComboBox<String> bookingsPeriodFilter;
    @FXML
    private ComboBox<String> bookingsStatusFilter;
    @FXML
    private TableView<BookingDTO> bookingsTable;
    @FXML
    private TableColumn<BookingDTO, String> itemTitleColumn;
    @FXML
    private TableColumn<BookingDTO, String> dateColumn;
    @FXML
    private TableColumn<BookingDTO, String> bookedAtColumn;
    @FXML
    private TableColumn<BookingDTO, String> locationColumn;
    @FXML
    private TableColumn<BookingDTO, String> seatsColumn;
    @FXML
    private TableColumn<BookingDTO, String> priceColumn;
    @FXML
    private TableColumn<BookingDTO, String> statusColumn;
    @FXML
    private TableColumn<BookingDTO, Void> actionColumn;

    @FXML
    private Button previousRecommendation, nextRecommendation;
    private long discoveryGeneration;
    private final List<Object> discoveryItems = new ArrayList<>();
    private int discoveryOffset;
    private int discoveryPageSize = 3;
    private final List<BookingDTO> allBookings = new ArrayList<>();
    private LocalDateTime evaluationTime = LocalDateTime.now();
    private PopupFactory.Handle activeModal;

    @FXML
    public void initialize() {
        searchTypeCombo.setItems(FXCollections.observableArrayList("Attività", "Viaggi"));
        searchTypeCombo.getSelectionModel().selectFirst();
        setupBookingFilters();
        setupBookingTable();
        PauseTransition resizeCards = new PauseTransition(Duration.millis(150));
        resizeCards.setOnFinished(e -> {
            if (!discoveryItems.isEmpty())
                renderDiscovery();
        });
        recommendedScroll.viewportBoundsProperty().addListener((o, a, b) -> {
            if (Math.abs(a.getWidth() - b.getWidth()) > 1)
                resizeCards.playFromStart();
        });
        PauseTransition searchDebounce = new PauseTransition(Duration.millis(280));
        searchDebounce.setOnFinished(event -> handleSearch());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchField.isVisible())
                searchDebounce.playFromStart();
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
        if (name == null || name.isBlank())
            name = session == null ? null : session.getUsername();
        if (name == null || name.isBlank())
            name = "di nuovo";
        else
            name = name.trim().split("\\s+")[0];

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

    /**
     * Evita che un FXML rimasto obsoleto in target/classes faccia fallire tutta la
     * navigazione.
     */
    private void setLabelText(Label injectedLabel, String fxId, String value) {
        Label target = resolveLabel(injectedLabel, fxId);
        if (target != null)
            target.setText(value);
    }

    private Label resolveLabel(Label injectedLabel, String fxId) {
        if (injectedLabel != null)
            return injectedLabel;
        if (dashboardRoot != null) {
            Node candidate = dashboardRoot.lookup("#" + fxId);
            if (candidate instanceof Label label)
                return label;
        }
        return null;
    }

    private void loadRecommendations() {
        discoveryTitle.setText("Scelte per te");
        discoverySubtitle.setText("Idee vicine ai tuoi interessi, ai tuoi preferiti e ai viaggi che hai scelto.");
        long request = ++discoveryGeneration;
        ApiService.callDetailed(ApiService.BASE_URL + "/api/recommendations/mine?limit=3", null, ApiService.Method.GET,
                response -> {
                    if (request != discoveryGeneration)
                        return;
                    discoveryItems.clear();
                    discoveryOffset = 0;
                    var result = JsonParser.parseString(response.body()).getAsJsonObject();
                    for (var pick : result.getAsJsonArray("trips"))
                        discoveryItems.add((TripHomeDTO) ApiService
                                .fromJson(pick.getAsJsonObject().get("item").toString(), TripHomeDTO.class));
                    for (var pick : result.getAsJsonArray("activities"))
                        discoveryItems.add((ActivityHomeDTO) ApiService
                                .fromJson(pick.getAsJsonObject().get("item").toString(), ActivityHomeDTO.class));
                    renderDiscovery();
                }, message -> {
                    if (request == discoveryGeneration) {
                        recommendedContainer.getChildren().setAll(new Label(message));
                        carousel(false);
                    }
                }, null);
    }

    private void carousel(boolean show) {
        if (previousRecommendation != null) {
            previousRecommendation.setVisible(show);
            previousRecommendation.setManaged(show);
        }
        if (nextRecommendation != null) {
            nextRecommendation.setVisible(show);
            nextRecommendation.setManaged(show);
        }
    }

    @FXML
    private void toggleSearch() {
        boolean show = !searchField.isVisible();
        searchField.setVisible(show);
        searchField.setManaged(show);
        searchTypeCombo.setVisible(show);
        searchTypeCombo.setManaged(show);
        if (show)
            searchField.requestFocus();
        else {
            searchField.clear();
            loadRecommendations();
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isBlank()) {
            loadRecommendations();
            return;
        }
        long request = ++discoveryGeneration;
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        boolean trips = "Viaggi".equals(searchTypeCombo.getValue());
        String endpoint = trips ? "/api/trips/search" : "/api/activities/search";
        ApiService.get(ApiService.BASE_URL + endpoint + "?query=" + encodedQuery + "&page=0&size=9",
                response -> {
                    if (request != discoveryGeneration)
                        return;
                    if (trips) {
                        Type type = new TypeToken<List<TripHomeDTO>>() {
                        }.getType();
                        List<TripHomeDTO> result = ApiService.fromJson(response.body(), type);
                        Platform.runLater(() -> renderTripCards(result));
                    } else {
                        Type type = new TypeToken<List<ActivityHomeDTO>>() {
                        }.getType();
                        List<ActivityHomeDTO> result = ApiService.fromJson(response.body(), type);
                        Platform.runLater(() -> renderActivityCards(result));
                    }
                },
                () -> Platform.runLater(() -> {
                    recommendedContainer.getChildren().clear();
                    dashboardStatus("Ricerca non riuscita. Verifica la connessione e riprova.", true);
                }), null);
    }

    @FXML
    private void handleCategory(ActionEvent event) {
        Button selectedCategory = (Button) event.getSource();
        String category = selectedCategory.getUserData().toString();
        if (category.equals("RECOMMENDED")) {
            loadRecommendations();
            return;
        }
        discoveryTitle.setText(selectedCategory.getText());
        discoverySubtitle.setText("Esplora le esperienze di questa categoria.");
        recommendedContainer.getChildren().setAll(new Label("Caricamento delle esperienze…"));
        carousel(false);
        boolean trips = category.equals("TRIP");
        long request = ++discoveryGeneration;
        String endpoint = trips ? "/api/trips/search?page=0&size=9"
                : "/api/activities/search?page=0&size=9&category=" + category;
        ApiService.get(ApiService.BASE_URL + endpoint, response -> {
            if (request != discoveryGeneration)
                return;
            if (trips) {
                Type type = new TypeToken<List<TripHomeDTO>>() {
                }.getType();
                renderTripCards(ApiService.fromJson(response.body(), type));
            } else {
                Type type = new TypeToken<List<ActivityHomeDTO>>() {
                }.getType();
                renderActivityCards(ApiService.fromJson(response.body(), type));
            }
        }, () -> {
            if (request == discoveryGeneration) {
                recommendedContainer.getChildren().setAll(new Label("Categoria non disponibile. Riprova tra poco."));
                carousel(false);
            }
        }, null);
    }

    private void renderActivityCards(List<ActivityHomeDTO> items) {
        setDiscovery(items);
    }

    private void renderTripCards(List<TripHomeDTO> items) {
        setDiscovery(items);
    }

    private void setDiscovery(List<?> items) {
        discoveryItems.clear();
        if (items != null)
            items.stream().filter(Objects::nonNull).forEach(discoveryItems::add);
        discoveryOffset = 0;
        renderDiscovery();
    }

    private void renderDiscovery() {
        double viewport = recommendedScroll.getViewportBounds().getWidth();
        discoveryPageSize = viewport <= 0 ? 3 : Math.max(1, Math.min(3, (int) ((viewport + 16) / 280)));
        discoveryOffset = Math.min(discoveryOffset,
                Math.max(0, ((discoveryItems.size() - 1) / discoveryPageSize) * discoveryPageSize));
        recommendedContainer.getChildren().clear();
        carousel(discoveryItems.size() > discoveryPageSize);
        if (discoveryItems.isEmpty()) {
            Label empty = new Label("Nessuna esperienza corrisponde alla ricerca. Prova un'altra destinazione.");
            empty.getStyleClass().add("dashboard-empty-label");
            recommendedContainer.getChildren().add(empty);
            return;
        }
        for (Object item : discoveryItems.subList(discoveryOffset,
                Math.min(discoveryOffset + discoveryPageSize, discoveryItems.size())))
            addCard(item, item instanceof TripHomeDTO);
        previousRecommendation.setDisable(discoveryOffset == 0);
        nextRecommendation.setDisable(discoveryOffset + discoveryPageSize >= discoveryItems.size());
    }

    private void addCard(Object item, boolean trip) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/home/experience-card.fxml"));
            VBox card = loader.load();
            ExperienceCardController controller = loader.getController();
            if (trip)
                controller.setData((TripHomeDTO) item);
            else
                controller.setData((ActivityHomeDTO) item);
            double available = recommendedScroll.getViewportBounds().getWidth();
            controller
                    .setCardWidth(
                            available > 0
                                    ? Math.max(240,
                                            Math.min(325,
                                                    (available - (discoveryPageSize - 1) * 16) / discoveryPageSize))
                                    : 300);
            recommendedContainer.getChildren().add(card);
        } catch (IOException exception) {
            dashboardStatus("Una proposta non è stata visualizzata correttamente.", true);
        }
    }

    @FXML
    private void handlePreviousRecommendation() {
        discoveryOffset = Math.max(0, discoveryOffset - discoveryPageSize);
        renderDiscovery();
    }

    @FXML
    private void handleNextRecommendation() {
        if (discoveryOffset + discoveryPageSize < discoveryItems.size()) {
            discoveryOffset += discoveryPageSize;
            renderDiscovery();
        }
    }

    @FXML
    private void handleOpenHistory() {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/booking-history.fxml", null);
    }

    private void setupBookingFilters() {
        bookingsPeriodFilter.getItems().setAll("Tutte", "In programma", "Storico");
        bookingsPeriodFilter.getSelectionModel().selectFirst();
        bookingsStatusFilter.getItems().setAll("Tutti", "In attesa", "Confermate", "Completate / terminate",
                "Cancellate");
        bookingsStatusFilter.getSelectionModel().selectFirst();
        bookingsSearchField.textProperty().addListener((obs, oldValue, newValue) -> filterBookings());
        bookingsPeriodFilter.valueProperty().addListener((obs, oldValue, newValue) -> filterBookings());
        bookingsStatusFilter.valueProperty().addListener((obs, oldValue, newValue) -> filterBookings());
    }

    private void setupBookingTable() {
        itemTitleColumn.setCellValueFactory(
                cell -> new SimpleStringProperty(
                        UiText.orDefault(cell.getValue().getItemTitle(), "Esperienza non disponibile")));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatEventPeriod(cell.getValue())));
        bookedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatBookedAt(cell.getValue())));
        locationColumn
                .setCellValueFactory(
                        cell -> new SimpleStringProperty(UiText.orDefault(cell.getValue().getItemLocation(), "—")));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSeats() == null ? "—" : String.valueOf(cell.getValue().getSeats())));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(UiText.money(cell.getValue().getPrice())));
        statusColumn
                .setCellValueFactory(cell -> new SimpleStringProperty(displayStatus(cell.getValue(), evaluationTime)));
        statusColumn.setCellFactory(column -> statusCell());
        bookingsTable.setPlaceholder(new Label("Non hai ancora effettuato prenotazioni."));
        bookingsTable.setRowFactory(table -> bookingRow(() -> evaluationTime));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button export = new Button("📅");
            private final Button cancel = new Button("Annulla");
            private final HBox box = new HBox(6, export, cancel);
            {
                export.setMinWidth(40);
                cancel.setMinWidth(68);
                export.setTooltip(new Tooltip("Esporta il calendario .ics"));
                export.getStyleClass().add("btn-secondary");
                cancel.getStyleClass().add("danger-action-button");
                export.setOnAction(event -> exportIcs(currentBooking()));
                cancel.setOnAction(event -> requestCancel(currentBooking()));
            }

            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                BookingDTO booking = currentBooking();
                if (booking == null) {
                    setGraphic(null);
                    return;
                }
                boolean cancelled = isCancelled(booking);
                boolean canCancel = !cancelled && !isHistorical(booking, evaluationTime)
                        && !hasEventStarted(booking, evaluationTime)
                        && !"COMPLETED".equalsIgnoreCase(booking.getBookingStatus());
                export.setVisible(!cancelled);
                export.setManaged(!cancelled);
                cancel.setVisible(canCancel);
                cancel.setManaged(canCancel);
                setGraphic(box);
            }

            private BookingDTO currentBooking() {
                int index = getIndex();
                return index < 0 || index >= getTableView().getItems().size() ? null
                        : getTableView().getItems().get(index);
            }
        });
    }

    private void loadBookings() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/mine", response -> {
            try {
                Type type = new TypeToken<List<BookingDTO>>() {
                }.getType();
                List<BookingDTO> result = ApiService.fromJson(response.body(), type);
                Platform.runLater(() -> {
                    allBookings.clear();
                    if (result != null)
                        allBookings.addAll(result);
                    evaluationTime = LocalDateTime.now();
                    updateBookingCounters();
                    filterBookings();
                    dashboardStatus(null, false);
                });
            } catch (Exception exception) {
                Platform.runLater(
                        () -> dashboardStatus("Il server ha restituito un elenco di prenotazioni non valido.", true));
            }
        }, () -> Platform.runLater(
                () -> dashboardStatus("Non è stato possibile caricare le prenotazioni. Verifica il backend.", true)),
                null);
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
        setLabelText(bookingDetail, "bookingDetail",
                allBookings.size() == 1 ? "prenotazione totale" : "prenotazioni totali");
        long upcoming = allBookings.stream().filter(b -> !isCancelled(b) && !isHistorical(b, evaluationTime))
                .filter(b -> {
                    LocalDateTime start = eventStartDateTime(b);
                    return start != null && start.isAfter(evaluationTime);
                }).count();
        setLabelText(upcomingBookingValue, "upcomingBookingValue", String.valueOf(upcoming));
        setLabelText(historicalBookingValue, "historicalBookingValue",
                String.valueOf(allBookings.stream().filter(booking -> isHistorical(booking, evaluationTime)).count()));
        renderUpcomingExperiences();
    }

    private void renderUpcomingExperiences() {
        // Un FXML ancora presente in target/classes potrebbe non avere questa sezione
        // nuova.
        // La schermata deve rimanere navigabile e la build clean aggiornerà la risorsa.
        if (upcomingExperienceContainer == null)
            return;
        upcomingExperienceContainer.getChildren().clear();
        List<BookingDTO> upcoming = allBookings.stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getBookingId() != null && !isCancelled(b) && !isHistorical(b, evaluationTime))
                .filter(b -> {
                    LocalDateTime start = eventStartDateTime(b);
                    return start != null && start.isAfter(evaluationTime);
                })
                .sorted((a, b) -> {
                    LocalDateTime aStart = eventStartDateTime(a), bStart = eventStartDateTime(b);
                    if (aStart == null && bStart == null)
                        return 0;
                    if (aStart == null)
                        return 1;
                    if (bStart == null)
                        return -1;
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
            Label title = new Label(UiText.orDefault(booking.getItemTitle(), "Esperienza"));
            title.getStyleClass().add("dashboard-upcoming-title");
            title.setWrapText(true);
            Label date = new Label(formatEventPeriod(booking));
            date.getStyleClass().add("dashboard-upcoming-date");
            Label location = new Label(UiText.orDefault(booking.getItemLocation(), "Luogo da confermare"));
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
        if (booking == null || booking.getBookingId() == null)
            return;
        bookingsPeriodFilter.getSelectionModel().select("Tutte");
        bookingsStatusFilter.getSelectionModel().select("Tutti");
        bookingsSearchField.setText(UiText.orDefault(booking.getItemTitle(), ""));
        filterBookings();
        bookingsTable.getSelectionModel().select(booking);
        bookingsTable.scrollTo(booking);
        bookingFeedback("Prenotazione selezionata: " + UiText.orDefault(booking.getItemTitle(), "Esperienza")
                + " · " + UiText.orDefault(booking.getItemLocation(), "") + " · " + formatEventPeriod(booking), false);
        ScrollSupport.reveal(bookingFeedbackLabel);
    }

    private void filterBookings() {
        evaluationTime = LocalDateTime.now();
        String query = bookingsSearchField.getText() == null ? ""
                : bookingsSearchField.getText().trim().toLowerCase(Locale.ITALY);
        String selectedPeriod = bookingsPeriodFilter.getValue();
        String selectedStatus = bookingsStatusFilter.getValue();
        List<BookingDTO> filtered = allBookings.stream()
                .filter(b -> matchesQuery(b, query))
                .filter(b -> matchesPeriod(b, selectedPeriod))
                .filter(b -> matchesStatus(b, selectedStatus))
                .sorted((left, right) -> compareBookings(left, right, evaluationTime))
                .toList();
        bookingsTable.setItems(FXCollections.observableArrayList(filtered));
        bookingsTable.setPrefHeight(Math.max(160, Math.min(360, 56 + filtered.size() * 66)));
        bookingsTable.setPlaceholder(new Label(allBookings.isEmpty()
                ? "Non hai ancora effettuato prenotazioni. Quando ne farai una, apparirà qui."
                : "Nessuna prenotazione corrisponde ai filtri selezionati."));
    }

    private boolean matchesQuery(BookingDTO b, String query) {
        return query.isBlank() || UiText.contains(b.getItemTitle(), query)
                || UiText.contains(b.getItemLocation(), query)
                || UiText.contains(formatEventPeriod(b), query) || UiText.contains(b.getBookingType(), query)
                || UiText.contains(formatBookedAt(b), query);
    }

    private boolean matchesPeriod(BookingDTO b, String period) {
        if (period == null || "Tutte".equalsIgnoreCase(period))
            return true;
        if ("Storico".equalsIgnoreCase(period))
            return isHistorical(b, evaluationTime);
        if ("In programma".equalsIgnoreCase(period))
            return !isCancelled(b) && !isHistorical(b, evaluationTime);
        return true;
    }

    private boolean matchesStatus(BookingDTO b, String selected) {
        if (selected == null || "Tutti".equalsIgnoreCase(selected))
            return true;
        String status = UiText.orDefault(b.getBookingStatus(), "").toUpperCase(Locale.ROOT);
        return switch (selected) {
            case "In attesa" -> "PENDING".equals(status) && !isCancelled(b) && !isHistorical(b, evaluationTime);
            case "Confermate" -> "CONFIRMED".equals(status) && !isCancelled(b) && !isHistorical(b, evaluationTime);
            case "Completate / terminate" -> !isCancelled(b) && ("COMPLETED".equals(status)
                    || "COMPLETED".equalsIgnoreCase(b.getItemStatus()) || isEventEnded(b, evaluationTime));
            case "Cancellate" -> isCancelled(b);
            default -> true;
        };
    }

    private void requestCancel(BookingDTO booking) {
        if (booking == null || booking.getBookingId() == null || isCancelled(booking)
                || isHistorical(booking, evaluationTime)
                || hasEventStarted(booking, evaluationTime)) {
            bookingFeedback("Questa prenotazione non può più essere cancellata.", true);
            return;
        }
        StackPane root = sceneRoot();
        if (root == null) {
            bookingFeedback("Impossibile aprire il popup di conferma.", true);
            return;
        }
        VBox panel = new VBox(14);
        panel.setMaxWidth(470);
        Label title = new Label("Cancella prenotazione?");
        title.getStyleClass().add("app-modal-title");
        Label message = new Label("Vuoi cancellare «" + UiText.orDefault(booking.getItemTitle(), "questa esperienza")
                + "»? L'operazione non può essere annullata e i posti torneranno disponibili.");
        message.setWrapText(true);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button back = new Button("Mantieni prenotazione");
        back.getStyleClass().add("btn-secondary");
        Button confirm = new Button("Cancella prenotazione");
        confirm.getStyleClass().add("danger-action-button");
        back.setOnAction(e -> closeModal());
        confirm.setOnAction(e -> {
            confirm.setDisable(true);
            ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/" + booking.getBookingId(), null,
                    ApiService.Method.DELETE,
                    response -> Platform.runLater(() -> {
                        closeModal();
                        bookingFeedback("Prenotazione cancellata correttamente.", false);
                        loadBookings();
                    }),
                    messageText -> Platform.runLater(() -> {
                        closeModal();
                        bookingFeedback(messageText, true);
                        loadBookings();
                    }), null);
        });
        actions.getChildren().addAll(back, confirm);
        panel.getChildren().addAll(title, message, actions);
        closeModal();
        activeModal = PopupFactory.show(root, panel, PopupFactory.Size.COMPACT);
    }

    private void exportIcs(BookingDTO booking) {
        if (booking == null || booking.getBookingId() == null || isCancelled(booking)) {
            bookingFeedback("Questa prenotazione non può essere esportata.", true);
            return;
        }
        ApiService.get(ApiService.BASE_URL + "/api/bookings/" + booking.getBookingId() + "/export/ics",
                response -> Platform.runLater(() -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Salva calendario della prenotazione");
                    String name = UiText.orDefault(booking.getItemTitle(), "prenotazione").replaceAll("[\\\\/:*?\"<>|]",
                            "_")
                            + ".ics";
                    chooser.setInitialFileName(name);
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCalendar (*.ics)", "*.ics"));
                    Window owner = bookingsTable.getScene() == null ? null : bookingsTable.getScene().getWindow();
                    File file = chooser.showSaveDialog(owner);
                    if (file == null)
                        return;
                    try {
                        Files.writeString(file.toPath(), response.body(), StandardCharsets.UTF_8);
                        bookingFeedback("Calendario salvato in " + file.getAbsolutePath(), false);
                    } catch (IOException ex) {
                        bookingFeedback("Non è stato possibile salvare il file .ics.", true);
                    }
                }),
                () -> Platform.runLater(() -> bookingFeedback("Esportazione non riuscita. Riprova tra poco.", true)),
                null);
    }

    private StackPane sceneRoot() {
        return bookingsTable.getScene() != null && bookingsTable.getScene().getRoot() instanceof StackPane root ? root
                : null;
    }

    private void closeModal() {
        if (activeModal != null) {
            activeModal.close();
            activeModal = null;
        }
    }

    private void bookingFeedback(String text, boolean error) {
        bookingFeedbackLabel.setText(text);
        bookingFeedbackLabel.getStyleClass().setAll("dashboard-status",
                error ? "dashboard-status-error" : "dashboard-status-success");
        bookingFeedbackLabel.setVisible(true);
        bookingFeedbackLabel.setManaged(true);
    }

    private void dashboardStatus(String text, boolean error) {
        if (dashboardStatusLabel == null)
            return;
        if (text == null || text.isBlank()) {
            dashboardStatusLabel.setText("");
            dashboardStatusLabel.setVisible(false);
            dashboardStatusLabel.setManaged(false);
            return;
        }
        dashboardStatusLabel.setText(text);
        dashboardStatusLabel.getStyleClass().setAll("dashboard-status",
                error ? "dashboard-status-error" : "dashboard-status-success");
        dashboardStatusLabel.setVisible(true);
        dashboardStatusLabel.setManaged(true);
    }
}
