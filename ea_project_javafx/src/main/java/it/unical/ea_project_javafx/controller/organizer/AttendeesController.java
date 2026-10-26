package it.unical.ea_project_javafx.controller.organizer;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class AttendeesController {

    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private Label statusMessageLabel;

    @FXML
    private Label totalAttendeesLabel;
    @FXML
    private Label confirmedRevenueLabel;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> eventFilterCombo;

    @FXML
    private TableView<BookingDTO> attendeesTableView;
    @FXML
    private TableColumn<BookingDTO, String> participantColumn;
    @FXML
    private TableColumn<BookingDTO, String> emailColumn;
    @FXML
    private TableColumn<BookingDTO, String> eventColumn;
    @FXML
    private TableColumn<BookingDTO, String> seatsColumn;
    @FXML
    private TableColumn<BookingDTO, String> dateColumn;
    @FXML
    private TableColumn<BookingDTO, String> statusColumn;

    private final ObservableList<BookingDTO> masterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        ViewSupport.loadNavbar(navbarContainer);
        setupTableColumns();
        loadAttendees();
        loadPaidRevenue();
        eventFilterCombo.setOnAction(event -> filterTable());

        searchField.textProperty().addListener((obs, oldV, newV) -> filterTable());
    }

    private void setupTableColumns() {
        participantColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getParticipantName()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getParticipantEmail()));
        eventColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemTitle()));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getSeats())));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() == null ? ""
                        : cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBookingStatus()));
    }

    private void loadAttendees() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/organizer/mine",
                response -> {
                    Type type = new TypeToken<List<BookingDTO>>() {
                    }.getType();
                    List<BookingDTO> bookings = ApiService.fromJson(response.body(), type);
                    Platform.runLater(() -> {
                        masterList.setAll(bookings == null ? List.of() : bookings);
                        eventFilterCombo.getItems().setAll("Tutti gli eventi");
                        masterList.stream().map(BookingDTO::getItemTitle).filter(Objects::nonNull)
                                .distinct().sorted().forEach(title -> eventFilterCombo.getItems().add(title));
                        eventFilterCombo.getSelectionModel().selectFirst();
                        updateKPIs();
                        filterTable();
                    });
                },
                () -> Platform.runLater(() -> ViewSupport.error(statusMessageLabel,
                        "Impossibile caricare il registro degli iscritti.")),
                null);
    }

    private void updateKPIs() {
        int totalSeats = masterList.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getBookingStatus()))
                .mapToInt(b -> b.getSeats() != null ? b.getSeats() : 1)
                .sum();
        totalAttendeesLabel.setText(String.valueOf(totalSeats));

    }

    private void loadPaidRevenue() {
        confirmedRevenueLabel.setText("—");
        ApiService.get(ApiService.BASE_URL + "/api/payments/organizer/revenue",
                response -> {
                    BigDecimal revenue = ApiService.fromJson(response.body(), BigDecimal.class);
                    confirmedRevenueLabel.setText(NumberFormat.getCurrencyInstance(Locale.ITALY)
                            .format(revenue == null ? BigDecimal.ZERO : revenue));
                }, () -> ViewSupport.error(statusMessageLabel, "Impossibile caricare gli incassi da pagamenti."), null);
    }

    private void filterTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        List<BookingDTO> filtered = masterList.stream().filter(b -> {
            String selectedEvent = eventFilterCombo.getValue();
            boolean matchesEvent = selectedEvent == null || "Tutti gli eventi".equals(selectedEvent)
                    || selectedEvent.equals(b.getItemTitle());
            return matchesEvent && (query.isEmpty()
                    || (b.getParticipantName() != null && b.getParticipantName().toLowerCase().contains(query))
                    || (b.getParticipantEmail() != null && b.getParticipantEmail().toLowerCase().contains(query))
                    || (b.getItemTitle() != null && b.getItemTitle().toLowerCase().contains(query)));
        }).toList();

        attendeesTableView.setItems(FXCollections.observableArrayList(filtered));
    }

}
