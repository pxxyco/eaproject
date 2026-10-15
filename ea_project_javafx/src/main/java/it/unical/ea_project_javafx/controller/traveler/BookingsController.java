package it.unical.ea_project_javafx.controller.traveler;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

public class BookingsController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private Label totalBookingsLabel;
    @FXML private Label confirmedBookingsLabel;
    @FXML private Label spentTotalLabel;

    @FXML private TextField searchFilterField;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TableView<BookingDTO> bookingsTableView;
    @FXML private TableColumn<BookingDTO, String> itemTitleColumn;
    @FXML private TableColumn<BookingDTO, String> dateColumn;
    @FXML private TableColumn<BookingDTO, String> locationColumn;
    @FXML private TableColumn<BookingDTO, String> seatsColumn;
    @FXML private TableColumn<BookingDTO, String> priceColumn;
    @FXML private TableColumn<BookingDTO, String> statusColumn;
    @FXML private TableColumn<BookingDTO, Void> actionsColumn;

    private final ObservableList<BookingDTO> masterList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        setupFilters();
        setupTableColumns();
        loadBookings();
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

    private void setupFilters() {
        statusFilterCombo.getItems().setAll("Tutti", "CONFIRMED", "PENDING", "CANCELLED");
        statusFilterCombo.getSelectionModel().selectFirst();

        searchFilterField.textProperty().addListener((obs, oldV, newV) -> filterTable());
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterTable());
    }

    private void setupTableColumns() {
        itemTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemTitle()));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemDate()));
        locationColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItemLocation()));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getSeats())));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getPrice() != null ? "€ " + cell.getValue().getPrice().toPlainString() : "€ 0.00"));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBookingStatus()));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button exportIcsBtn = new Button("📅 .ics");
            private final Button cancelBtn = new Button("Cancella");
            private final HBox container = new HBox(6, exportIcsBtn, cancelBtn);

            {
                exportIcsBtn.getStyleClass().add("btn-secondary");
                cancelBtn.getStyleClass().add("danger-action-button");

                exportIcsBtn.setOnAction(e -> {
                    BookingDTO item = getTableView().getItems().get(getIndex());
                    exportIcs(item);
                });

                cancelBtn.setOnAction(e -> {
                    BookingDTO item = getTableView().getItems().get(getIndex());
                    cancelBooking(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BookingDTO dto = getTableView().getItems().get(getIndex());
                    cancelBtn.setVisible(!"CANCELLED".equalsIgnoreCase(dto.getBookingStatus()));
                    setGraphic(container);
                }
            }
        });
    }

    private void loadBookings() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/mine",
                response -> {
                    Type type = new TypeToken<List<BookingDTO>>() { }.getType();
                    List<BookingDTO> list = ApiService.fromJson(response.body(), type);
                    Platform.runLater(() -> {
                        masterList.setAll(list == null ? List.of() : list);
                        updateKPIs();
                        filterTable();
                    });
                },
                () -> Platform.runLater(() -> showError("Impossibile caricare le prenotazioni.")),
                null
        );
    }

    private void updateKPIs() {
        totalBookingsLabel.setText(String.valueOf(masterList.size()));
        long confirmed = masterList.stream().filter(b -> "CONFIRMED".equalsIgnoreCase(b.getBookingStatus())).count();
        confirmedBookingsLabel.setText(String.valueOf(confirmed));

        BigDecimal total = masterList.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getBookingStatus()) && b.getPrice() != null)
                .map(BookingDTO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        spentTotalLabel.setText("€ " + total.toPlainString());
    }

    private void filterTable() {
        String query = searchFilterField.getText() == null ? "" : searchFilterField.getText().toLowerCase().trim();
        String selectedStatus = statusFilterCombo.getValue();

        List<BookingDTO> filtered = masterList.stream().filter(b -> {
            boolean matchQuery = query.isEmpty()
                    || (b.getItemTitle() != null && b.getItemTitle().toLowerCase().contains(query))
                    || (b.getItemLocation() != null && b.getItemLocation().toLowerCase().contains(query));
            boolean matchStatus = selectedStatus == null || "Tutti".equalsIgnoreCase(selectedStatus)
                    || selectedStatus.equalsIgnoreCase(b.getBookingStatus());
            return matchQuery && matchStatus;
        }).toList();

        bookingsTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void exportIcs(BookingDTO dto) {
        ApiService.get(ApiService.BASE_URL + "/api/v1/trips/" + dto.getBookingId() + "/export/ics",
                response -> Platform.runLater(() -> showSuccess("File calendario .ics esportato con successo!")),
                () -> Platform.runLater(() -> showError("Esportazione calendario in corso...")),
                null
        );
    }

    private void cancelBooking(BookingDTO dto) {
        ApiService.delete(ApiService.BASE_URL + "/api/bookings/" + dto.getBookingId(),
                response -> Platform.runLater(() -> {
                    showSuccess("Prenotazione cancellata.");
                    loadBookings();
                }),
                () -> Platform.runLater(() -> showError("Impossibile cancellare la prenotazione.")),
                null
        );
    }

    private void showError(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-error");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-success");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }
}