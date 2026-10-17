package it.unical.ea_project_javafx.controller.organizer;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.BookingDTO;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttendeesController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private Label totalAttendeesLabel;
    @FXML private Label confirmedRevenueLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventFilterCombo;

    @FXML private TableView<BookingDTO> attendeesTableView;
    @FXML private TableColumn<BookingDTO, String> participantColumn;
    @FXML private TableColumn<BookingDTO, String> emailColumn;
    @FXML private TableColumn<BookingDTO, String> eventColumn;
    @FXML private TableColumn<BookingDTO, String> seatsColumn;
    @FXML private TableColumn<BookingDTO, String> dateColumn;
    @FXML private TableColumn<BookingDTO, String> statusColumn;

    private final ObservableList<BookingDTO> masterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupTableColumns();
        loadAttendees();

        searchField.textProperty().addListener((obs, oldV, newV) -> filterTable());
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        participantColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getParticipantName()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getParticipantEmail()));
        eventColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemTitle()));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getSeats())));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() == null ? "" : cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBookingStatus()));
    }

    private void loadAttendees() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/organizer/mine",
                response -> {
                    Type type = new TypeToken<List<BookingDTO>>() { }.getType();
                    List<BookingDTO> bookings = ApiService.fromJson(response.body(), type);
                    Platform.runLater(() -> {
                        masterList.setAll(bookings == null ? List.of() : bookings);
                        updateKPIs();
                        filterTable();
                    });
                },
                () -> Platform.runLater(() -> showError("Impossibile caricare il registro degli iscritti.")),
                null
        );
    }

    private void updateKPIs() {
        int totalSeats = masterList.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getBookingStatus()))
                .mapToInt(b -> b.getSeats() != null ? b.getSeats() : 1)
                .sum();
        totalAttendeesLabel.setText(String.valueOf(totalSeats));

        BigDecimal totalRev = masterList.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getBookingStatus()) && b.getPrice() != null)
                .map(BookingDTO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        confirmedRevenueLabel.setText("€ " + totalRev.toPlainString());
    }

    private void filterTable() {
        String query = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        List<BookingDTO> filtered = masterList.stream().filter(b -> {
            return query.isEmpty()
                    || (b.getParticipantName() != null && b.getParticipantName().toLowerCase().contains(query))
                    || (b.getParticipantEmail() != null && b.getParticipantEmail().toLowerCase().contains(query))
                    || (b.getItemTitle() != null && b.getItemTitle().toLowerCase().contains(query));
        }).toList();

        attendeesTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showError(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-error");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }
}