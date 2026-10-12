package it.unical.ea_project_javafx.controller.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.home.ExperienceCardController;
import it.unical.ea_project_javafx.dto.BookingDTO;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TravelerDashboardController implements ProfileDashboardController {

    @FXML private VBox bookingStat;
    @FXML private HBox recommendedContainer;
    @FXML private ScrollPane recommendedScroll;
    @FXML private Button searchToggleButton;
    @FXML private javafx.scene.control.TextField searchField;
    @FXML private ComboBox<String> searchTypeCombo;
    @FXML private Label bookingValue;
    @FXML private Label bookingDetail;
    @FXML private TableView<BookingRow> bookingsTable;
    @FXML private TableColumn<BookingRow, String> activityColumn;
    @FXML private TableColumn<BookingRow, String> dateColumn;
    @FXML private TableColumn<BookingRow, String> locationColumn;
    @FXML private TableColumn<BookingRow, String> statusColumn;
    @FXML private TableColumn<BookingRow, Void> actionColumn;

    @FXML
    public void initialize() {
        searchTypeCombo.setItems(FXCollections.observableArrayList("Attività", "Viaggi"));
        searchTypeCombo.getSelectionModel().selectFirst();

        PauseTransition debounce = new PauseTransition(Duration.millis(280));
        debounce.setOnFinished(event -> handleSearch());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchField.isVisible()) debounce.playFromStart();
        });
        searchTypeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (searchField.isVisible() && searchField.getText() != null && !searchField.getText().isBlank()) {
                debounce.playFromStart();
            }
        });
    }

    @Override
    public void setUser(UserSession userSession) {
        loadRecommendations();
        setupBookingsTable();
        loadBookings();
    }

    private void loadRecommendations() {
        ApiService.get(
                ApiService.BASE_URL + "/api/activities/top/cards?page=0&size=6",
                response -> {
                    Type type = new TypeToken<List<ActivityHomeDTO>>() { }.getType();
                    List<ActivityHomeDTO> activities = GSON.fromJson(response.body(), type);
                    Platform.runLater(() -> renderActivityCards(activities));
                },
                () -> Platform.runLater(() -> recommendedContainer.getChildren().clear()),
                null
        );
    }

    @FXML
    private void toggleSearch() {
        boolean show = !searchField.isVisible();
        searchField.setVisible(show);
        searchField.setManaged(show);
        searchTypeCombo.setVisible(show);
        searchTypeCombo.setManaged(show);
        if (show) {
            searchField.requestFocus();
        } else {
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
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        boolean trips = "Viaggi".equals(searchTypeCombo.getValue());
        String endpoint = trips ? "/api/trips/search" : "/api/activities/search";
        String url = ApiService.BASE_URL + endpoint + "?query=" + encodedQuery + "&page=0&size=9";

        ApiService.get(url, response -> {
            if (trips) {
                Type type = new TypeToken<List<TripHomeDTO>>() { }.getType();
                List<TripHomeDTO> results = GSON.fromJson(response.body(), type);
                Platform.runLater(() -> renderTripCards(results));
            } else {
                Type type = new TypeToken<List<ActivityHomeDTO>>() { }.getType();
                List<ActivityHomeDTO> results = GSON.fromJson(response.body(), type);
                Platform.runLater(() -> renderActivityCards(results));
            }
        },
                () -> Platform.runLater(() -> recommendedContainer.getChildren().clear()),
                null);
    }

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> 
            LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
            new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> 
            LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
        .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> 
            new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
        .create();

    private void renderActivityCards(List<ActivityHomeDTO> activities) {
        recommendedContainer.getChildren().clear();
        if (activities == null) return;
        activities.stream().filter(item -> item != null).forEach(item -> addCard(item, false));
    }

    private void renderTripCards(List<TripHomeDTO> trips) {
        recommendedContainer.getChildren().clear();
        if (trips == null) return;
        trips.stream().filter(item -> item != null).forEach(item -> addCard(item, true));
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
            exception.printStackTrace();
        }
    }

    @FXML
    private void handlePreviousRecommendation() {
        recommendedScroll.setHvalue(Math.max(0.0, recommendedScroll.getHvalue() - 0.35));
    }

    @FXML
    private void handleNextRecommendation() {
        recommendedScroll.setHvalue(Math.min(1.0, recommendedScroll.getHvalue() + 0.35));
    }

    private void setupBookingsTable() {
        activityColumn.setCellValueFactory(cell -> cell.getValue().titleProperty());
        dateColumn.setCellValueFactory(cell -> cell.getValue().dateProperty());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button cancelButton = new Button("Cancella");

            {
                cancelButton.getStyleClass().add("danger-action-button");
                cancelButton.setOnAction(event -> {
                    BookingRow row = getTableView().getItems().get(getIndex());
                    cancelBooking(row.booking());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()
                        || "CANCELLED".equals(getTableView().getItems().get(getIndex()).booking().getBookingStatus())) {
                    setGraphic(null);
                } else {
                    setGraphic(cancelButton);
                }
            }
        });
    }

    private void loadBookings() {
        ApiService.get(
                ApiService.BASE_URL + "/api/bookings/mine",
                response -> {
                    Type listType = new TypeToken<List<BookingDTO>>() { }.getType();
                    List<BookingDTO> bookings = GSON.fromJson(response.body(), listType);
                    Platform.runLater(() -> renderBookings(bookings));
                },
                () -> Platform.runLater(() -> renderBookings(List.of())),
                null
        );
    }

    private void renderBookings(List<BookingDTO> bookings) {
        List<BookingRow> rows = bookings == null ? List.of() : bookings.stream().map(BookingRow::new).toList();
        bookingsTable.setItems(FXCollections.observableArrayList(rows));
        bookingStat.setVisible(true);
        bookingStat.setManaged(true);
        bookingValue.setText(String.valueOf(rows.size()));
        bookingDetail.setText("prenotazioni");
    }

    private void cancelBooking(BookingDTO booking) {
        ApiService.delete(
                ApiService.BASE_URL + "/api/bookings/" + booking.getBookingId(),
                response -> loadBookings(),
                () -> { },
                null
        );
    }

    public static final class BookingRow {
        private final BookingDTO booking;

        public BookingRow(BookingDTO booking) {
            this.booking = booking;
        }

        public BookingDTO booking() { return booking; }
        public javafx.beans.property.SimpleStringProperty titleProperty() {
            return new javafx.beans.property.SimpleStringProperty(valueOrEmpty(booking.getItemTitle()));
        }
        public javafx.beans.property.SimpleStringProperty dateProperty() {
            return new javafx.beans.property.SimpleStringProperty(valueOrEmpty(booking.getItemDate()));
        }
        public javafx.beans.property.SimpleStringProperty locationProperty() {
            return new javafx.beans.property.SimpleStringProperty(valueOrEmpty(booking.getItemLocation()));
        }
        public javafx.beans.property.SimpleStringProperty statusProperty() {
            return new javafx.beans.property.SimpleStringProperty(valueOrEmpty(booking.getBookingStatus()));
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}