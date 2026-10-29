package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.UiText;
import it.unical.ea_project_javafx.ui.BookingPresentation;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import it.unical.ea_project_javafx.ui.file.NativeFiles;
import javafx.stage.Window;

import static it.unical.ea_project_javafx.ui.BookingPresentation.bookingRow;
import static it.unical.ea_project_javafx.ui.BookingPresentation.compareBookings;
import static it.unical.ea_project_javafx.ui.BookingPresentation.displayStatus;
import static it.unical.ea_project_javafx.ui.BookingPresentation.formatBookedAt;
import static it.unical.ea_project_javafx.ui.BookingPresentation.formatEventPeriod;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isCancelled;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isEventEnded;
import static it.unical.ea_project_javafx.ui.BookingPresentation.isHistorical;
import static it.unical.ea_project_javafx.ui.BookingPresentation.statusCell;

public class BookingsController {

    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private Label statusMessageLabel;

    @FXML
    private Label totalBookingsLabel;
    @FXML
    private Label activeBookingsLabel;
    @FXML
    private Label historicalBookingsLabel;
    @FXML
    private Label spentTotalLabel;

    @FXML
    private TextField searchFilterField;
    @FXML
    private ComboBox<String> periodFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private TableView<BookingDTO> bookingsTableView;
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
    private TableColumn<BookingDTO, Void> actionsColumn;

