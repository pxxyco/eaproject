package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.BookingDTO;
import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.PopupFactory;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BookingHistoryController {
    @FXML private StackPane pageRoot;
    @FXML private BorderPane mainContent;
    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;
    @FXML private Label historyTotalLabel;
    @FXML private Label historyCompletedLabel;
    @FXML private Label historyCancelledLabel;
    @FXML private Label historyReviewableLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> historyFilterCombo;
    @FXML private VBox historyCardsContainer;
    @FXML private VBox emptyStateContainer;
    @FXML private Label emptyTitleLabel;
    @FXML private Label emptyDescriptionLabel;

    private final List<BookingDTO> allBookings = new ArrayList<>();
    private final List<ReviewDTO> myReviews = new ArrayList<>();
    private boolean reviewsLoaded;
    private LocalDateTime evaluationTime = LocalDateTime.now();
    private PopupFactory.Handle activeModal;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH:mm", Locale.ITALY);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALY);

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        historyFilterCombo.getItems().setAll("Tutte", "Terminate / completate", "Cancellate", "Da recensire", "Con recensione");
        historyFilterCombo.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderHistory());
        historyFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> renderHistory());
        loadHistory();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException exception) {
            showMessage("Impossibile caricare la barra di navigazione.", true);
        }
    }

    private void loadHistory() {
        reviewsLoaded = false;
        myReviews.clear();
        updateCounters();
        renderHistory();
        showMessage("Caricamento dello storico...", false);
        ApiService.get(ApiService.BASE_URL + "/api/bookings/mine", response -> {
            try {
                Type type = new TypeToken<List<BookingDTO>>() {}.getType();
                List<BookingDTO> result = ApiService.fromJson(response.body(), type);
                Platform.runLater(() -> {
                    allBookings.clear();
                    if (result != null) allBookings.addAll(result);
                    evaluationTime = LocalDateTime.now();
                    updateCounters();
                    renderHistory();
                    showMessage(null, false);
                });
            } catch (Exception exception) {
                Platform.runLater(() -> showMessage("Il server ha restituito un elenco di prenotazioni non valido.", true));
            }
        }, () -> Platform.runLater(() -> showMessage("Impossibile caricare lo storico. Verifica che il backend sia avviato e riprova.", true)), null);

        ApiService.get(ApiService.BASE_URL + "/api/reviews/mine", response -> {
            try {
                Type type = new TypeToken<List<ReviewDTO>>() {}.getType();
                List<ReviewDTO> result = ApiService.fromJson(response.body(), type);
                Platform.runLater(() -> {
                    myReviews.clear();
                    if (result != null) myReviews.addAll(result);
                    reviewsLoaded = true;
                    updateCounters();
                    renderHistory();
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    reviewsLoaded = false;
                    updateCounters();
                    renderHistory();
                    showMessage("Non è stato possibile leggere le recensioni personali. Le azioni di recensione restano disabilitate per evitare doppioni; aggiorna il backend e riprova.", true);
                });
            }
        }, () -> Platform.runLater(() -> {
            reviewsLoaded = false;
            updateCounters();
            renderHistory();
            showMessage("Le recensioni personali non sono disponibili. Le azioni di recensione restano disabilitate finché il servizio non è raggiungibile.", true);
        }), null);
    }

    @FXML private void handleRefresh() { loadHistory(); }

    @FXML private void handleGoToBookings() {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/dashboard-view.fxml", null);
    }

    private void updateCounters() {
        evaluationTime = LocalDateTime.now();
        List<BookingDTO> history = allBookings.stream().filter(this::isHistorical).toList();
        historyTotalLabel.setText(String.valueOf(history.size()));
        historyCompletedLabel.setText(String.valueOf(history.stream().filter(b -> !isCancelledEntry(b) && isEndedOrCompleted(b)).count()));
        historyCancelledLabel.setText(String.valueOf(history.stream().filter(this::isCancelledEntry).count()));
        historyReviewableLabel.setText(reviewsLoaded
                ? String.valueOf(history.stream()
                        .filter(this::canReview)
                        .filter(b -> reviewFor(b) == null)
                        .map(this::reviewTargetKey)
                        .distinct()
                        .count())
                : "—");
    }

    private void renderHistory() {
        if (historyCardsContainer == null) return;
        evaluationTime = LocalDateTime.now();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ITALY);
        String filter = historyFilterCombo.getValue() == null ? "Tutte" : historyFilterCombo.getValue();
        List<BookingDTO> filtered = allBookings.stream()
                .filter(this::isHistorical)
                .filter(booking -> matchesSearch(booking, query))
                .filter(booking -> matchesFilter(booking, filter))
                .sorted(historyComparator())
                .toList();

        historyCardsContainer.getChildren().clear();
        for (BookingDTO booking : filtered) historyCardsContainer.getChildren().add(createBookingCard(booking));
        boolean empty = filtered.isEmpty();
        emptyStateContainer.setVisible(empty);
        emptyStateContainer.setManaged(empty);
        boolean hasAnyHistory = allBookings.stream().anyMatch(this::isHistorical);
        if (allBookings.isEmpty()) {
            emptyTitleLabel.setText("Non hai ancora effettuato prenotazioni");
            emptyDescriptionLabel.setText("Le prenotazioni concluse e quelle cancellate saranno raccolte qui.");
        } else if (!hasAnyHistory) {
            emptyTitleLabel.setText("Il tuo storico è ancora vuoto");
            emptyDescriptionLabel.setText("Le tue prenotazioni sono ancora in programma. Quando un'esperienza terminerà o una prenotazione verrà cancellata, troverai qui tutti i dettagli.");
        } else if (empty) {
            emptyTitleLabel.setText("Nessuna esperienza corrisponde ai filtri");
            emptyDescriptionLabel.setText("Prova a cambiare la ricerca o il filtro selezionato.");
        }
    }

    private VBox createBookingCard(BookingDTO booking) {
        VBox card = new VBox(11);
        card.getStyleClass().add("history-booking-card");
        if (isCancelledEntry(booking)) card.getStyleClass().add("history-cancelled-card");

        HBox top = new HBox(12);
        top.setAlignment(Pos.TOP_LEFT);
        VBox identity = new VBox(5);
        HBox.setHgrow(identity, Priority.ALWAYS);
        Label title = new Label(safe(booking.getItemTitle(), "Esperienza non disponibile"));
        title.getStyleClass().add("history-card-title");
        title.setWrapText(true);
        Label type = new Label(typeLabel(booking) + " · Prenotazione #" + safeId(booking));
        type.getStyleClass().add("history-card-meta");
        Label date = new Label("Periodo: " + eventPeriod(booking));
        date.getStyleClass().add("history-card-date");
        Label place = new Label("Luogo: " + safe(booking.getItemLocation(), "Non disponibile"));
        place.getStyleClass().add("history-card-meta");
        place.setWrapText(true);
        Label booked = new Label("Prenotata il: " + bookedAt(booking));
        booked.getStyleClass().add("history-card-meta");
        identity.getChildren().addAll(title, type, date, place, booked);

        Label status = new Label(displayStatus(booking));
        status.getStyleClass().setAll("history-status-pill", statusClass(booking));
        top.getChildren().addAll(identity, status);

        HBox lower = new HBox(14);
        lower.setAlignment(Pos.CENTER_LEFT);
        VBox financial = new VBox(4);
        Label price = new Label(formatPrice(booking.getPrice()));
        price.getStyleClass().add("history-card-price");
        Label seats = new Label((booking.getSeats() == null ? "—" : booking.getSeats()) + " post" + (Objects.equals(booking.getSeats(), 1) ? "o" : "i"));
        seats.getStyleClass().add("history-card-seats");
        financial.getChildren().addAll(price, seats);
        HBox.setHgrow(financial, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button details = new Button("Dettagli");
        details.getStyleClass().addAll("history-action-button", "history-detail-button");
        details.setOnAction(event -> showDetails(booking));
        actions.getChildren().add(details);
        ReviewDTO review = reviewFor(booking);
        if (canReview(booking) && reviewsLoaded) {
            Button reviewButton = new Button(review == null ? "Lascia recensione" : "Modifica recensione");
            reviewButton.getStyleClass().addAll("history-action-button", "history-review-button");
            reviewButton.setOnAction(event -> openReviewEditor(booking, review));
            actions.getChildren().add(reviewButton);
        } else if (review != null) {
            Label reviewed = new Label("★ Recensita");
            reviewed.getStyleClass().add("history-review-note");
            actions.getChildren().add(reviewed);
        }
        lower.getChildren().addAll(financial, actions);
        card.getChildren().addAll(top, new Separator(), lower);
        return card;
    }

    private void showDetails(BookingDTO booking) {
        VBox panel = modalShell("Dettaglio prenotazione", safe(booking.getItemTitle(), "Prenotazione #" + safeId(booking)));
        VBox content = new VBox(11);
        content.getStyleClass().add("history-dialog-content");
        content.getChildren().addAll(
                detailRow("Codice prenotazione", "#" + safeId(booking)),
                detailRow("Tipo", typeLabel(booking)),
                detailRow("Esperienza", safe(booking.getItemTitle(), "Non disponibile")),
                detailRow("Periodo", eventPeriod(booking)),
                detailRow("Luogo", safe(booking.getItemLocation(), "Non disponibile")),
                detailRow("Prenotata il", bookedAt(booking)),
                detailRow("Partecipante principale", safe(booking.getParticipantName(), "Non disponibile")),
                detailRow("Email partecipante", safe(booking.getParticipantEmail(), "Non disponibile")),
                detailRow("Posti / partecipanti", booking.getSeats() == null ? "Non specificati" : String.valueOf(booking.getSeats())),
                detailRow("Importo prenotazione", formatPrice(booking.getPrice())),
                detailRow("Stato prenotazione", bookingStatus(booking)),
                detailRow("Stato esperienza", itemStatus(booking)),
                detailRow("Stato mostrato", displayStatus(booking)),
                detailRow("Stato pagamento", paymentStatus(booking)),
                detailRow("Recensione", reviewSummary(reviewFor(booking)))
        );
        if (isCancelledEntry(booking)) {
            content.getChildren().add(detailRow("Rimborso", "Non tracciato: il modello attuale non gestisce lo stato dei rimborsi."));
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(390);
        scroll.getStyleClass().add("history-modal-scroll");
        panel.getChildren().add(scroll);
        if (canReview(booking) && reviewsLoaded) {
            Button reviewButton = new Button(reviewFor(booking) == null ? "Lascia una recensione" : "Modifica la recensione");
            reviewButton.getStyleClass().addAll("history-action-button", "history-review-button");
            reviewButton.setMaxWidth(Double.MAX_VALUE);
            reviewButton.setOnAction(event -> {
                closeActiveModal();
                openReviewEditor(booking, reviewFor(booking));
            });
            panel.getChildren().add(reviewButton);
        }
        panel.setMaxWidth(660);
        panel.setMaxHeight(640);
        showInAppModal(panel);
    }

    private VBox modalShell(String titleText, String subtitleText) {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("history-modal-panel");
        HBox heading = new HBox(12);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox headings = new VBox(4);
        HBox.setHgrow(headings, Priority.ALWAYS);
        Label title = new Label(titleText);
        title.getStyleClass().add("app-modal-title");
        Label subtitle = new Label(subtitleText == null ? "" : subtitleText);
        subtitle.getStyleClass().add("app-modal-subtitle");
        subtitle.setWrapText(true);
        headings.getChildren().addAll(title, subtitle);
        Button close = new Button("×");
        close.getStyleClass().add("app-modal-close");
        close.setAccessibleText("Chiudi popup");
        close.setOnAction(event -> closeActiveModal());
        heading.getChildren().addAll(headings, close);
        panel.getChildren().add(heading);
        return panel;
    }

    private void showInAppModal(VBox panel) {
        closeActiveModal();
        activeModal = PopupFactory.show(pageRoot, panel, PopupFactory.Size.MEDIUM);
    }

    private void closeActiveModal() {
        if (activeModal != null) {
            activeModal.close();
            activeModal = null;
        }
    }

    private HBox detailRow(String key, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        Label label = new Label(key);
        label.getStyleClass().add("history-detail-label");
        Label text = new Label(value == null ? "—" : value);
        text.getStyleClass().add("history-detail-value");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        row.getChildren().addAll(label, text);
        return row;
    }

    private void openReviewEditor(BookingDTO booking, ReviewDTO existing) {
        if (!reviewsLoaded) {
            showMessage("Le recensioni personali non sono ancora disponibili. Aggiorna lo storico e riprova.", true);
            return;
        }
        if (!canReview(booking)) {
            showMessage("Puoi recensire un'esperienza soltanto dopo che è terminata e se la prenotazione non è stata cancellata.", true);
            return;
        }
        VBox panel = modalShell(existing == null ? "Lascia una recensione" : "Modifica recensione",
                safe(booking.getItemTitle(), "La tua esperienza"));
        panel.setMaxWidth(560);
        panel.setMaxHeight(590);
        VBox body = new VBox(12);
        body.getStyleClass().add("history-dialog-content");
        Label explanation = new Label("Valuta la tua esperienza da 1 a 5 stelle e, se vuoi, aggiungi un commento (massimo 200 caratteri).");
        explanation.setWrapText(true);
        HBox stars = new HBox(6);
        stars.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup group = new ToggleGroup();
        List<ToggleButton> starButtons = new ArrayList<>();
        final int[] selectedRating = {existing == null || existing.getRating() == null ? 0 : existing.getRating()};
        for (int i = 1; i <= 5; i++) {
            ToggleButton star = new ToggleButton(i <= selectedRating[0] ? "★" : "☆");
            star.getStyleClass().add("history-star-button");
            star.setToggleGroup(group);
            star.setUserData(i);
            star.setTooltip(new Tooltip(i + " su 5 stelle"));
            star.setSelected(i == selectedRating[0]);
            starButtons.add(star);
            stars.getChildren().add(star);
        }
        Label ratingCaption = new Label(selectedRating[0] == 0 ? "Seleziona una valutazione" : selectedRating[0] + " / 5 stelle");
        for (ToggleButton star : starButtons) star.setOnAction(event -> {
            int rating = (Integer) star.getUserData();
            selectedRating[0] = rating;
            ratingCaption.setText(rating + " / 5 stelle");
            for (ToggleButton item : starButtons) item.setText((Integer) item.getUserData() <= rating ? "★" : "☆");
        });
        TextArea comment = new TextArea(existing == null ? "" : safe(existing.getComment(), ""));
        comment.setPromptText("Racconta com'è andata (facoltativo)...");
        comment.setWrapText(true);
        comment.setPrefRowCount(4);
        comment.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change ->
                change.getControlNewText().length() <= 200 ? change : null));
        Label counter = new Label(comment.getText().length() + " / 200 caratteri");
        counter.getStyleClass().add("history-form-counter");
        comment.textProperty().addListener((obs, oldValue, newValue) -> counter.setText(newValue.length() + " / 200 caratteri"));
        Label error = new Label();
        error.setWrapText(true);
        error.getStyleClass().add("history-modal-error");
        Button save = new Button(existing == null ? "Pubblica recensione" : "Salva modifiche");
        save.getStyleClass().addAll("history-action-button", "history-review-button");
        save.setMaxWidth(Double.MAX_VALUE);
        body.getChildren().addAll(explanation, stars, ratingCaption, comment, counter, error, save);

        if (existing != null && existing.getReviewId() != null) {
            Button delete = new Button("Elimina recensione");
            delete.getStyleClass().addAll("history-action-button", "history-delete-review-button");
            delete.setMaxWidth(Double.MAX_VALUE);
            delete.setOnAction(event -> {
                // Conferma in-app, mantenendo aperto il form e senza perdere la bozza.
                delete.setVisible(false);
                delete.setManaged(false);
                Label confirmationText = new Label("Vuoi eliminare definitivamente questa recensione?");
                confirmationText.getStyleClass().add("history-modal-error");
                HBox confirmationActions = new HBox(8);
                confirmationActions.setAlignment(Pos.CENTER_RIGHT);
                Button keep = new Button("Mantieni recensione");
                keep.getStyleClass().add("btn-secondary");
                Button confirmDelete = new Button("Elimina");
                confirmDelete.getStyleClass().add("history-delete-review-button");
                keep.setOnAction(e -> {
                    body.getChildren().removeAll(confirmationText, confirmationActions);
                    delete.setVisible(true);
                    delete.setManaged(true);
                });
                confirmDelete.setOnAction(e -> {
                    confirmDelete.setDisable(true);
                    ApiService.callDetailed(ApiService.BASE_URL + "/api/reviews/" + existing.getReviewId(), null,
                            ApiService.Method.DELETE,
                            response -> Platform.runLater(() -> {
                                closeActiveModal();
                                showMessage("Recensione eliminata.", false);
                                loadHistory();
                            }),
                            message -> Platform.runLater(() -> {
                                confirmDelete.setDisable(false);
                                error.setText(message);
                            }), null);
                });
                confirmationActions.getChildren().addAll(keep, confirmDelete);
                body.getChildren().addAll(confirmationText, confirmationActions);
            });
            body.getChildren().add(delete);
        }

        save.setOnAction(event -> {
            if (selectedRating[0] < 1 || selectedRating[0] > 5) {
                error.setText("Seleziona una valutazione da 1 a 5 stelle.");
                return;
            }
            save.setDisable(true);
            error.setText("");
            ReviewDTO payload = ReviewDTO.builder()
                    .rating(selectedRating[0])
                    .comment(comment.getText() == null || comment.getText().isBlank() ? null : comment.getText().trim())
                    .tripId(booking.getTripId())
                    .activityId(booking.getActivityId())
                    .build();
            String endpoint = existing == null ? ApiService.BASE_URL + "/api/reviews"
                    : ApiService.BASE_URL + "/api/reviews/" + existing.getReviewId();
            ApiService.callDetailed(endpoint, ApiService.toJson(payload),
                    existing == null ? ApiService.Method.POST : ApiService.Method.PUT,
                    response -> Platform.runLater(() -> {
                        closeActiveModal();
                        showMessage(existing == null ? "Recensione pubblicata correttamente." : "Recensione aggiornata correttamente.", false);
                        loadHistory();
                    }),
                    message -> Platform.runLater(() -> {
                        save.setDisable(false);
                        error.setText(message);
                    }), null);
        });
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(385);
        scroll.getStyleClass().add("history-modal-scroll");
        // Header remains visible; only the form body scrolls on small screens.
        panel.getChildren().add(scroll);
        showInAppModal(panel);
    }

    private boolean matchesSearch(BookingDTO booking, String query) {
        if (query.isBlank()) return true;
        return contains(booking.getItemTitle(), query)
                || contains(booking.getItemLocation(), query)
                || contains(eventPeriod(booking), query)
                || contains(typeLabel(booking), query)
                || contains(String.valueOf(booking.getBookingId()), query)
                || contains(bookedAt(booking), query);
    }

    private boolean matchesFilter(BookingDTO booking, String filter) {
        return switch (filter) {
            case "Terminate / completate" -> !isCancelledEntry(booking) && isEndedOrCompleted(booking);
            case "Cancellate" -> isCancelledEntry(booking);
            case "Da recensire" -> canReview(booking) && reviewFor(booking) == null;
            case "Con recensione" -> reviewFor(booking) != null;
            default -> true;
        };
    }

    private Comparator<BookingDTO> historyComparator() {
        return (left, right) -> {
            if (isCancelledEntry(left) != isCancelledEntry(right)) return isCancelledEntry(left) ? 1 : -1;
            LocalDateTime leftEnd = eventEndDateTime(left);
            LocalDateTime rightEnd = eventEndDateTime(right);
            if (leftEnd != null && rightEnd != null) {
                int byEndDate = rightEnd.compareTo(leftEnd);
                if (byEndDate != 0) return byEndDate;
            } else if (leftEnd != null) return -1;
            else if (rightEnd != null) return 1;
            if (left.getCreatedAt() != null && right.getCreatedAt() != null) return right.getCreatedAt().compareTo(left.getCreatedAt());
            return Long.compare(right.getBookingId() == null ? 0 : right.getBookingId(), left.getBookingId() == null ? 0 : left.getBookingId());
        };
    }

    private boolean isHistorical(BookingDTO booking) {
        return booking != null && (isCancelledEntry(booking) || isEndedOrCompleted(booking));
    }

    private boolean isEndedOrCompleted(BookingDTO booking) {
        return booking != null && ("COMPLETED".equalsIgnoreCase(booking.getBookingStatus())
                || "COMPLETED".equalsIgnoreCase(booking.getItemStatus())
                || (eventEndDateTime(booking) != null && !eventEndDateTime(booking).isAfter(evaluationTime)));
    }

    private boolean canReview(BookingDTO booking) {
        if (booking == null || isCancelledEntry(booking) || !("CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())
                || "COMPLETED".equalsIgnoreCase(booking.getBookingStatus()))) return false;
        return "COMPLETED".equalsIgnoreCase(booking.getBookingStatus())
                || "COMPLETED".equalsIgnoreCase(booking.getItemStatus())
                || (eventEndDateTime(booking) != null && !eventEndDateTime(booking).isAfter(LocalDateTime.now()));
    }

    private String reviewTargetKey(BookingDTO booking) {
        if (booking != null && booking.getTripId() != null) return "TRIP:" + booking.getTripId();
        if (booking != null && booking.getActivityId() != null) return "ACTIVITY:" + booking.getActivityId();
        return "BOOKING:" + safeId(booking);
    }

    private ReviewDTO reviewFor(BookingDTO booking) {
        if (booking == null) return null;
        return myReviews.stream().filter(review -> review != null && Boolean.FALSE.equals(review.getDeleted()))
                .filter(review -> booking.getTripId() != null ? booking.getTripId().equals(review.getTripId())
                        : booking.getActivityId() != null && booking.getActivityId().equals(review.getActivityId()))
                .findFirst().orElse(null);
    }

    private boolean isCancelled(BookingDTO booking) {
        return booking != null && "CANCELLED".equalsIgnoreCase(booking.getBookingStatus());
    }

    private boolean isExperienceCancelled(BookingDTO booking) {
        return booking != null && "CANCELLED".equalsIgnoreCase(booking.getItemStatus());
    }

    private boolean isCancelledEntry(BookingDTO booking) {
        return isCancelled(booking) || isExperienceCancelled(booking);
    }

    private String bookingStatus(BookingDTO booking) {
        if (booking == null || booking.getBookingStatus() == null) return "Non disponibile";
        return switch (booking.getBookingStatus().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "In attesa";
            case "CONFIRMED" -> "Confermata";
            case "COMPLETED" -> "Completata";
            case "CANCELLED" -> "Cancellata";
            default -> booking.getBookingStatus();
        };
    }

    private String itemStatus(BookingDTO booking) {
        if (booking == null || booking.getItemStatus() == null || booking.getItemStatus().isBlank()) return "Non disponibile";
        return switch (booking.getItemStatus().toUpperCase(Locale.ROOT)) {
            case "PUBLISHED" -> "Pubblicata";
            case "COMPLETED" -> "Completata";
            case "CANCELLED" -> "Annullata dall'organizzatore";
            case "DRAFT" -> "Bozza";
            default -> booking.getItemStatus();
        };
    }

    private LocalDateTime eventStartDateTime(BookingDTO booking) {
        return parseEventDate(booking == null ? null : booking.getItemDate(), booking);
    }

    private LocalDateTime eventEndDateTime(BookingDTO booking) {
        if (booking == null) return null;
        String end = booking.getItemEndDate();
        if (end == null || end.isBlank()) end = booking.getItemDate();
        return parseEventDate(end, booking);
    }

    private LocalDateTime parseEventDate(String value, BookingDTO booking) {
        if (value == null || value.isBlank()) return null;
        try {
            if (isTrip(booking) || !value.contains("T")) return LocalDate.parse(value).atTime(LocalTime.MAX);
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isTrip(BookingDTO booking) { return booking != null && "TRIP".equalsIgnoreCase(booking.getBookingType()); }

    private String eventPeriod(BookingDTO booking) {
        LocalDateTime start = eventStartDateTime(booking);
        LocalDateTime end = eventEndDateTime(booking);
        if (start == null) return "Data non disponibile";
        if (isTrip(booking)) {
            String first = start.toLocalDate().format(DATE);
            if (end == null || end.toLocalDate().equals(start.toLocalDate())) return first;
            return first + " – " + end.toLocalDate().format(DATE);
        }
        String first = start.format(DATE_TIME);
        if (end == null || end.equals(start)) return first;
        if (end.toLocalDate().equals(start.toLocalDate())) return first + " – " + end.format(TIME);
        return first + " – " + end.format(DATE_TIME);
    }

    private String bookedAt(BookingDTO booking) {
        return booking == null || booking.getCreatedAt() == null ? "Non disponibile" : booking.getCreatedAt().format(DATE_TIME);
    }

    private String displayStatus(BookingDTO booking) {
        if (booking == null) return "Non disponibile";
        String status = booking.getBookingStatus() == null ? "" : booking.getBookingStatus().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status)) return "Prenotazione cancellata";
        if (isExperienceCancelled(booking)) return "Esperienza annullata";
        if ("COMPLETED".equals(status) || "COMPLETED".equalsIgnoreCase(booking.getItemStatus())) return "Completata";
        if ("CONFIRMED".equals(status) && isEndedOrCompleted(booking)) return "Terminata";
        if ("PENDING".equals(status) && isEndedOrCompleted(booking)) return "Terminata · in attesa";
        if ("CONFIRMED".equals(status)) return "Confermata";
        if ("PENDING".equals(status)) return "In attesa";
        return status.isBlank() ? "Stato non disponibile" : status;
    }

    private String statusClass(BookingDTO booking) {
        String status = displayStatus(booking);
        if (status.startsWith("Prenotazione cancellata") || status.startsWith("Esperienza annullata")) return "history-status-cancelled";
        if ("Completata".equals(status) || "Terminata".equals(status)) return "history-status-completed";
        if ("Confermata".equals(status)) return "history-status-confirmed";
        if (status.contains("attesa")) return "history-status-pending";
        return "history-status-neutral";
    }

    private String paymentStatus(BookingDTO booking) {
        if (booking == null || booking.getPaymentStatus() == null || booking.getPaymentStatus().isBlank()) return "Nessun pagamento registrato";
        return switch (booking.getPaymentStatus().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "In attesa";
            case "CONFIRMED" -> "Confermato";
            case "COMPLETED" -> "Completato";
            case "CANCELLED" -> "Annullato";
            default -> booking.getPaymentStatus();
        };
    }

    private String reviewSummary(ReviewDTO review) {
        if (review == null) return "Nessuna recensione";
        int rating = review.getRating() == null ? 0 : review.getRating();
        return "★".repeat(Math.max(0, Math.min(5, rating))) + "☆".repeat(Math.max(0, 5 - rating))
                + (review.getComment() == null || review.getComment().isBlank() ? "" : " — " + review.getComment());
    }

    private String typeLabel(BookingDTO booking) { return isTrip(booking) ? "Viaggio" : "Attività"; }
    private String safeId(BookingDTO booking) { return booking == null || booking.getBookingId() == null ? "—" : String.valueOf(booking.getBookingId()); }
    private String safe(String text, String fallback) { return text == null || text.isBlank() ? fallback : text; }
    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ITALY).contains(query); }
    private String formatPrice(BigDecimal price) { return price == null ? "Importo non disponibile" : "€ " + String.format(Locale.ITALY, "%,.2f", price); }

    private void showMessage(String message, boolean error) {
        if (statusMessageLabel == null) return;
        boolean visible = message != null && !message.isBlank();
        statusMessageLabel.setText(visible ? message : "");
        statusMessageLabel.setVisible(visible);
        statusMessageLabel.setManaged(visible);
        statusMessageLabel.getStyleClass().removeAll("history-message-error", "history-message-success");
        if (visible) statusMessageLabel.getStyleClass().add(error ? "history-message-error" : "history-message-success");
    }
}
