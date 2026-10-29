package it.unical.ea_project_javafx.controller.organizer;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.controller.chat.ChatController;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.BookingPresentation;
import it.unical.ea_project_javafx.ui.UiText;
import static it.unical.ea_project_javafx.ui.TableSupport.textColumn;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class AttendeesController implements AutoCloseable {
    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private Label statusMessageLabel, totalAttendeesLabel, matchingLabel, selectionLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Event> eventFilterCombo;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Pagination pagination;
    @FXML
    private Button contactButton;
    @FXML
    private TableView<BookingDTO> attendeesTableView;
    @FXML
    private TableColumn<BookingDTO, String> participantColumn, emailColumn, eventColumn, seatsColumn, dateColumn,
            statusColumn, paymentColumn;
    private List<BookingDTO> rows = List.of(), filtered = List.of();
    private boolean closed;
    private long generation;

    private record Event(String type, Long id, String title) {
        @Override
        public String toString() {
            return title;
        }
    }

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        ViewSupport.loadNavbar(navbarContainer);
        textColumn(participantColumn, BookingDTO::getParticipantName);
        textColumn(emailColumn, BookingDTO::getParticipantEmail);
        textColumn(eventColumn, BookingDTO::getItemTitle);
        textColumn(seatsColumn, b -> Objects.toString(b.getSeats(), "—"));
        textColumn(dateColumn, BookingPresentation::formatEventPeriod);
        textColumn(statusColumn, b -> BookingPresentation.displayStatus(b, LocalDateTime.now()));
        statusColumn.setCellFactory(c -> BookingPresentation.statusCell());
        textColumn(paymentColumn, b -> BookingPresentation.paymentStatus(b.getPaymentStatus()));
        attendeesTableView.setPlaceholder(new Label("Nessuna prenotazione corrisponde ai filtri."));
        attendeesTableView.setFixedCellSize(58);
        attendeesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        statusColumn.setMinWidth(135);
        paymentColumn.setMinWidth(100);
        seatsColumn.setMinWidth(45);
        attendeesTableView.setRowFactory(t -> BookingPresentation.bookingRow(LocalDateTime::now));
        statusFilter.getItems().setAll("Tutti gli stati", "Attive", "In attesa", "Annullate", "Concluse");
        statusFilter.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((o, a, b) -> filter());
        eventFilterCombo.valueProperty().addListener((o, a, b) -> filter());
        statusFilter.valueProperty().addListener((o, a, b) -> filter());
        pagination.currentPageIndexProperty().addListener((o, a, b) -> page());
        attendeesTableView.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            contactButton.setDisable(b == null || b.getUserId() == null);
            selectionLabel.setText(b == null ? "Seleziona una prenotazione per contattare il partecipante."
                    : b.getParticipantName() + " · " + b.getItemTitle() + " · " + BookingPresentation.formatBookedAt(b)
                            + " · " + UiText.money(b.getPrice()));
        });
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        long version = ++generation;
        ApiService.callDetailed(ApiService.BASE_URL + "/api/bookings/organizer/mine", null, ApiService.Method.GET,
                r -> {
                    if (closed || version != generation)
                        return;
                    List<BookingDTO> loaded = ApiService.fromJson(r.body(), new TypeToken<List<BookingDTO>>() {
                    }.getType());
                    rows = loaded == null ? List.of() : loaded;
                    Event previous = eventFilterCombo.getValue();
                    eventFilterCombo.getItems().setAll(new Event("", null, "Tutte le esperienze"));
                    rows.stream()
                            .map(b -> new Event(b.getBookingType(),
                                    BookingPresentation.isTrip(b) ? b.getTripId() : b.getActivityId(),
                                    b.getItemTitle() + " · " + (BookingPresentation.isTrip(b) ? "Viaggio" : "Attività")
                                            + " · " + BookingPresentation.formatEventPeriod(b)))
                            .distinct().sorted(Comparator.comparing(Event::title))
                            .forEach(eventFilterCombo.getItems()::add);
                    if (previous != null && eventFilterCombo.getItems().contains(previous))
                        eventFilterCombo.setValue(previous);
                    else
                        eventFilterCombo.getSelectionModel().selectFirst();
                    filter();
                    ViewSupport.clear(statusMessageLabel);
                }, message -> ViewSupport.error(statusMessageLabel, message), null);
    }

    private void filter() {
        String q = searchField.getText().strip().toLowerCase(Locale.ROOT);
        Event event = eventFilterCombo.getValue();
        int state = statusFilter.getSelectionModel().getSelectedIndex();
        filtered = rows.stream()
                .filter(b -> event == null || event.id() == null
                        || (Objects.equals(event.type(), b.getBookingType()) && Objects.equals(event.id(),
                                BookingPresentation.isTrip(b) ? b.getTripId() : b.getActivityId())))
                .filter(b -> q.isBlank()
                        || (b.getParticipantName() + " " + b.getParticipantEmail() + " " + b.getItemTitle())
                                .toLowerCase(Locale.ROOT).contains(q))
                .filter(b -> switch (state) {
                    case 1 -> !BookingPresentation.isHistorical(b, LocalDateTime.now());
                    case 2 -> "PENDING".equals(b.getBookingStatus()) && !BookingPresentation.isCancelled(b);
                    case 3 -> BookingPresentation.isCancelled(b);
                    case 4 ->
                        BookingPresentation.isHistorical(b, LocalDateTime.now()) && !BookingPresentation.isCancelled(b);
                    default -> true;
                })
                .sorted(Comparator.comparing(BookingDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        totalAttendeesLabel.setText(String.valueOf(filtered.stream().filter(b -> !BookingPresentation.isCancelled(b))
                .mapToInt(b -> b.getSeats() == null ? 0 : b.getSeats()).sum()));
        matchingLabel
                .setText(filtered.size() + (filtered.size() == 1 ? " prenotazione trovata" : " prenotazioni trovate"));
        pagination.setCurrentPageIndex(0);
        pagination.setPageCount(Math.max(1, (filtered.size() + 14) / 15));
        page();
    }

    private void page() {
        attendeesTableView.getItems()
                .setAll(filtered.stream().skip(pagination.getCurrentPageIndex() * 15L).limit(15).toList());
        attendeesTableView.setPrefHeight(Math.min(530, Math.max(160, attendeesTableView.getItems().size() * 58 + 60)));
    }

    @FXML
    private void handleContact() {
        BookingDTO row = attendeesTableView.getSelectionModel().getSelectedItem();
        if (row != null && row.getUserId() != null)
            SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/chat/chat.fxml",
                    c -> ((ChatController) c).openTraveler(row.getUserId()));
    }

    @Override
    public void close() {
        closed = true;
        generation++;
    }
}
