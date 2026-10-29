package it.unical.ea_project_javafx.controller.traveler;

import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.booking.BookingDTO;
import it.unical.ea_project_javafx.dto.review.ReviewDTO;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.BookingPresentation;
import it.unical.ea_project_javafx.ui.UiText;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class BookingHistoryController implements AutoCloseable {
    @FXML
    private StackPane pageRoot;

    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private Label statusMessageLabel;
    @FXML
    private Label historyTotalLabel;
    @FXML
    private Label historyCompletedLabel;
    @FXML
    private Label historyCancelledLabel;
    @FXML
    private Label historyReviewableLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> historyFilterCombo;
    @FXML
    private VBox historyHeader;
    @FXML
    private StackPane historyHost;
    private final ListView<Object> historyList = new ListView<>();
    private final PauseTransition searchDelay = new PauseTransition(
            Duration.millis(280));
    private boolean loading, hasMore = true, closed;
    private int page;
    private long generation;
    @FXML
    private VBox emptyStateContainer;
    @FXML
    private Label emptyTitleLabel;

    private final List<BookingDTO> allBookings = new ArrayList<>();
    private final List<ReviewDTO> myReviews = new ArrayList<>();
    private boolean reviewsLoaded;
    private LocalDateTime evaluationTime = LocalDateTime.now();
    private PopupFactory.Handle activeModal;

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        historyFilterCombo.getItems().setAll("Tutte", "Concluse", "Cancellate", "Scadute", "Da recensire",
                "Con recensione");
        historyFilterCombo.getSelectionModel().selectFirst();
        historyHost.getChildren().clear();
        historyHost.getChildren().add(historyList);
        historyList.getStyleClass().add("history-feed");
        historyList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null
                        : item instanceof BookingDTO booking ? createBookingCard(booking) : (Node) item);
            }
        });
        historyList.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() < 0)
                Platform.runLater(this::loadNearEnd);
        });
        searchDelay.setOnFinished(event -> loadHistory());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchDelay.playFromStart());
        historyFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadHistory());
        loadHistory();
    }

    private void loadNavbar() {
        ViewSupport.loadNavbar(navbarContainer, message -> showMessage(message, true));
    }

    private void loadHistory() {
        evaluationTime = LocalDateTime.now();
        generation++;
        page = 0;
        hasMore = true;
        loading = false;
        allBookings.clear();
        myReviews.clear();
        reviewsLoaded = false;
        historyList.getItems().setAll(historyHeader);
        historyList.scrollTo(0);
        loadPage();
    }

    private void loadNearEnd() {
        if (closed || loading || !hasMore)
            return;
        historyList.layout();
        Node viewport = historyList.lookup(".clipped-container");
        if (viewport == null)
            return;
        var visibleArea = viewport.localToScene(viewport.getBoundsInLocal());
        boolean near = historyList.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof ListCell<?> cell && !cell.isEmpty()
                        && cell.getIndex() >= historyList.getItems().size() - 2)
                .anyMatch(node -> {
                    boolean inViewport = false;
                    for (Node parent = node; parent != null; parent = parent.getParent()) {
                        if (!parent.isVisible())
                            return false;
                        if (parent == viewport) {
                            inViewport = true;
                            break;
                        }
                    }
                    if (!inViewport)
                        return false;
                    var bounds = node.localToScene(node.getBoundsInLocal());
                    return bounds.getMinY() <= visibleArea.getMaxY() + 40
                            && bounds.getMaxY() >= visibleArea.getMinY();
                });
        if (near)
            loadPage();
    }

    private void loadPage() {
        if (closed || loading || !hasMore)
            return;
        loading = true;
        long request = generation;
        String query = URLEncoder.encode(searchField.getText().strip(),
                StandardCharsets.UTF_8);
        String filter = switch (historyFilterCombo.getValue()) {
            case "Concluse" -> "COMPLETED";
            case "Cancellate" -> "CANCELLED";
            case "Scadute" -> "EXPIRED";
            case "Da recensire" -> "REVIEWABLE";
            case "Con recensione" -> "REVIEWED";
            default -> "ALL";
        };
        showMessage("Caricamento dello storico…", false);
        ApiService.get(
                ApiService.BASE_URL + "/api/bookings/mine/history?page=" + page + "&size=6&filter=" + filter + "&query="
                        + query,
                response -> {
                    if (closed || request != generation)
                        return;
                    try {
                        var data = JsonParser.parseString(response.body()).getAsJsonObject();
                        List<BookingDTO> items = ApiService.fromJson(data.get("items").toString(),
                                new TypeToken<List<BookingDTO>>() {
                                }.getType());
                        List<ReviewDTO> reviews = ApiService.fromJson(data.get("reviews").toString(),
                                new TypeToken<List<ReviewDTO>>() {
                                }.getType());
                        for (ReviewDTO review : reviews)
                            if (myReviews.stream()
                                    .noneMatch(r -> Objects.equals(r.getReviewId(), review.getReviewId())))
                                myReviews.add(review);
                        allBookings.addAll(items);
                        reviewsLoaded = true;
                        historyList.getItems().addAll(items);
                        hasMore = data.get("hasMore").getAsBoolean();
                        page++;
                        loading = false;
                        historyTotalLabel.setText(data.get("total").getAsString());
                        historyCompletedLabel.setText(data.get("completed").getAsString());
                        historyCancelledLabel.setText(data.get("cancelled").getAsString());
                        historyReviewableLabel.setText(data.get("reviewable").getAsString());
                        if (allBookings.isEmpty()) {
                            emptyTitleLabel.setText(data.get("total").getAsLong() == 0 ? "Il tuo storico è ancora vuoto"
                                    : "Nessuna esperienza corrisponde ai filtri");
                            emptyStateContainer.setVisible(true);
                            emptyStateContainer.setManaged(true);
                            historyList.getItems().add(emptyStateContainer);
                        }
                        showMessage(null, false);
                    } catch (RuntimeException ex) {
                        loading = false;
                        showMessage("Storico non disponibile. Premi Aggiorna per riprovare.", true);
                    }
                }, () -> {
                    if (!closed && request == generation) {
                        loading = false;
                        showMessage("Caricamento non riuscito. Scorri o premi Aggiorna per riprovare.", true);
                    }
                }, null);
    }

    @Override
    public void close() {
        closed = true;
        generation++;
        searchDelay.stop();
        if (activeModal != null)
            activeModal.close();
    }

    @FXML
    private void handleRefresh() {
        loadHistory();
    }

    @FXML
    private void handleGoToBookings() {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/dashboard-view.fxml", null);
    }

    private VBox createBookingCard(BookingDTO booking) {
        VBox card = new VBox(11);
        card.getStyleClass().add("history-booking-card");
        if (isCancelledEntry(booking))
            card.getStyleClass().add("history-cancelled-card");

        HBox top = new HBox(12);
        top.setAlignment(Pos.TOP_LEFT);
        VBox identity = new VBox(5);
        HBox.setHgrow(identity, Priority.ALWAYS);
        Label title = new Label(UiText.orDefault(booking.getItemTitle(), "Esperienza non disponibile"));
        title.getStyleClass().add("history-card-title");
        title.setWrapText(true);
        Label type = new Label(typeLabel(booking));
        type.getStyleClass().add("history-card-meta");
        Label date = new Label("Periodo: " + BookingPresentation.formatEventPeriod(booking));
        date.getStyleClass().add("history-card-date");
        Label place = new Label("Luogo: " + UiText.orDefault(booking.getItemLocation(), "Non disponibile"));
        place.getStyleClass().add("history-card-meta");
        place.setWrapText(true);
        Label booked = new Label("Prenotata il: " + BookingPresentation.formatBookedAt(booking));
        booked.getStyleClass().add("history-card-meta");
        identity.getChildren().addAll(title, type, date, place, booked);

        Label status = new Label(BookingPresentation.displayStatus(booking, evaluationTime));
        status.getStyleClass().setAll("history-status-pill", statusClass(booking));
        top.getChildren().addAll(identity, status);

        HBox lower = new HBox(14);
        lower.setAlignment(Pos.CENTER_LEFT);
        VBox financial = new VBox(4);
        Label price = new Label(UiText.money(booking.getPrice()));
        price.getStyleClass().add("history-card-price");
        Label seats = new Label((booking.getSeats() == null ? "—" : booking.getSeats()) + " post"
                + (Objects.equals(booking.getSeats(), 1) ? "o" : "i"));
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
        VBox panel = modalShell("Dettaglio prenotazione",
                UiText.orDefault(booking.getItemTitle(), "Esperienza prenotata"));
        VBox content = new VBox(11);
        content.getStyleClass().add("history-dialog-content");
        content.getChildren().addAll(
                detailRow("Tipo", typeLabel(booking)),
                detailRow("Esperienza", UiText.orDefault(booking.getItemTitle(), "Non disponibile")),
                detailRow("Periodo", BookingPresentation.formatEventPeriod(booking)),
                detailRow("Luogo", UiText.orDefault(booking.getItemLocation(), "Non disponibile")),
                detailRow("Prenotata il", BookingPresentation.formatBookedAt(booking)),
                detailRow("Partecipante principale", UiText.orDefault(booking.getParticipantName(), "Non disponibile")),
                detailRow("Email partecipante", UiText.orDefault(booking.getParticipantEmail(), "Non disponibile")),
                detailRow("Posti / partecipanti",
                        booking.getSeats() == null ? "Non specificati" : String.valueOf(booking.getSeats())),
                detailRow("Importo prenotazione", UiText.money(booking.getPrice())),
                detailRow("Stato prenotazione", bookingStatus(booking)),
                detailRow("Stato esperienza", itemStatus(booking)),
                detailRow("Stato mostrato", BookingPresentation.displayStatus(booking, evaluationTime)),
                detailRow("Stato pagamento", paymentStatus(booking)),
                detailRow("Recensione", reviewSummary(reviewFor(booking))));
        if (isCancelledEntry(booking)) {
            content.getChildren().add(
                    detailRow("Rimborso", "Non tracciato: il modello attuale non gestisce lo stato dei rimborsi."));
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(390);
        scroll.getStyleClass().add("history-modal-scroll");
        panel.getChildren().add(scroll);
        if (canReview(booking) && reviewsLoaded) {
            Button reviewButton = new Button(
                    reviewFor(booking) == null ? "Lascia una recensione" : "Modifica la recensione");
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
            showMessage(
                    "Puoi recensire un'esperienza soltanto dopo che è terminata e se la prenotazione non è stata cancellata.",
                    true);
            return;
        }
        VBox panel = modalShell(existing == null ? "Lascia una recensione" : "Modifica recensione",
                UiText.orDefault(booking.getItemTitle(), "La tua esperienza"));
        panel.setMaxWidth(560);
        panel.setMaxHeight(590);
        VBox body = new VBox(12);
        body.getStyleClass().add("history-dialog-content");
        Label explanation = new Label(
                "Valuta la tua esperienza da 1 a 5 stelle e, se vuoi, aggiungi un commento (massimo 200 caratteri).");
        explanation.setWrapText(true);
        HBox stars = new HBox(6);
        stars.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup group = new ToggleGroup();
        List<ToggleButton> starButtons = new ArrayList<>();
        final int[] selectedRating = { existing == null || existing.getRating() == null ? 0 : existing.getRating() };
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
        Label ratingCaption = new Label(
                selectedRating[0] == 0 ? "Seleziona una valutazione" : selectedRating[0] + " / 5 stelle");
        for (ToggleButton star : starButtons)
            star.setOnAction(event -> {
                int rating = (Integer) star.getUserData();
                selectedRating[0] = rating;
                ratingCaption.setText(rating + " / 5 stelle");
                for (ToggleButton item : starButtons)
                    item.setText((Integer) item.getUserData() <= rating ? "★" : "☆");
            });
        TextArea comment = new TextArea(existing == null ? "" : UiText.orDefault(existing.getComment(), ""));
        comment.setPromptText("Racconta com'è andata (facoltativo)...");
        comment.setWrapText(true);
        comment.setPrefRowCount(4);
        comment.setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().length() <= 200 ? change : null));
        Label counter = new Label(comment.getText().length() + " / 200 caratteri");
        counter.getStyleClass().add("history-form-counter");
        comment.textProperty()
                .addListener((obs, oldValue, newValue) -> counter.setText(newValue.length() + " / 200 caratteri"));
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
                        showMessage(existing == null ? "Recensione pubblicata correttamente."
                                : "Recensione aggiornata correttamente.", false);
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
        panel.getChildren().add(scroll);
        showInAppModal(panel);
    }

    private boolean canReview(BookingDTO booking) {
        return BookingPresentation.canReview(booking, LocalDateTime.now());
    }

    private ReviewDTO reviewFor(BookingDTO booking) {
        if (booking == null)
            return null;
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
        return BookingPresentation.displayStatus(booking, evaluationTime);
    }

    private String itemStatus(BookingDTO booking) {
        if (booking == null || booking.getItemStatus() == null || booking.getItemStatus().isBlank())
            return "Non disponibile";
        return switch (booking.getItemStatus().toUpperCase(Locale.ROOT)) {
            case "PUBLISHED" -> "Pubblicata";
            case "COMPLETED" -> "Conclusa";
            case "CANCELLED" -> "Annullata dall'organizzatore";
            case "DRAFT" -> "Bozza";
            default -> booking.getItemStatus();
        };
    }

    private String statusClass(BookingDTO booking) {
        String status = BookingPresentation.displayStatus(booking, evaluationTime);
        if (status.startsWith("Prenotazione cancellata") || status.startsWith("Esperienza annullata"))
            return "history-status-cancelled";
        if ("Conclusa".equals(status))
            return "history-status-completed";
        if ("Confermata".equals(status))
            return "history-status-confirmed";
        if (status.contains("attesa") || "Scaduta".equals(status) || "Da pagare".equals(status))
            return "history-status-pending";
        return "history-status-neutral";
    }

    private String paymentStatus(BookingDTO booking) {
        if (booking == null) return "Non disponibile";
        if (booking.getPrice() != null && booking.getPrice().signum() == 0) return "Non dovuto";
        return BookingPresentation.paymentStatus(booking.getPaymentStatus());
    }

    private String reviewSummary(ReviewDTO review) {
        if (review == null)
            return "Nessuna recensione";
        int rating = review.getRating() == null ? 0 : review.getRating();
        return "★".repeat(Math.max(0, Math.min(5, rating))) + "☆".repeat(Math.max(0, 5 - rating))
                + (review.getComment() == null || review.getComment().isBlank() ? "" : " — " + review.getComment());
    }

    private String typeLabel(BookingDTO booking) {
        return BookingPresentation.isTrip(booking) ? "Viaggio" : "Attività";
    }

    private void showMessage(String message, boolean error) {
        if (statusMessageLabel == null)
            return;
        boolean visible = message != null && !message.isBlank();
        statusMessageLabel.setText(visible ? message : "");
        statusMessageLabel.setVisible(visible);
        statusMessageLabel.setManaged(visible);
        statusMessageLabel.getStyleClass().removeAll("history-message-error", "history-message-success");
        if (visible)
            statusMessageLabel.getStyleClass().add(error ? "history-message-error" : "history-message-success");
    }
}
