package it.unical.ea_project_javafx.controller.organizer;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.activity.ActivityDetailsController;
import it.unical.ea_project_javafx.controller.profile.ProfileDashboardController;
import it.unical.ea_project_javafx.dto.activity.ActivityDTO;
import it.unical.ea_project_javafx.dto.trip.TripDTO;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.BookingPresentation;
import it.unical.ea_project_javafx.ui.UiText;
import static it.unical.ea_project_javafx.ui.TableSupport.textColumn;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import it.unical.ea_project_javafx.ui.control.PageNavigation;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import it.unical.ea_project_javafx.ui.chart.OrganizerEconomy;
import javafx.scene.layout.StackPane;

/**
 * Operational organizer workspace: the dashboard is bounded, the catalog is
 * paged and virtualized.
 */
public class OrganizerDashboardController implements ProfileDashboardController, AutoCloseable {
    @FXML
    private StackPane rootStackPane, navbarContainer;
    @FXML
    private ImageView bgImageView;
    @FXML
    private TextField offerSearchField;
    @FXML
    private ComboBox<String> offerStatusFilter, offerTypeFilter;
    @FXML
    private Label dashboardErrorLabel, publishedCountLabel, totalSubscribersLabel, totalRevenueLabel, draftCountLabel;
    @FXML
    private TableView<Offer> offersTable;
    @FXML
    private TableColumn<Offer, String> titleColumn, typeColumn, placeColumn, periodColumn, stateColumn, seatsColumn,
            priceColumn;
    @FXML
    private TableColumn<Offer, Void> actionsColumn;
    @FXML
    private PageNavigation pagination;
    @FXML
    private TableView<BookingDTO> subscribersTable;
    @FXML
    private TableColumn<BookingDTO, String> participantColumn, activityColumn, dateColumn;
    @FXML
    private VBox economyHost, draftActions;
    private OrganizerEconomy economy;
    private List<TripDTO> trips = List.of();
    private List<ActivityDTO> activities = List.of();
    private List<Offer> filtered = List.of();
    private boolean closed;
    private long generation;
    private PopupFactory.Handle offerPopup;

    private record Offer(String type, Long id, String title, String place, String status, LocalDateTime start,
            LocalDateTime end, Integer capacity, Integer available, BigDecimal price, TripDTO trip,
            ActivityDTO activity) {
    }

    protected boolean catalog() {
        return false;
    }

