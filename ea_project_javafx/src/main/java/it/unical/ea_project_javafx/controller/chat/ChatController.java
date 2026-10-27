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
import java.time.LocalDate;
import java.net.http.HttpResponse;
import java.io.IOException;
import javafx.geometry.Side;
import javafx.scene.layout.Priority;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import it.unical.ea_project_javafx.ui.file.NativeFiles;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import it.unical.ea_project_javafx.ui.text.EmojiText;
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
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;
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
import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;
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
    @FXML
    private HBox replyBanner, attachmentBanner;
    @FXML
    private Label replyLabel, attachmentLabel;
    @FXML
    private Button attachButton, emojiButton;
    private Long replyToId;
    private Path attachment;
    private PopupFactory.Handle mediaPopup;
    private Scene observedScene;
    private Window observedWindow;
    private final ChangeListener<Boolean> focusListener = (o, before, focused) -> {
        if (focused)
            markRead();
    };
    private final ChangeListener<Window> windowListener = (o, before, window) -> watchWindow(window);
    private final ChangeListener<Scene> sceneListener = (o, before, scene) -> watchScene(scene);
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
    private long renderEpoch;
    private final LinkedHashMap<Long, Image> photoCache = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Runnable> loadedPhotos = new LinkedHashMap<>();
    private final Map<Long, String> drafts = new HashMap<>();
    private static final String API = ApiService.BASE_URL + "/api/chat";

    @FXML
    public void initialize() {
        if (!compactMode)
            BackgroundHelper.setupBackground(bgImageView);
        if (!compactMode)
            ViewSupport.loadNavbar(navbarContainer, this::show);
        conversations.setPlaceholder(new Label("Nessuna conversazione"));
        conversations.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JsonObject item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                if (empty || item == null)
                    return;
                long id = item.get("id").getAsLong();
                Label name = new Label(JsonCodec.string(item, "name", ""));
                name.getStyleClass().add("chat-peer-name");
                name.setMaxWidth(Double.MAX_VALUE);
                name.setTextOverrun(OverrunStyle.ELLIPSIS);
                HBox.setHgrow(name, Priority.ALWAYS);
                Label time = new Label(clock(JsonCodec.string(item, "updatedAt", "")));
                time.getStyleClass().add("chat-meta");
                Circle dot = new Circle(4);
                dot.getStyleClass().add("chat-unread-dot");
                dot.setVisible(item.get("unread").getAsLong() > 0);
                HBox heading = new HBox(8, dot, name, time);
                heading.setAlignment(Pos.CENTER_LEFT);
                Label preview = new Label(
                        JsonCodec.string(item, "preview", "Nessun messaggio").replaceAll("\\s+", " "));
                preview.setTextOverrun(OverrunStyle.ELLIPSIS);
                preview.setMaxWidth(Double.MAX_VALUE);
                preview.getStyleClass().add("chat-preview");
                VBox content = new VBox(7, heading, preview);
                content.prefWidthProperty().bind(conversations.widthProperty().subtract(65));
                content.setMaxWidth(340);
                setGraphic(content);
                MenuItem read = new MenuItem("Segna come letto");
                read.setOnAction(e -> ApiService.callDetailed(API + "/conversations/" + id + "/read-all", "{}",
                        ApiService.Method.POST, r -> loadInbox(false), ChatController.this::show, null));
                MenuItem delete = new MenuItem("Elimina per me…");
                delete.setOnAction(e -> deleteConversation(id));
                setContextMenu(new ContextMenu(read, delete));
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
        pageRoot.sceneProperty().addListener(sceneListener);
        watchScene(pageRoot.getScene());
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

    private void watchScene(Scene scene) {
        if (observedScene != null)
            observedScene.windowProperty().removeListener(windowListener);
        observedScene = scene;
        if (scene != null)
            scene.windowProperty().addListener(windowListener);
        watchWindow(scene == null ? null : scene.getWindow());
    }

    private void watchWindow(Window window) {
        if (observedWindow != null)
            observedWindow.focusedProperty().removeListener(focusListener);
        observedWindow = window;
        if (window != null && !closed)
            window.focusedProperty().addListener(focusListener);
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
        pageRoot.getStyleClass().add("chat-compact");
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
        var border = (BorderPane) pageRoot.getChildren().stream().filter(n -> n instanceof BorderPane).findFirst()
                .orElseThrow();
        if (border.getCenter() instanceof BorderPane panel) {
            BorderPane.setMargin(panel, Insets.EMPTY);
            if (panel.getCenter() instanceof BorderPane thread) {
                panel.setCenter(null);
                border.setCenter(thread);
                BorderPane.setMargin(thread, Insets.EMPTY);
            }
        }
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

    public void openTraveler(Long traveler) {
        if (traveler == null)
            return;
        ApiService.callDetailed(API + "/conversations", ApiService.toJson(Map.of("travelerId", traveler)),
                ApiService.Method.POST,
                r -> {
                    if (!closed) {
                        select(JsonParser.parseString(r.body()).getAsJsonObject());
                        loadInbox(false);
                    }
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
        handleCancelReply();
        handleRemoveAttachment();
        photoCache.clear();
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
                        renderEpoch++;
                        loadedPhotos.clear();
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
        var body = EmojiText.create(JsonCodec.string(message, "body", ""));
        body.maxWidthProperty().bind(messagesBox.widthProperty().multiply(.70));
        body.prefWidthProperty().bind(messagesBox.widthProperty().multiply(.70).subtract(36));
        Label timestamp = new Label((mine ? "Tu · " : JsonCodec.string(selected, "name", "") + " · ")
                + DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())
                        .format(Instant.parse(JsonCodec.string(message, "sentAt", ""))));
        timestamp.getStyleClass().add("chat-message-time");
        VBox box = new VBox(8);
        if (message.has("replyTo") && !message.get("replyTo").isJsonNull()) {
            JsonObject reply = message.getAsJsonObject("replyTo");
            Label quote = new Label((reply.get("senderId").getAsLong() == UserSession.getInstance().getId()
                    ? "Tu"
                    : JsonCodec.string(selected, "name", "")) + "\n"
                    + JsonCodec.string(reply, "body", "").replaceAll("\\s+", " "));
            quote.setWrapText(true);
            quote.setMaxWidth(380);
            quote.getStyleClass().add("chat-quote");
            box.getChildren().add(quote);
        }
        if (message.has("hasPhoto") && message.get("hasPhoto").getAsBoolean()) {
            Button photo = new Button("📷 Apri foto");
            photo.getStyleClass().add("btn-secondary");
            long conversation = selectedId;
            photo.setOnAction(e -> {
                photo.setDisable(true);
                long revision = renderEpoch;
                ApiService.callDetailed(API + "/conversations/" + conversation + "/messages/"
                        + message.get("id").getAsLong() + "/photo", null, ApiService.Method.GET, r -> {
                            if (closed || selectedId != conversation || revision != renderEpoch)
                                return;
                            try {
                                byte[] data = Base64.getDecoder().decode(
                                        JsonParser.parseString(r.body()).getAsJsonObject().get("data").getAsString());
                                Image image = new Image(new ByteArrayInputStream(data), 1000, 800, true, true);
                                photoCache.put(message.get("id").getAsLong(), image);
                                installPhoto(box, photo, image, message.get("id").getAsLong());
                                if (photoCache.size() > 12) {
                                    Long evicted = photoCache.firstEntry().getKey();
                                    photoCache.remove(evicted);
                                    Runnable reset = loadedPhotos.remove(evicted);
                                    if (reset != null)
                                        reset.run();
                                }
                            } catch (RuntimeException ex) {
                                photo.setDisable(false);
                                show("Foto non disponibile.");
                            }
                        }, error -> {
                            photo.setDisable(false);
                            show(error);
                        }, null);
            });
            box.getChildren().add(photo);
            Image cached = photoCache.get(message.get("id").getAsLong());
            if (cached != null)
                installPhoto(box, photo, cached, message.get("id").getAsLong());
        }
        box.getChildren().addAll(body, timestamp);
        box.getStyleClass().add(mine ? "chat-message-own" : "chat-message-other");
        box.setMaxWidth(520);
        MenuItem reply = new MenuItem("Rispondi a questo messaggio");
        reply.setOnAction(e -> {
            if (sending)
                return;
            pendingBody = null;
            replyToId = message.get("id").getAsLong();
            replyLabel.setText((mine ? "Tu: " : JsonCodec.string(selected, "name", "") + ": ")
                    + JsonCodec.string(message, "body", "Foto").replaceAll("\\s+", " "));
            replyBanner.setManaged(true);
            replyBanner.setVisible(true);
            composer.requestFocus();
        });
        Tooltip.install(box, new Tooltip("Tasto destro per rispondere a questo messaggio"));
        ContextMenu menu = new ContextMenu(reply);
        box.setOnContextMenuRequested(e -> menu.show(box, e.getScreenX(), e.getScreenY()));
        HBox row = new HBox(box);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private void installPhoto(VBox box, Button photo, Image image, long id) {
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.fitWidthProperty()
                .bind(messagesBox.widthProperty().multiply(.65).map(w -> Math.min(360, w.doubleValue())));
        box.getChildren().set(box.getChildren().indexOf(photo), view);
        view.getStyleClass().add("media-interactive");
        view.setOnMouseClicked(click -> {
            ImageView full = new ImageView(image);
            full.setPreserveRatio(true);
            full.setFitWidth(Math.min(1000, pageRoot.getScene().getWidth() - 100));
            full.setFitHeight(pageRoot.getScene().getHeight() - 150);
            mediaPopup = PopupFactory.show(pageRoot, new StackPane(full), PopupFactory.Size.LARGE);
        });
        loadedPhotos.put(id, () -> {
            int position = box.getChildren().indexOf(view);
            if (position >= 0)
                box.getChildren().set(position, photo);
            photo.setDisable(false);
        });
    }

    @FXML
    private void handleSend() {
        String body = composer.getText().strip();
        if (closed || sending || selectedId == 0 || (body.isBlank() && attachment == null))
            return;
        String fingerprint = body + "|" + replyToId + "|" + attachment;
        if (!fingerprint.equals(pendingBody)) {
            pendingBody = fingerprint;
            pendingId = UUID.randomUUID().toString();
        }
        sending = true;
        conversations.setDisable(true);
        composer.setDisable(true);
        updateSend();
        show("Invio in corso…");
        long conversation = selectedId;
        Consumer<HttpResponse<String>> success = r -> {
            if (closed)
                return;
            sending = false;
            conversations.setDisable(false);
            composer.setDisable(false);
            composer.clear();
            handleCancelReply();
            handleRemoveAttachment();
            drafts.remove(conversation);
            pendingBody = null;
            pendingId = null;
            show("");
            updateSend();
            loadMessages(null);
            loadInbox(false);
        };
        Consumer<String> failure = message -> {
            if (closed)
                return;
            sending = false;
            conversations.setDisable(false);
            composer.setDisable(false);
            updateSend();
            show(message + " Il messaggio è conservato: puoi ritentare l'invio.");
        };
        Map<String, String> fields = new HashMap<>();
        fields.put("clientId", pendingId);
        fields.put("body", body);
        if (replyToId != null)
            fields.put("replyToId", replyToId.toString());
        if (attachment != null)
            ApiService.uploadPhoto(API + "/conversations/" + conversation + "/photos", attachment, fields, success,
                    failure);
        else
            ApiService.callDetailed(API + "/conversations/" + conversation + "/messages", ApiService.toJson(fields),
                    ApiService.Method.POST, success, failure, null);
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
        composer.setDisable(sending || selectedId == 0);
        sendButton.setDisable(sending || selectedId == 0 || (composer.getText().isBlank() && attachment == null));
        attachButton.setDisable(sending || selectedId == 0);
        emojiButton.setDisable(sending || selectedId == 0);
    }

    private String clock(String instant) {
        try {
            var date = Instant.parse(instant).atZone(ZoneId.systemDefault());
            return date.format(
                    DateTimeFormatter.ofPattern(date.toLocalDate().equals(LocalDate.now()) ? "HH:mm" : "dd/MM"));
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void deleteConversation(long id) {
        if (sending)
            return;
        PopupFactory.showConfirmation(pageRoot, "Eliminare la conversazione?",
                "La cronologia sarà nascosta solo per te. L'altro partecipante conserverà i suoi messaggi.",
                PopupFactory.Size.COMPACT, () -> ApiService.callDetailed(API + "/conversations/" + id, null,
                        ApiService.Method.DELETE, r -> {
                            if (closed)
                                return;
                            drafts.remove(id);
                            if (selectedId == id) {
                                selectedId = 0;
                                selected = null;
                                requestGeneration++;
                                messagesBusy = false;
                                renderEpoch++;
                                loadedPhotos.clear();
                                messagesBox.getChildren().clear();
                                composer.clear();
                                drafts.remove(id);
                                conversationTitle.setText("Le tue conversazioni");
                                conversationSubtitle.setText("Scegli una conversazione");
                                handleCancelReply();
                                handleRemoveAttachment();
                                updateSend();
                            }
                            loadInbox(false);
                        }, this::show, null));
    }

    @FXML
    private void handleCancelReply() {
        if (sending)
            return;
        pendingBody = null;
        replyToId = null;
        replyBanner.setVisible(false);
        replyBanner.setManaged(false);
    }

    @FXML
    private void handleRemoveAttachment() {
        if (sending)
            return;
        pendingBody = null;
        attachment = null;
        attachmentBanner.setVisible(false);
        attachmentBanner.setManaged(false);
        updateSend();
    }

    @FXML
    private void handleAttach() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Allega foto o screenshot");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Immagini (massimo 8 MB)", "*.png", "*.jpg",
                "*.jpeg", "*.gif", "*.bmp"));
        var file = NativeFiles.open(chooser, pageRoot.getScene().getWindow());
        if (file == null)
            return;
        try {
            if (Files.size(file.toPath()) > 8 * 1024 * 1024) {
                show("La foto deve essere inferiore a 8 MB.");
                return;
            }
            pendingBody = null;
            attachment = file.toPath();
            attachmentLabel.setText("📷 " + file.getName());
            attachmentBanner.setVisible(true);
            attachmentBanner.setManaged(true);
            updateSend();
        } catch (IOException ex) {
            show("Impossibile leggere il file selezionato.");
        }
    }

    @FXML
    private void handleEmoji() {
        ContextMenu picker = new ContextMenu();
        for (String emoji : List.of("😀", "😊", "😂", "❤️", "👍", "🙏", "🎉", "✈️", "📍", "👋")) {
            MenuItem item = new MenuItem(emoji);
            item.setGraphic(EmojiText.create(emoji));
            item.setText(null);
            item.setOnAction(e -> composer.insertText(composer.getCaretPosition(), emoji));
            picker.getItems().add(item);
        }
        picker.show(emojiButton, Side.TOP, 0, 0);
    }

    private void show(String message) {
        if (!closed)
            statusLabel.setText(message == null ? "" : message);
    }

    @Override
    public void close() {
        closed = true;
        if (mediaPopup != null)
            mediaPopup.close();
        pageRoot.sceneProperty().removeListener(sceneListener);
        watchScene(null);
        renderEpoch++;
        loadedPhotos.clear();
        photoCache.clear();
        drafts.clear();
        settleScroll.stop();
        if (stream != null)
            try {
                stream.close();
            } catch (Exception ignored) {
            }
    }
}
