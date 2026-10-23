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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class BookingsController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private Label totalBookingsLabel;
    @FXML private Label activeBookingsLabel;
    @FXML private Label historicalBookingsLabel;
    @FXML private Label spentTotalLabel;

    @FXML private TextField searchFilterField;
    @FXML private ComboBox<String> periodFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private TableView<BookingDTO> bookingsTableView;
    @FXML private TableColumn<BookingDTO, String> itemTitleColumn;
    @FXML private TableColumn<BookingDTO, String> dateColumn;
    @FXML private TableColumn<BookingDTO, String> bookedAtColumn;
    @FXML private TableColumn<BookingDTO, String> locationColumn;
    @FXML private TableColumn<BookingDTO, String> seatsColumn;
    @FXML private TableColumn<BookingDTO, String> priceColumn;
    @FXML private TableColumn<BookingDTO, String> statusColumn;
    @FXML private TableColumn<BookingDTO, Void> actionsColumn;

    private final ObservableList<BookingDTO> masterList = FXCollections.observableArrayList();
    private StackPane exportPopupOverlay;
    private TextField exportFileNameField;
    private BookingDTO pendingExportBooking;
    private LocalDateTime evaluationTime = LocalDateTime.now();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ITALY);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALY);

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
        periodFilterCombo.getItems().setAll("Tutte", "In programma", "Storico");
        periodFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getItems().setAll(
                "Tutti", "In attesa", "Confermate", "Completate / terminate", "Cancellate");
        statusFilterCombo.getSelectionModel().selectFirst();

        searchFilterField.textProperty().addListener((obs, oldV, newV) -> filterTable());
        periodFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterTable());
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> filterTable());
    }

    private void setupTableColumns() {
        itemTitleColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                safeText(cell.getValue().getItemTitle(), "Esperienza non disponibile")));
        dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatEventPeriod(cell.getValue())));
        bookedAtColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatBookedAt(cell.getValue())));
        locationColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                safeText(cell.getValue().getItemLocation(), "—")));
        seatsColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSeats() == null ? "—" : String.valueOf(cell.getValue().getSeats())));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPrice(cell.getValue().getPrice())));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(displayStatus(cell.getValue())));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                setGraphic(null);
                if (empty || status == null || status.isBlank()) return;
                badge.setText(status);
                badge.getStyleClass().setAll("label", "booking-status-pill", statusStyleClass(status));
                setGraphic(badge);
            }
        });

        bookingsTableView.setPlaceholder(new Label("Non hai ancora effettuato prenotazioni."));
        // Le prenotazioni future vengono mostrate prima dello storico; le cancellate restano in fondo.
        bookingsTableView.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(BookingDTO booking, boolean empty) {
                super.updateItem(booking, empty);
                getStyleClass().removeAll("cancelled-booking-row", "historical-booking-row");
                if (empty || booking == null) return;
                if (isCancelled(booking)) getStyleClass().add("cancelled-booking-row");
                else if (isHistorical(booking, evaluationTime)) getStyleClass().add("historical-booking-row");
            }
        });

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
                boolean canCancel = !cancelled && !isHistorical(dto, evaluationTime)
                        && !hasEventStarted(dto, evaluationTime)
                        && !"COMPLETED".equalsIgnoreCase(dto.getBookingStatus());
                exportIcsBtn.setVisible(!cancelled);
                exportIcsBtn.setManaged(!cancelled);
                cancelBtn.setVisible(canCancel);
                cancelBtn.setManaged(canCancel);
                setGraphic(container);
            }

            private BookingDTO currentRowBooking() {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(index);
            }
        });
    }

    private void loadBookings() {
        ApiService.get(ApiService.BASE_URL + "/api/bookings/mine",
                response -> {
                    try {
                        Type type = new TypeToken<List<BookingDTO>>() { }.getType();
                        List<BookingDTO> list = ApiService.fromJson(response.body(), type);
                        Platform.runLater(() -> {
                            evaluationTime = LocalDateTime.now();
                            masterList.setAll(list == null ? List.of() : list);
                            updateKPIs();
                            filterTable();
                        });
                    } catch (Exception exception) {
                        Platform.runLater(() -> showError("Il server ha restituito uno storico prenotazioni non valido."));
                    }
                },
                () -> Platform.runLater(() -> showError("Impossibile caricare le prenotazioni. Verifica la connessione e riprova.")),
                null
        );
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
        spentTotalLabel.setText(formatPrice(total));
    }

    private void filterTable() {
        evaluationTime = LocalDateTime.now();
        String query = searchFilterField.getText() == null
                ? "" : searchFilterField.getText().toLowerCase(Locale.ITALY).trim();
        String selectedStatus = statusFilterCombo.getValue();
        String selectedPeriod = periodFilterCombo.getValue();

        List<BookingDTO> filtered = masterList.stream()
                .filter(b -> matchesQuery(b, query))
                .filter(b -> matchesPeriod(b, selectedPeriod))
                .filter(b -> matchesStatus(b, selectedStatus))
                .sorted(this::compareBookings)
                .toList();

        bookingsTableView.setItems(FXCollections.observableArrayList(filtered));
        if (filtered.isEmpty()) {
            bookingsTableView.setPlaceholder(new Label(masterList.isEmpty()
                    ? "Non hai ancora effettuato prenotazioni."
                    : "Nessuna prenotazione corrisponde ai filtri selezionati."));
        }
    }

    private boolean matchesQuery(BookingDTO booking, String query) {
        if (query.isBlank()) return true;
        return contains(booking.getItemTitle(), query)
                || contains(booking.getItemLocation(), query)
                || contains(formatEventPeriod(booking), query)
                || contains(booking.getBookingType(), query)
                || contains(String.valueOf(booking.getBookingId()), query);
    }

    private boolean matchesPeriod(BookingDTO booking, String period) {
        if (period == null || "Tutte".equalsIgnoreCase(period)) return true;
        boolean historical = isHistorical(booking, evaluationTime);
        if ("Storico".equalsIgnoreCase(period)) return historical;
        if ("In programma".equalsIgnoreCase(period)) return !historical && !isCancelled(booking);
        return true;
    }

    private boolean matchesStatus(BookingDTO booking, String selectedStatus) {
        if (selectedStatus == null || "Tutti".equalsIgnoreCase(selectedStatus)) return true;
        String status = booking.getBookingStatus() == null ? "" : booking.getBookingStatus().toUpperCase(Locale.ROOT);
        return switch (selectedStatus) {
            case "In attesa" -> "PENDING".equals(status);
            case "Confermate" -> "CONFIRMED".equals(status);
            case "Completate / terminate" -> "COMPLETED".equals(status)
                    || (!"CANCELLED".equals(status) && isEventEnded(booking, evaluationTime));
            case "Cancellate" -> "CANCELLED".equals(status);
            default -> true;
        };
    }

    /** Upcoming first (nearest date first), then the most recent historical items; cancelled last. */
    private int compareBookings(BookingDTO left, BookingDTO right) {
        boolean leftCancelled = isCancelled(left);
        boolean rightCancelled = isCancelled(right);
        if (leftCancelled != rightCancelled) return leftCancelled ? 1 : -1;

        boolean leftHistory = isHistorical(left, evaluationTime);
        boolean rightHistory = isHistorical(right, evaluationTime);
        if (leftHistory != rightHistory) return leftHistory ? 1 : -1;

        LocalDateTime leftDate = leftHistory ? eventEndDateTime(left) : eventStartDateTime(left);
        LocalDateTime rightDate = rightHistory ? eventEndDateTime(right) : eventStartDateTime(right);
        if (leftDate != null && rightDate != null) {
            int dateComparison = leftHistory ? rightDate.compareTo(leftDate) : leftDate.compareTo(rightDate);
            if (dateComparison != 0) return dateComparison;
        } else if (leftDate != null) {
            return -1;
        } else if (rightDate != null) {
            return 1;
        }

        LocalDateTime leftCreated = left.getCreatedAt();
        LocalDateTime rightCreated = right.getCreatedAt();
        if (leftCreated != null && rightCreated != null) return rightCreated.compareTo(leftCreated);
        if (leftCreated != null) return -1;
        if (rightCreated != null) return 1;
        Long leftId = left.getBookingId();
        Long rightId = right.getBookingId();
        return Long.compare(rightId == null ? 0 : rightId, leftId == null ? 0 : leftId);
    }

    private boolean isHistorical(BookingDTO booking, LocalDateTime now) {
        if (booking == null) return false;
        return isCancelled(booking)
                || "COMPLETED".equalsIgnoreCase(booking.getBookingStatus())
                || isEventEnded(booking, now);
    }

    private boolean isEventEnded(BookingDTO booking, LocalDateTime now) {
        LocalDateTime end = eventEndDateTime(booking);
        return end != null && !end.isAfter(now);
    }

    private boolean hasEventStarted(BookingDTO booking, LocalDateTime now) {
        LocalDateTime start = eventStartDateTime(booking);
        return start != null && !start.isAfter(now);
    }

    private boolean isCancelled(BookingDTO booking) {
        return booking != null && "CANCELLED".equalsIgnoreCase(booking.getBookingStatus());
    }

    private LocalDateTime eventStartDateTime(BookingDTO booking) {
        if (booking == null || booking.getItemDate() == null || booking.getItemDate().isBlank()) return null;
        String value = booking.getItemDate().trim();
        try {
            if (isTrip(booking) || !value.contains("T")) return LocalDate.parse(value).atStartOfDay();
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDateTime eventEndDateTime(BookingDTO booking) {
        if (booking == null) return null;
        String value = booking.getItemEndDate();
        if (value == null || value.isBlank()) {
            // Graceful fallback for older backend responses without itemEndDate.
            LocalDateTime start = eventStartDateTime(booking);
            if (start == null) return null;
            return isTrip(booking) ? start.toLocalDate().atTime(LocalTime.MAX) : start.plusHours(1);
        }
        try {
            if (isTrip(booking) || !value.contains("T")) return LocalDate.parse(value).atTime(LocalTime.MAX);
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            LocalDateTime start = eventStartDateTime(booking);
            return start == null ? null : (isTrip(booking)
                    ? start.toLocalDate().atTime(LocalTime.MAX) : start.plusHours(1));
        }
    }

    private boolean isTrip(BookingDTO booking) {
        return booking != null && "TRIP".equalsIgnoreCase(booking.getBookingType());
    }

    private String formatEventPeriod(BookingDTO booking) {
        LocalDateTime start = eventStartDateTime(booking);
        LocalDateTime end = eventEndDateTime(booking);
        if (start == null) return "Data non disponibile";
        if (isTrip(booking)) {
            String formattedStart = start.toLocalDate().format(DATE_FORMAT);
            if (end == null || end.toLocalDate().equals(start.toLocalDate())) return formattedStart;
            return formattedStart + " – " + end.toLocalDate().format(DATE_FORMAT);
        }
        String formattedStart = start.format(DATE_TIME_FORMAT);
        if (end == null || end.equals(start)) return formattedStart;
        if (end.toLocalDate().equals(start.toLocalDate())) {
            return formattedStart + " – " + end.format(TIME_FORMAT);
        }
        return formattedStart + " – " + end.format(DATE_TIME_FORMAT);
    }

    private String formatBookedAt(BookingDTO booking) {
        return booking == null || booking.getCreatedAt() == null
                ? "—" : booking.getCreatedAt().format(DATE_TIME_FORMAT);
    }

    private String displayStatus(BookingDTO booking) {
        if (booking == null) return "—";
        String status = booking.getBookingStatus() == null ? "" : booking.getBookingStatus().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status)) return "Cancellata";
        if ("COMPLETED".equals(status)) return "Completata";
        if (isEventEnded(booking, evaluationTime)) return "Terminata";
        if ("CONFIRMED".equals(status)) return "Confermata";
        if ("PENDING".equals(status)) return "In attesa";
        return status.isBlank() ? "Stato non disponibile" : status;
    }

    private String statusStyleClass(String status) {
        return switch (status) {
            case "Cancellata" -> "booking-status-cancelled";
            case "Completata", "Terminata" -> "booking-status-completed";
            case "Confermata" -> "booking-status-confirmed";
            case "In attesa" -> "booking-status-pending";
            default -> "booking-status-neutral";
        };
    }

    private String formatPrice(BigDecimal amount) {
        return amount == null ? "€ 0,00" : "€ " + String.format(Locale.ITALY, "%,.2f", amount);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ITALY).contains(query);
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
        if (dto == null || dto.getBookingId() == null) {
            showError("Seleziona una prenotazione valida da cancellare.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (isCancelled(dto) || isHistorical(dto, now) || hasEventStarted(dto, now)
                || "COMPLETED".equalsIgnoreCase(dto.getBookingStatus())) {
            showError("Questa prenotazione non può più essere cancellata perché è già iniziata, terminata o annullata.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancella prenotazione");
        confirmation.setHeaderText("Vuoi cancellare questa prenotazione?");
        confirmation.setContentText("L'operazione non può essere annullata. I posti verranno rimessi a disposizione.");
        if (statusMessageLabel.getScene() != null) confirmation.initOwner(statusMessageLabel.getScene().getWindow());
        if (confirmation.showAndWait().filter(button -> button == ButtonType.OK).isEmpty()) return;

        ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/" + dto.getBookingId(), null,
                ApiService.Method.DELETE,
                response -> Platform.runLater(() -> {
                    showSuccess("Prenotazione cancellata correttamente.");
                    loadBookings();
                }),
                message -> Platform.runLater(() -> showError(message)),
                null);
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