    @FXML
    public void initialize() {
        if (bgImageView != null) {
            BackgroundHelper.setupBackground(bgImageView);
            ViewSupport.loadNavbar(navbarContainer);
        }
        textColumn(titleColumn, Offer::title);
        textColumn(typeColumn, o -> o.type().equals("TRIP") ? "Viaggio" : "Attività");
        textColumn(placeColumn, Offer::place);
        textColumn(periodColumn, o -> date(o.start(), o.type().equals("TRIP"))
                + (o.end() == null ? "" : " → " + date(o.end(), o.type().equals("TRIP"))));
        textColumn(stateColumn, o -> state(o.status()));
        textColumn(seatsColumn, o -> (o.available() == null ? "—" : o.available()) + " / "
                + (o.capacity() == null ? "—" : o.capacity()));
        textColumn(priceColumn, o -> UiText.money(o.price()));
        offersTable
                .setPlaceholder(new Label("Nessuna offerta. Crea la tua prima esperienza oppure modifica i filtri."));
        offersTable.setFixedCellSize(64);
        offersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        actionsColumn.setMinWidth(180);
        seatsColumn.setMinWidth(85);
        priceColumn.setMinWidth(80);
        actionsColumn.setCellFactory(c -> new TableCell<>() {
            private final Button edit = new Button("Modifica");
            private final MenuItem view = new MenuItem("Apri dettaglio"), delete = new MenuItem("Elimina offerta…");
            private final MenuButton more = new MenuButton("•••", null, view, delete);
            private final HBox controls = new HBox(6, edit, more);
            {
                edit.getStyleClass().add("organizer-row-action");
                more.getStyleClass().add("organizer-row-action");
                edit.setMinHeight(38);
                more.setMinHeight(38);
                edit.setPrefHeight(38);
                more.setPrefHeight(38);
                edit.setMaxHeight(38);
                more.setMaxHeight(38);
                edit.setMinWidth(82);
                more.setMinWidth(60);
                more.setAccessibleText("Altre azioni sull’offerta");
                edit.setOnAction(e -> edit(row()));
                view.setOnAction(e -> open(row()));
                delete.setOnAction(e -> delete(row()));
            }

            private Offer row() {
                return getIndex() < getTableView().getItems().size() ? getTableView().getItems().get(getIndex()) : null;
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : controls);
            }
        });
        offersTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2)
                open(offersTable.getSelectionModel().getSelectedItem());
        });
        if (offerStatusFilter != null) {
            offerStatusFilter.getItems().setAll("Tutti gli stati", "Pubblicata", "Bozza", "Annullata", "Conclusa");
            offerStatusFilter.getSelectionModel().selectFirst();
            offerTypeFilter.getItems().setAll("Tutti i tipi", "Viaggi", "Attività");
            offerTypeFilter.getSelectionModel().selectFirst();
            offerStatusFilter.valueProperty().addListener((o, a, b) -> filter());
            offerTypeFilter.valueProperty().addListener((o, a, b) -> filter());
            offerSearchField.textProperty().addListener((o, a, b) -> filter());
            pagination.currentPageIndexProperty().addListener((o, a, b) -> page());
        }
        if (subscribersTable != null) {
            participantColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getParticipantName()));
            activityColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemTitle()));
            dateColumn.setCellValueFactory(
                    c -> new SimpleStringProperty(BookingPresentation.formatBookedAt(c.getValue())));
            subscribersTable.setPlaceholder(new Label("Le nuove prenotazioni appariranno qui."));
            subscribersTable.setFixedCellSize(46);
        }
        if (!catalog() && economyHost != null) {
            economy = new OrganizerEconomy();
            economyHost.getChildren().setAll(economy);
        }
        if (catalog())
            setUser(UserSession.getInstance());
    }

    private String date(LocalDateTime date, boolean trip) {
        return date == null ? "Da definire"
                : date.format(DateTimeFormatter.ofPattern(trip ? "dd/MM/yy" : "dd/MM/yy HH:mm"));
    }

    private String state(String state) {
        return state == null ? "—" : switch (state) {
            case "PUBLISHED" -> "Pubblicata";
            case "DRAFT" -> "Bozza";
            case "CANCELLED" -> "Annullata";
            case "COMPLETED" -> "Conclusa";
            default -> state;
        };
    }

    @Override
    public void setUser(UserSession user) {
        if (user != null && user.getId() != null)
            handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        if (economy != null)
            economy.refresh();
        long version = ++generation;
        ViewSupport.clear(dashboardErrorLabel);
        ApiService.callDetailed(ApiService.BASE_URL + "/api/trips/organizer/" + UserSession.getInstance().getId(), null,
                ApiService.Method.GET, r -> {
                    if (closed || version != generation)
                        return;
                    trips = ApiService.fromJson(r.body(), new TypeToken<List<TripDTO>>() {
                    }.getType());
                    if (trips == null)
                        trips = List.of();
                    filter();
                }, this::error, null);
        ApiService.callDetailed(ApiService.BASE_URL + "/api/activities/organizer/mine", null, ApiService.Method.GET,
                r -> {
                    if (closed || version != generation)
                        return;
                    activities = ApiService.fromJson(r.body(), new TypeToken<List<ActivityDTO>>() {
                    }.getType());
                    if (activities == null)
                        activities = List.of();
                    filter();
                }, this::error, null);
        if (catalog())
            return;
        ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/organizer/mine", null, ApiService.Method.GET,
                r -> {
                    if (closed || version != generation)
                        return;
                    List<BookingDTO> rows = ApiService.fromJson(r.body(), new TypeToken<List<BookingDTO>>() {
                    }.getType());
                    if (rows == null)
                        rows = List.of();
                    totalSubscribersLabel
                            .setText(String.valueOf(rows.stream().filter(b -> !BookingPresentation.isCancelled(b))
                                    .mapToInt(b -> b.getSeats() == null ? 0 : b.getSeats()).sum()));
                    subscribersTable
                            .getItems().setAll(
                                    rows.stream()
                                            .sorted(Comparator.comparing(BookingDTO::getCreatedAt,
                                                    Comparator.nullsLast(Comparator.reverseOrder())))
                                            .limit(5).toList());
                }, this::error, null);
        ApiService.callDetailed(ApiService.BASE_URL + "/api/payments/organizer/revenue", null, ApiService.Method.GET,
                r -> {
                    if (!closed && version == generation)
                        totalRevenueLabel.setText(UiText.money(ApiService.fromJson(r.body(), BigDecimal.class)));
                }, this::error, null);
    }

    private void filter() {
        List<Offer> all = new ArrayList<>();
        trips.forEach(t -> all.add(new Offer("TRIP", t.getTripId(), t.getTitle(), t.getDestinationCity(), t.getStatus(),
                t.getStartDate() == null ? null : t.getStartDate().atStartOfDay(),
                t.getEndDate() == null ? null : t.getEndDate().atStartOfDay(), t.getMaxSeats(), t.getAvailableSeats(),
                t.getTotalPrice(), t, null)));
        activities.forEach(a -> all.add(new Offer("ACTIVITY", a.getActivityId(), a.getTitle(), a.getCity(),
                a.getStatus(), a.getStartDate(), a.getEndDate(), a.getMaxSeats(), a.getAvailableSeats(),
                a.getPrice() == null ? null : BigDecimal.valueOf(a.getPrice()), null, a)));
        if (!catalog()) {
            publishedCountLabel
                    .setText(String.valueOf(all.stream().filter(o -> "PUBLISHED".equals(o.status())).count()));
            draftCountLabel.setText(String.valueOf(all.stream().filter(o -> "DRAFT".equals(o.status())).count()));
        }
        if (draftActions != null) {
            draftActions.getChildren().clear();
            all.stream().filter(o -> "DRAFT".equals(o.status())).limit(3).forEach(offer -> {
                Button complete = new Button("Completa “" + offer.title() + "”");
                complete.setMaxWidth(Double.MAX_VALUE);
                complete.getStyleClass().add("btn-secondary");
                complete.setOnAction(e -> edit(offer));
                draftActions.getChildren().add(complete);
            });
            if (draftActions.getChildren().isEmpty())
                draftActions.getChildren().add(new Label("Nessuna bozza da completare. Le tue offerte sono pronte."));
        }
        String query = offerSearchField == null ? "" : offerSearchField.getText().strip().toLowerCase(Locale.ROOT);
        filtered = all.stream().filter(o -> catalog()
                || ("PUBLISHED".equals(o.status()) && o.start() != null && !o.start().isBefore(LocalDateTime.now())))
                .filter(o -> query.isBlank() || (o.title() + " " + o.place()).toLowerCase(Locale.ROOT).contains(query))
                .filter(o -> offerStatusFilter == null || offerStatusFilter.getSelectionModel().getSelectedIndex() == 0
                        || state(o.status()).equals(offerStatusFilter.getValue()))
                .filter(o -> offerTypeFilter == null || offerTypeFilter.getSelectionModel().getSelectedIndex() == 0
                        || (offerTypeFilter.getValue().equals("Viaggi") == o.type().equals("TRIP")))
                .sorted(Comparator.comparing(Offer::start, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Offer::id))
                .toList();
        if (pagination != null) {
            pagination.setCurrentPageIndex(0);
            pagination.setPageCount(Math.max(1, (filtered.size() + 11) / 12));
        }
        page();
    }

    private void page() {
        int start = pagination == null ? 0 : pagination.getCurrentPageIndex() * 12;
        offersTable.getItems().setAll(filtered.stream().skip(start).limit(catalog() ? 12 : 5).toList());
        offersTable
                .setPrefHeight(Math.min(catalog() ? 590 : 375, Math.max(120, offersTable.getItems().size() * 64 + 54)));
    }

    @FXML
    private void handleOpenCreateOffer() {
        edit(null);
    }

    private void edit(Offer offer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/organizer/create-offer.fxml"));
            Parent content = loader.load();
            CreateOfferController controller = loader.getController();
            if (offer != null) {
                if (offer.trip() != null)
                    controller.setTripToEdit(offer.trip());
                else
                    controller.setActivityToEdit(offer.activity());
            }
            controller.setCloseAction(() -> {
                if (offerPopup != null) {
                    offerPopup.close();
                    offerPopup = null;
                }
                handleRefresh();
            });
            offerPopup = PopupFactory.show(rootStackPane, content, PopupFactory.Size.LARGE);
        } catch (IOException ex) {
            error("Impossibile aprire il modulo. Riprova.");
        }
    }

    private void open(Offer offer) {
        if (offer != null)
            SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/activity/activity-details.fxml",
                    c -> ((ActivityDetailsController) c).loadData(ActivityDetailsController.Type.valueOf(offer.type()),
                            offer.id().toString()));
    }

    private void delete(Offer offer) {
        if (offer == null)
            return;
        PopupFactory.showConfirmation(rootStackPane, "Eliminare “" + offer.title() + "”?",
                "L'offerta non sarà più disponibile. Le prenotazioni collegate saranno gestite secondo le regole del servizio.",
                PopupFactory.Size.COMPACT,
                () -> ApiService
                        .callDetailed(
                                ApiService.BASE_URL + "/api/" + (offer.type().equals("TRIP") ? "trips/" : "activities/")
                                        + offer.id(),
                                null, ApiService.Method.DELETE, r -> handleRefresh(), this::error, null));
    }

    @FXML
    private void handleCatalog() {
        navigate("organizer/events");
    }

    @FXML
    private void handleBookings() {
        navigate("organizer/attendees");
    }

    @FXML
    private void handleFinance() {
        navigate("profile/payment-methods");
    }

    private void navigate(String page) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/" + page + ".fxml", null);
    }

    private void error(String message) {
        if (!closed)
            ViewSupport.error(dashboardErrorLabel, message);
    }

    @Override
    public void close() {
        if (economy != null)
            economy.close();
        closed = true;
        generation++;
        if (offerPopup != null)
            offerPopup.close();
    }
}
