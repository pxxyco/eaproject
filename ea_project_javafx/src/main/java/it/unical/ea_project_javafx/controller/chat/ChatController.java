package it.unical.ea_project_javafx.controller.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.service.chat.ChatEventHub;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.animation.PageTransitions;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.util.JsonCodec;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ChatController implements AutoCloseable {
    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane pageRoot, navbarContainer;
    @FXML
    private ListView<JsonObject> conversations;
    @FXML
    private VBox messagesBox;
    @FXML
    private ScrollPane messagesScroll;
    @FXML
    private TextArea composer;
    @FXML
    private Label connectionLabel, statusLabel, conversationTitle, conversationSubtitle, characterCount;
    @FXML
    private Button sendButton, olderButton, latestButton, moreConversations;
    private AutoCloseable stream;
    private JsonObject selected;
    private boolean closed, updatingInbox, inboxBusy, messagesBusy, sending, messagesPending, inboxPending;
    private final PauseTransition settleScroll = new PauseTransition(
            Duration.millis(60));
    private int inboxPage;
    private boolean compactMode;

    public ChatController() {
    }

    public ChatController(boolean compact) {
        compactMode = compact;
    }

    private long selectedId, requestGeneration, lastRead, renderedConversation;
    private Long renderedMaximum;
    private boolean latest = true;
    private boolean followLatest = true;
    private Long oldest, maximum;
    private String pendingBody, pendingId;
    private final Map<Long, String> drafts = new HashMap<>();
    private static final String API = ApiService.BASE_URL + "/api/chat";

    @FXML
    public void initialize() {
        if (!compactMode)
            BackgroundHelper.setupBackground(bgImageView);
        if (!compactMode)
            ViewSupport.loadNavbar(navbarContainer, this::show);
        conversations.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JsonObject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String unread = JsonCodec.string(item, "unread", "");
                setText(JsonCodec.string(item, "name", "") + (unread.equals("0") ? "" : "  • " + unread) + "\n"
                        + JsonCodec.string(item, "preview", ""));
            }
        });
        conversations.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> {
            if (!updatingInbox && value != null)
                select(value);
        });
        composer.setTextFormatter(
                new TextFormatter<String>(change -> change.getControlNewText().length() <= 4000 ? change : null));
        composer.textProperty().addListener((o, old, value) -> {
            characterCount.setText(value.length() + " / 4000");
            updateSend();
        });
        composer.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                handleSend();
                e.consume();
            }
        });
        messagesBox.layoutBoundsProperty().addListener((o, a, b) -> {
            if (latest && followLatest && Math.abs(a.getHeight() - b.getHeight()) > 1)
                Platform.runLater(() -> {
                    if (!closed && latest && followLatest) {
                        messagesScroll.setVvalue(1);
                        markRead();
                    }
                });
        });
        messagesScroll.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaY() > 0)
                followLatest = false;
        });
        messagesScroll.vvalueProperty().addListener((o, a, b) -> {
            if (b.doubleValue() > .98) {
                followLatest = true;
                markRead();
            }
        });
        pageRoot.sceneProperty().addListener((o, old, scene) -> {
            if (scene != null)
                scene.windowProperty().addListener((w, previous, window) -> {
                    if (window != null)
                        window.focusedProperty().addListener((f, was, is) -> {
                            if (is)
                                markRead();
                        });
                });
        });
        updateSend();
        loadInbox(false);
        stream = ChatEventHub.subscribe(id -> {
            loadInbox(false);
            if (selectedId != 0 && (id == -1 || id == selectedId)) {
                if (latest && followLatest)
                    loadMessages(null);
                else
                    show("Nuovi aggiornamenti. Premi “Più recenti” per tornare alla conversazione.");
            }
        }, connectionLabel::setText);
    }

    public void openConversation(long id) {
        ApiService.callDetailed(API + "/conversations/" + id, null, ApiService.Method.GET, r -> {
            if (!closed)
                select(JsonParser.parseString(r.body()).getAsJsonObject());
        }, this::show, null);
    }

    public void compact() {
        compactMode = true;
        bgImageView.setVisible(false);
        bgImageView.setManaged(false);
        pageRoot.getChildren().stream().filter(n -> n.getStyleClass().contains("app-background-shade"))
                .forEach(n -> {
                    n.setVisible(false);
                    n.setManaged(false);
                });
        pageRoot.setStyle("-fx-background-color: transparent;");
        navbarContainer.getChildren().clear();
        if (connectionLabel.getParent() instanceof Pane old)
            old.getChildren().remove(connectionLabel);
        if (conversationTitle.getParent() instanceof VBox header)
            header.getChildren().add(2, connectionLabel);
        navbarContainer.setVisible(false);
        navbarContainer.setManaged(false);
        var sidebar = conversations.getParent();
        sidebar.setVisible(false);
        sidebar.setManaged(false);
        pageRoot.setPrefSize(660, 570);
        pageRoot.setMinSize(0, 0);
        pageRoot.setMaxSize(660, 610);
        pageRoot.getStyleClass().add("chat-compact");
    }

    public void openOrganizer(Long organizer) {
        if (organizer == null)
            return;
        ApiService.callDetailed(API + "/conversations", ApiService.toJson(Map.of("organizerId", organizer)),
                ApiService.Method.POST, r -> {
                    if (closed)
                        return;
                    JsonObject conversation = JsonParser.parseString(r.body()).getAsJsonObject();
                    select(conversation);
                    loadInbox(false);
                }, this::show, null);
    }

    @FXML
    private void handleRefresh() {
        loadInbox(false);
        if (selectedId != 0)
            loadMessages(null);
    }

    @FXML
    private void handleMoreConversations() {
        loadInbox(true);
    }

    private void loadInbox(boolean append) {
        if (compactMode)
            return;
        if (closed)
            return;
        if (inboxBusy) {
            if (!append)
                inboxPending = true;
            return;
        }
        inboxBusy = true;
        int page = append ? inboxPage + 1 : 0;
        ApiService.callDetailed(API + "/conversations?page=" + page, null, ApiService.Method.GET, r -> {
            inboxBusy = false;
            if (closed)
                return;
            try {
                JsonArray array = JsonParser.parseString(r.body()).getAsJsonArray();
                LinkedHashMap<Long, JsonObject> rows = new LinkedHashMap<>();
                if (append)
                    conversations.getItems().forEach(c -> rows.put(c.get("id").getAsLong(), c));
                for (var entry : array) {
                    var c = entry.getAsJsonObject();
                    rows.put(c.get("id").getAsLong(), c);
                }
                updatingInbox = true;
                conversations.getItems().setAll(rows.values());
                if (selectedId != 0)
                    rows.values().stream().filter(c -> c.get("id").getAsLong() == selectedId).findFirst()
                            .ifPresent(c -> {
                                selected = c;
                                conversations.getSelectionModel().select(c);
                            });
                updatingInbox = false;
                inboxPage = page;
                moreConversations.setDisable(array.size() < 30);
                if (rows.isEmpty())
                    show("Non hai ancora conversazioni. Per iniziare apri un'esperienza e contatta il suo organizzatore.");
            } catch (Exception ex) {
                updatingInbox = false;
                show("Elenco conversazioni non valido. Riprova.");
            }
            if (inboxPending) {
                inboxPending = false;
                loadInbox(false);
            }
        }, message -> {
            inboxBusy = false;
            show(message);
        }, null);
    }

    private void select(JsonObject conversation) {
        if (sending)
            return;
        if (selectedId != 0)
            drafts.put(selectedId, composer.getText());
        selected = conversation;
        followLatest = true;
        selectedId = conversation.get("id").getAsLong();
        requestGeneration++;
        lastRead = 0;
        messagesBusy = false;
        messagesPending = false;
        conversationTitle.setText(JsonCodec.string(conversation, "name", ""));
        conversationSubtitle.setText(
                JsonCodec.string(conversation, "role", "").equals("ORGANIZER") ? "Organizzatore" : "Viaggiatore");
        composer.setText(drafts.getOrDefault(selectedId, ""));
        pendingBody = null;
        pendingId = null;
        messagesBox.getChildren().clear();
        statusLabel.setText("");
        updateSend();
        loadMessages(null);
    }

    @FXML
    private void handleOlder() {
        if (oldest != null)
            loadMessages(oldest);
    }

    @FXML
    private void handleLatest() {
        followLatest = true;
        loadMessages(null);
    }

    private void loadMessages(Long before) {
        if (closed || selectedId == 0)
            return;
        if (messagesBusy) {
            if (before == null)
                messagesPending = true;
            return;
        }
        messagesBusy = true;
        long conversation = selectedId, version = requestGeneration;
        ApiService.callDetailed(
                API + "/conversations/" + conversation + "/messages" + (before == null ? "" : "?before=" + before),
                null, ApiService.Method.GET, r -> {
                    if (closed || version != requestGeneration)
                        return;
                    messagesBusy = false;
                    try {
                        JsonArray page = JsonParser.parseString(r.body()).getAsJsonArray();
                        List<JsonObject> ordered = new ArrayList<>();
                        page.forEach(e -> ordered.add(e.getAsJsonObject()));
                        Collections.reverse(ordered);
                        latest = before == null;
                        oldest = ordered.isEmpty() ? null : ordered.getFirst().get("id").getAsLong();
                        maximum = ordered.isEmpty() ? null : ordered.getLast().get("id").getAsLong();
                        olderButton.setDisable(ordered.size() < 50);
                        latestButton.setDisable(latest);
                        messagesBox.getChildren().clear();
                        for (var message : ordered) {
                            HBox row = bubble(message);
                            messagesBox.getChildren().add(row);
                            if (before == null && renderedConversation == conversation
                                    && (renderedMaximum == null || message.get("id").getAsLong() > renderedMaximum))
                                PageTransitions.appear(row);
                        }
                        renderedConversation = conversation;
                        renderedMaximum = maximum;
                        if (ordered.isEmpty())
                            messagesBox.getChildren().add(new Label("Scrivi il primo messaggio."));
                        if (latest && followLatest)
                            Platform.runLater(() -> {
                                messagesScroll.applyCss();
                                messagesScroll.layout();
                                messagesScroll.setVvalue(1);
                                settleScroll.setOnFinished(e -> {
                                    if (!closed && selectedId == conversation) {
                                        messagesScroll.setVvalue(1);
                                        markRead();
                                    }
                                });
                                settleScroll.playFromStart();
                            });
                        if (messagesPending) {
                            messagesPending = false;
                            loadMessages(null);
                        }
                    } catch (Exception ex) {
                        show("Cronologia non disponibile. Riprova.");
                    }
                }, message -> {
                    if (version == requestGeneration) {
                        messagesBusy = false;
                        show(message);
                    }
                }, null);
    }

    private HBox bubble(JsonObject message) {
        boolean mine = message.get("senderId").getAsLong() == UserSession.getInstance().getId();
        Label body = new Label(JsonCodec.string(message, "body", ""));
        body.setWrapText(true);
        body.getStyleClass().add("chat-message-text");
        Label timestamp = new Label(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
                .format(Instant.parse(JsonCodec.string(message, "sentAt", ""))));
        timestamp.getStyleClass().add("chat-message-time");
        VBox box = new VBox(6, body, timestamp);
        box.getStyleClass().add(mine ? "chat-message-own" : "chat-message-other");
        box.setMaxWidth(520);
        body.maxWidthProperty().bind(messagesBox.widthProperty().multiply(.72));
        HBox row = new HBox(box);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    @FXML
    private void handleSend() {
        String body = composer.getText().strip();
        if (closed || sending || selectedId == 0 || body.isBlank())
            return;
        if (!body.equals(pendingBody)) {
            pendingBody = body;
            pendingId = UUID.randomUUID().toString();
        }
        sending = true;
        conversations.setDisable(true);
        composer.setDisable(true);
        updateSend();
        show("Invio in corso…");
        long conversation = selectedId;
        ApiService.callDetailed(API + "/conversations/" + conversation + "/messages",
                ApiService.toJson(Map.of("clientId", pendingId, "body", body)), ApiService.Method.POST, r -> {
                    if (closed)
                        return;
                    sending = false;
                    conversations.setDisable(false);
                    composer.setDisable(false);
                    composer.clear();
                    drafts.remove(conversation);
                    pendingBody = null;
                    pendingId = null;
                    show("");
                    updateSend();
                    loadMessages(null);
                    loadInbox(false);
                }, message -> {
                    if (closed)
                        return;
                    sending = false;
                    conversations.setDisable(false);
                    composer.setDisable(false);
                    updateSend();
                    show(message + " Il testo è conservato: puoi ritentare l'invio.");
                }, null);
    }

    private void markRead() {
        if (closed || !latest || maximum == null || maximum <= lastRead || selectedId == 0
                || pageRoot.getScene() == null || pageRoot.getScene().getWindow() == null
                || !pageRoot.getScene().getWindow().isFocused() || messagesScroll.getVvalue() < .98)
            return;
        long through = maximum, conversation = selectedId;
        lastRead = through;
        ApiService.callDetailed(API + "/conversations/" + conversation + "/read",
                ApiService.toJson(Map.of("throughId", through)), ApiService.Method.POST, r -> loadInbox(false),
                message -> {
                    if (selectedId == conversation)
                        lastRead = 0;
                }, null);
    }

    private void updateSend() {
        sendButton.setDisable(sending || selectedId == 0 || composer.getText().isBlank());
    }

    private void show(String message) {
        if (!closed)
            statusLabel.setText(message == null ? "" : message);
    }

    @Override
    public void close() {
        closed = true;
        settleScroll.stop();
        if (stream != null)
            try {
                stream.close();
            } catch (Exception ignored) {
            }
    }
}