    private final ObservableList<BookingDTO> masterList = FXCollections.observableArrayList();
    private StackPane exportPopupOverlay;
    private TextField exportFileNameField;
    private BookingDTO pendingExportBooking;
    private LocalDateTime evaluationTime = LocalDateTime.now();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        ViewSupport.loadNavbar(navbarContainer);
        setupFilters();
        setupTableColumns();
        loadBookings();
    }

    private void setupFilters() {
        periodFilterCombo.getItems().setAll("Tutte", "In programma", "Storico");
        periodFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getItems().setAll(
                "Tutti", "Da pagare", "Confermate", "In corso", "Concluse", "Scadute", "Cancellate");
        statusFilterCombo.getSelectionModel().selectFirst();

        searchFilterField.textProperty().addListener((obs, oldV, newV) -> filterTable());
        periodFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterTable());
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterTable());
    }

    private void setupTableColumns() {
        itemTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                UiText.orDefault(cell.getValue().getItemTitle(), "Esperienza non disponibile")));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatEventPeriod(cell.getValue())));
        bookedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatBookedAt(cell.getValue())));
        locationColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                UiText.orDefault(cell.getValue().getItemLocation(), "—")));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSeats() == null ? "—" : String.valueOf(cell.getValue().getSeats())));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(UiText.money(cell.getValue().getPrice())));
        statusColumn
                .setCellValueFactory(cell -> new SimpleStringProperty(displayStatus(cell.getValue(), evaluationTime)));
        statusColumn.setCellFactory(column -> statusCell());

        bookingsTableView.setPlaceholder(new Label("Non hai ancora effettuato prenotazioni."));
        // Le prenotazioni future vengono mostrate prima dello storico; le cancellate
        // restano in fondo.
        bookingsTableView.setRowFactory(table -> bookingRow(() -> evaluationTime));

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button exportIcsBtn = new Button("📅 .ics");
            private final Button cancelBtn = new Button("Cancella");
            private final HBox container = new HBox(6, exportIcsBtn, cancelBtn);

            {
                exportIcsBtn.getStyleClass().add("btn-secondary");
                cancelBtn.getStyleClass().add("danger-action-button");
                exportIcsBtn.setAccessibleText("Esporta questa prenotazione in calendario");
                cancelBtn.setAccessibleText("Cancella questa prenotazione");
                exportIcsBtn.setOnAction(e -> exportIcs(currentRowBooking()));
                cancelBtn.setOnAction(e -> cancelBooking(currentRowBooking()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                BookingDTO dto = currentRowBooking();
                if (dto == null) {
                    setGraphic(null);
                    return;
                }
                boolean cancelled = isCancelled(dto);
                boolean canCancel = BookingPresentation.canCancel(dto, evaluationTime);
                exportIcsBtn.setVisible(!cancelled);
                exportIcsBtn.setManaged(!cancelled);
                cancelBtn.setVisible(canCancel);
                cancelBtn.setManaged(canCancel);
                setGraphic(container);
            }

            private BookingDTO currentRowBooking() {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size())
                    return null;
                return getTableView().getItems().get(index);
            }
        });
    }

    private void loadBookings() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/mine",
                response -> {
                    try {
                        Type type = new TypeToken<List<BookingDTO>>() {
                        }.getType();
                        List<BookingDTO> list = ApiService.fromJson(response.body(), type);
                        Platform.runLater(() -> {
                            evaluationTime = LocalDateTime.now();
                            masterList.setAll(list == null ? List.of() : list);
                            updateKPIs();
                            filterTable();
                        });
                    } catch (Exception exception) {
                        Platform.runLater(
                                () -> ViewSupport.error(statusMessageLabel,
                                        "Il server ha restituito uno storico prenotazioni non valido."));
                    }
                },
                () -> Platform.runLater(
                        () -> ViewSupport.error(statusMessageLabel,
                                "Impossibile caricare le prenotazioni. Verifica la connessione e riprova.")),
                null);
    }

    private void updateKPIs() {
        totalBookingsLabel.setText(String.valueOf(masterList.size()));
        long active = masterList.stream()
                .filter(b -> !isCancelled(b) && !isHistorical(b, evaluationTime))
                .count();
        activeBookingsLabel.setText(String.valueOf(active));
        long historical = masterList.stream().filter(b -> isHistorical(b, evaluationTime)).count();
        historicalBookingsLabel.setText(String.valueOf(historical));

        BigDecimal total = masterList.stream()
                .filter(b -> !isCancelled(b) && b.getPrice() != null)
                .map(BookingDTO::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        spentTotalLabel.setText(UiText.money(total));
    }

    private void filterTable() {
        evaluationTime = LocalDateTime.now();
        String query = searchFilterField.getText() == null
                ? ""
                : searchFilterField.getText().toLowerCase(Locale.ITALY).trim();
        String selectedStatus = statusFilterCombo.getValue();
        String selectedPeriod = periodFilterCombo.getValue();

        List<BookingDTO> filtered = masterList.stream()
                .filter(b -> matchesQuery(b, query))
                .filter(b -> matchesPeriod(b, selectedPeriod))
                .filter(b -> matchesStatus(b, selectedStatus))
                .sorted((left, right) -> compareBookings(left, right, evaluationTime))
                .toList();

        bookingsTableView.setItems(FXCollections.observableArrayList(filtered));
        if (filtered.isEmpty()) {
            bookingsTableView.setPlaceholder(new Label(masterList.isEmpty()
                    ? "Non hai ancora effettuato prenotazioni."
                    : "Nessuna prenotazione corrisponde ai filtri selezionati."));
        }
    }

    private boolean matchesQuery(BookingDTO booking, String query) {
        if (query.isBlank())
            return true;
        return UiText.contains(booking.getItemTitle(), query)
                || UiText.contains(booking.getItemLocation(), query)
                || UiText.contains(formatEventPeriod(booking), query)
                || UiText.contains(booking.getBookingType(), query);
    }

    private boolean matchesPeriod(BookingDTO booking, String period) {
        if (period == null || "Tutte".equalsIgnoreCase(period))
            return true;
        boolean historical = isHistorical(booking, evaluationTime);
        if ("Storico".equalsIgnoreCase(period))
            return historical;
        if ("In programma".equalsIgnoreCase(period))
            return !historical && !isCancelled(booking);
        return true;
    }

    private boolean matchesStatus(BookingDTO booking, String selectedStatus) {
        if (selectedStatus == null || "Tutti".equalsIgnoreCase(selectedStatus))
            return true;
        String display = displayStatus(booking, evaluationTime);
        return switch (selectedStatus) {
            case "Da pagare" -> "Da pagare".equals(display);
            case "Confermate" -> "Confermata".equals(display);
            case "In corso" -> "In corso".equals(display);
            case "Concluse" -> "Conclusa".equals(display);
            case "Scadute" -> "Scaduta".equals(display);
            case "Cancellate" -> isCancelled(booking);
            default -> true;
        };
    }

    private void exportIcs(BookingDTO dto) {
        if (dto == null || dto.getBookingId() == null) {
            ViewSupport.error(statusMessageLabel, "Seleziona una prenotazione valida da esportare.");
            return;
        }
        if (isCancelled(dto)) {
            ViewSupport.error(statusMessageLabel, "Non puoi esportare una prenotazione o un'esperienza cancellata.");
            return;
        }
        pendingExportBooking = dto;
        String suggested = dto.getItemTitle() == null ? "prenotazione" : dto.getItemTitle();
        suggested = suggested.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (suggested.isBlank())
            suggested = "prenotazione";
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
        titleBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBar.getChildren().get(1), Priority.ALWAYS);
        Label subtitle = new Label(
                "Verrà creato un file .ics compatibile con Google Calendar, Apple Calendar e Outlook.");
        subtitle.getStyleClass().add("export-popup-description");
        subtitle.setWrapText(true);
        Label eventLabel = new Label(dto.getItemTitle() == null ? "Prenotazione" : dto.getItemTitle());
        eventLabel.getStyleClass().add("export-popup-event");
        Label filenameLabel = new Label("Nome del file");
        filenameLabel.getStyleClass().add("export-popup-label");
        exportFileNameField.getStyleClass().add("custom-input");
        exportFileNameField.setPromptText("Insert a filename ...");
        Label info = new Label(
                "Il file conterrà solo i dati della tua prenotazione. Per esportare devi essere autenticato.");
        info.getStyleClass().add("export-popup-description");
        info.setWrapText(true);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
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
            ViewSupport.error(statusMessageLabel, "Impossibile aprire il popup di esportazione.");
            exportPopupOverlay = null;
        }
    }

    private void downloadAndSaveIcs() {
        BookingDTO dto = pendingExportBooking;
        if (dto == null)
            return;
        String filename = exportFileNameField.getText() == null ? "" : exportFileNameField.getText().trim();
        if (filename.isBlank()) {
            exportFileNameField.setPromptText("Inserisci un nome file");
            return;
        }
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!filename.toLowerCase().endsWith(".ics"))
            filename += ".ics";
        final String suggestedFilename = filename;
        closeExportPopup();

        ApiService.get(ApiService.BASE_URL + "/api/bookings/" + dto.getBookingId() + "/export/ics",
                response -> Platform.runLater(() -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Salva esportazione calendario");
                    chooser.setInitialFileName(suggestedFilename);
                    chooser.getExtensionFilters()
                            .add(new FileChooser.ExtensionFilter("Calendario iCalendar (*.ics)", "*.ics"));
                    Window owner = statusMessageLabel.getScene() == null ? null
                            : statusMessageLabel.getScene().getWindow();
                    File file = NativeFiles.save(chooser, owner);
                    if (file == null) {
                        ViewSupport.success(statusMessageLabel, "Esportazione annullata: nessun file salvato.");
                        return;
                    }
                    try {
                        Files.writeString(file.toPath(), response.body(), StandardCharsets.UTF_8);
                        ViewSupport.success(statusMessageLabel, "File .ics salvato in: " + file.getAbsolutePath());
                    } catch (Exception ex) {
                        ViewSupport.error(statusMessageLabel, "Non è stato possibile salvare il file .ics.");
                    }
                }),
                () -> Platform.runLater(() -> ViewSupport.error(statusMessageLabel,
                        "Esportazione non riuscita. Verifica di essere loggato e che la prenotazione sia ancora attiva.")),
                null);
    }

    private void closeExportPopup() {
        if (exportPopupOverlay != null && statusMessageLabel.getScene() != null
                && statusMessageLabel.getScene().getRoot() instanceof StackPane root) {
            root.getChildren().remove(exportPopupOverlay);
        }
        exportPopupOverlay = null;
    }

    private void cancelBooking(BookingDTO dto) {
        if (dto == null || dto.getBookingId() == null) {
            ViewSupport.error(statusMessageLabel, "Seleziona una prenotazione valida da cancellare.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!BookingPresentation.canCancel(dto, now)) {
            ViewSupport.error(statusMessageLabel,
                    "Questa prenotazione non può più essere cancellata perché è già iniziata, terminata o annullata.");
            return;
        }
        StackPane popupRoot = PopupFactory.resolveRoot(statusMessageLabel);
        if (popupRoot == null) {
            ViewSupport.error(statusMessageLabel, "Impossibile aprire la conferma in questo momento.");
            return;
        }
        PopupFactory.showConfirmation(popupRoot,
                "Cancella prenotazione",
                "L'operazione non può essere annullata. I posti verranno rimessi a disposizione.",
                PopupFactory.Size.COMPACT,
                () -> {
                    ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/" + dto.getBookingId(), null,
                            ApiService.Method.DELETE,
                            response -> Platform.runLater(() -> {
                                ViewSupport.success(statusMessageLabel, "Prenotazione cancellata correttamente.");
                                loadBookings();
                            }),
                            message -> Platform.runLater(() -> ViewSupport.error(statusMessageLabel, message)),
                            null);
                });
    }

}
