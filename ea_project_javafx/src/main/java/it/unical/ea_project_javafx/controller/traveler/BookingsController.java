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
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Comparator;

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
    private StackPane exportPopupOverlay;
    private TextField exportFileNameField;
    private BookingDTO pendingExportBooking;

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

        // Le prenotazioni cancellate restano visibili, ma in fondo alla lista e attenuate graficamente.
        bookingsTableView.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(BookingDTO booking, boolean empty) {
                super.updateItem(booking, empty);
                getStyleClass().remove("cancelled-booking-row");
                if (!empty && booking != null && "CANCELLED".equalsIgnoreCase(booking.getBookingStatus())) {
                    getStyleClass().add("cancelled-booking-row");
                }
            }
        });

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
                    boolean cancelled = "CANCELLED".equalsIgnoreCase(dto.getBookingStatus());
                    exportIcsBtn.setVisible(!cancelled);
                    exportIcsBtn.setManaged(!cancelled);
                    cancelBtn.setVisible(!cancelled);
                    cancelBtn.setManaged(!cancelled);
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
        }).sorted(Comparator.comparing((BookingDTO b) -> "CANCELLED".equalsIgnoreCase(b.getBookingStatus()))).toList();

        bookingsTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void exportIcs(BookingDTO dto) {
        if (dto == null || dto.getBookingId() == null) {
            showError("Seleziona una prenotazione valida da esportare.");
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(dto.getBookingStatus())) {
            showError("Non puoi esportare una prenotazione cancellata.");
            return;
        }
        pendingExportBooking = dto;
        String suggested = dto.getItemTitle() == null ? "prenotazione" : dto.getItemTitle();
        suggested = suggested.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (suggested.isBlank()) suggested = "prenotazione";
        exportFileNameField = new TextField(suggested + ".ics");
        showExportPopup(dto);
    }

    private void showExportPopup(BookingDTO dto) {
        closeExportPopup();
        exportPopupOverlay = new StackPane();
        exportPopupOverlay.getStyleClass().add("export-popup-overlay");
        exportPopupOverlay.setPickOnBounds(true);

        VBox panel = new VBox(14);
        panel.getStyleClass().add("export-popup-panel");
        panel.setMaxWidth(480);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label("Esporta prenotazione in calendario");
        title.getStyleClass().add("export-popup-title");
        title.setWrapText(true);
        Button close = new Button("×");
        close.getStyleClass().add("export-popup-close");
        close.setAccessibleText("Chiudi finestra di esportazione");
        close.setOnAction(e -> closeExportPopup());
        HBox titleBar = new HBox(12, title, new Region(), close);
        titleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(titleBar.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
        Label subtitle = new Label("Verrà creato un file .ics compatibile con Google Calendar, Apple Calendar e Outlook.");
        subtitle.getStyleClass().add("export-popup-description");
        subtitle.setWrapText(true);
        Label eventLabel = new Label(dto.getItemTitle() == null ? "Prenotazione" : dto.getItemTitle());
        eventLabel.getStyleClass().add("export-popup-event");
        Label filenameLabel = new Label("Nome del file");
        filenameLabel.getStyleClass().add("export-popup-label");
        exportFileNameField.getStyleClass().add("custom-input");
        exportFileNameField.setPromptText("Insert a filename ...");
        Label info = new Label("Il file conterrà solo i dati della tua prenotazione. Per esportare devi essere autenticato.");
        info.getStyleClass().add("export-popup-description");
        info.setWrapText(true);

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button cancel = new Button("Annulla");
        cancel.getStyleClass().add("btn-secondary");
        Button save = new Button("Scegli posizione e salva");
        save.getStyleClass().add("btn-primary");
        cancel.setOnAction(e -> closeExportPopup());
        save.setOnAction(e -> downloadAndSaveIcs());
        actions.getChildren().addAll(cancel, save);
        panel.getChildren().addAll(titleBar, subtitle, eventLabel, filenameLabel, exportFileNameField, info, actions);
        exportPopupOverlay.getChildren().add(panel);

        Object sceneRoot = statusMessageLabel.getScene().getRoot();
        if (sceneRoot instanceof StackPane root) {
            root.getChildren().add(exportPopupOverlay);
        } else {
            showError("Impossibile aprire il popup di esportazione.");
            exportPopupOverlay = null;
        }
    }

    private void downloadAndSaveIcs() {
        BookingDTO dto = pendingExportBooking;
        if (dto == null) return;
        String filename = exportFileNameField.getText() == null ? "" : exportFileNameField.getText().trim();
        if (filename.isBlank()) {
            exportFileNameField.setPromptText("Inserisci un nome file");
            return;
        }
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!filename.toLowerCase().endsWith(".ics")) filename += ".ics";
        final String suggestedFilename = filename;
        closeExportPopup();

        ApiService.get(ApiService.BASE_URL + "/api/bookings/" + dto.getBookingId() + "/export/ics",
                response -> Platform.runLater(() -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Salva esportazione calendario");
                    chooser.setInitialFileName(suggestedFilename);
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Calendario iCalendar (*.ics)", "*.ics"));
                    Window owner = statusMessageLabel.getScene() == null ? null : statusMessageLabel.getScene().getWindow();
                    File file = chooser.showSaveDialog(owner);
                    if (file == null) {
                        showSuccess("Esportazione annullata: nessun file salvato.");
                        return;
                    }
                    try {
                        Files.writeString(file.toPath(), response.body(), StandardCharsets.UTF_8);
                        showSuccess("File .ics salvato in: " + file.getAbsolutePath());
                    } catch (Exception ex) {
                        showError("Non è stato possibile salvare il file .ics.");
                    }
                }),
                () -> Platform.runLater(() -> showError("Esportazione non riuscita. Verifica di essere loggato e che la prenotazione sia ancora attiva.")),
                null
        );
    }

    private void closeExportPopup() {
        if (exportPopupOverlay != null && statusMessageLabel.getScene() != null
                && statusMessageLabel.getScene().getRoot() instanceof StackPane root) {
            root.getChildren().remove(exportPopupOverlay);
        }
        exportPopupOverlay = null;
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