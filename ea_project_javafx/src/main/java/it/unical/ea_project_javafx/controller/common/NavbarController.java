package it.unical.ea_project_javafx.controller.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.controller.chat.ChatController;
import it.unical.ea_project_javafx.navigation.SceneNavigator;
import it.unical.ea_project_javafx.security.TokenStorage;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.service.notification.NotificationPollingService;
import it.unical.ea_project_javafx.ui.animation.NavIndicator;
import it.unical.ea_project_javafx.ui.animation.UiEffects;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import it.unical.ea_project_javafx.util.JsonCodec;
import java.util.Arrays;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Transform;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public class NavbarController {

    private static final String MAIN_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/home/mainview.fxml";
    private static final String LOGIN_FXML = "/it/unical/ea_project_javafx/fxml/login/login.fxml";
    private static final String DASHBOARD_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/profile/dashboard-view.fxml";
    private static final String SETTINGS_FXML = "/it/unical/ea_project_javafx/fxml/profile/settings.fxml";
    private static final String NOTIFICATIONS_URL = ApiService.BASE_URL + "/api/notifications";

    @FXML
    private Label loginLabel;
    @FXML
    private MenuButton userMenuButton;
    @FXML
    private ImageView userAvatarImageView;
    @FXML
    private Label userAvatarInitialsLabel;
    @FXML
    private Label userMenuLabel;

    @FXML
    private StackPane notificationBellContainer;
    @FXML
    private Label notificationBadge;

    @FXML
    private Label dashboardNavItem;
    @FXML
    private Label homeNavItem;
    @FXML
    private MenuItem profileMenuItem, settingsMenuItem, supportMenuItem;
    @FXML
    private Label travelerHistoryNavItem;
    @FXML
    private Label travelerFavoritesNavItem;
    @FXML
    private MenuItem chatMenuItem, recommendationsMenuItem, itineraryMenuItem;
    @FXML
    private Label organizerEventsNavItem;
    @FXML
    private Label organizerAttendeesNavItem;
    @FXML
    private MenuItem paymentsMenuItem;

    private NotificationPollingService.Subscription notificationSubscription;
    private Timeline toastTimeout;
    private Popup notificationPopup;
    private Popup toastPopup;
    private long avatarLoadRequestId = 0;
    private boolean notificationTrayLoading;
    private long lastTrayAutoHide;
    private long trayRequest;
    private Scene watchedScene;
    private Window watchedWindow;
    private final ChangeListener<Window> notificationWindowListener = (o, a, b) -> watchWindow(b);
    private final ChangeListener<Boolean> notificationShowingListener = (o, a, b) -> {
        if (b)
            startNotificationPolling();
        else
            stopNotificationPolling();
    };
    @FXML
    private Label travelerItineraryNavItem;

    @FXML
    public void initialize() {
        bindVisibilityToManaged(loginLabel);
        bindVisibilityToManaged(userMenuButton);
        bindVisibilityToManaged(notificationBellContainer);
        bindVisibilityToManaged(dashboardNavItem);
        bindVisibilityToManaged(travelerHistoryNavItem);
        bindVisibilityToManaged(travelerFavoritesNavItem);
        bindVisibilityToManaged(organizerEventsNavItem);
        bindVisibilityToManaged(organizerAttendeesNavItem);
        configureUserAvatar();
        updateNavbarState();
        for (Label item : Arrays.asList(homeNavItem, dashboardNavItem, travelerHistoryNavItem, travelerFavoritesNavItem,
                travelerItineraryNavItem, organizerEventsNavItem, organizerAttendeesNavItem)) {
            if (item != null)
                item.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    if (e.isPrimaryButtonDown())
                        NavIndicator.preview(item);
                });
        }
        if (notificationBellContainer != null) {
            notificationBellContainer.sceneProperty().addListener((o, a, b) -> watchScene(b));
            watchScene(notificationBellContainer.getScene());
        }
    }

    private void watchScene(Scene scene) {
        updateActiveSection(scene);
        if (watchedScene != null)
            watchedScene.windowProperty().removeListener(notificationWindowListener);
        watchedScene = scene;
        if (scene != null)
            scene.windowProperty().addListener(notificationWindowListener);
        watchWindow(scene == null ? null : scene.getWindow());
    }

    private void updateActiveSection(Scene scene) {
        if (scene == null)
            return;
        Platform.runLater(() -> {
            if (scene != watchedScene)
                return;
            String path = String.valueOf(scene.getUserData());
            var active = PseudoClass.getPseudoClass("active");
            markActive(homeNavItem, path.endsWith("home/mainview.fxml"), active);
            markActive(dashboardNavItem, path.endsWith("profile/dashboard-view.fxml"), active);
            markActive(travelerHistoryNavItem,
                    path.endsWith("booking-history.fxml") || path.endsWith("traveler/bookings.fxml"), active);
            markActive(travelerFavoritesNavItem, path.endsWith("favorites.fxml"), active);
            markActive(travelerItineraryNavItem, path.endsWith("collaborative-itinerary.fxml"), active);
            markActive(organizerEventsNavItem, path.endsWith("events.fxml") || path.endsWith("create-offer.fxml"),
                    active);
            markActive(organizerAttendeesNavItem, path.endsWith("attendees.fxml"), active);
            markMenuActive(chatMenuItem, path.endsWith("chat/chat.fxml"));
            markMenuActive(profileMenuItem, path.endsWith("profile/profile.fxml"));
            markMenuActive(settingsMenuItem, path.endsWith("settings.fxml"));
            markMenuActive(paymentsMenuItem, path.endsWith("payment-methods.fxml"));
            markMenuActive(supportMenuItem, path.endsWith("support.fxml"));
            NavIndicator.select(scene);
        });
    }

    private void markActive(Label label, boolean selected, PseudoClass active) {
        if (label == null)
            return;
        label.pseudoClassStateChanged(active, selected);
        label.setAccessibleText(label.getText() + (selected ? ", sezione attuale" : ""));
    }

    private void markMenuActive(MenuItem item, boolean selected) {
        if (item == null)
            return;
        item.getStyleClass().remove("nav-menu-active");
        if (selected)
            item.getStyleClass().add("nav-menu-active");
    }

    private void watchWindow(Window window) {
        if (watchedWindow != null)
            watchedWindow.showingProperty().removeListener(notificationShowingListener);
        watchedWindow = window;
        stopNotificationPolling();
        if (window != null) {
            Platform.runLater(() -> {
                Node node = notificationBellContainer;
                while (node != null && !node.getStyleClass().contains("navbar"))
                    node = node.getParent();
                if (node != null && node.getParent() != null)
                    node.getParent().setViewOrder(-1);
            });
            window.showingProperty().addListener(notificationShowingListener);
            if (window.isShowing())
                startNotificationPolling();
        }
    }

    private void bindVisibilityToManaged(Node node) {
        if (node != null)
            node.managedProperty().bind(node.visibleProperty());
    }

    public void updateNavbarState() {
        boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
        boolean isOrganizer = isLoggedIn && "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        boolean isTraveler = isLoggedIn && !isOrganizer;
        if (travelerItineraryNavItem != null) {
            travelerItineraryNavItem.setVisible(isTraveler);
            travelerItineraryNavItem.setManaged(isTraveler);
        }

        if (chatMenuItem != null)
            chatMenuItem.setVisible(isLoggedIn);
        if (recommendationsMenuItem != null)
            recommendationsMenuItem.setVisible(isTraveler);
        if (itineraryMenuItem != null)
            itineraryMenuItem.setVisible(isTraveler);
        if (loginLabel != null)
            loginLabel.setVisible(!isLoggedIn);
        if (userMenuButton != null) {
            userMenuButton.setVisible(isLoggedIn);
            if (isLoggedIn) {
                String username = UserSession.getInstance().getUsername();
                if (userMenuLabel != null)
                    userMenuLabel.setText(username == null || username.isBlank() ? "Utente" : username);
                userMenuButton.setAccessibleText("Menu utente " + (username == null ? "" : username));
                updateUserAvatar();
            } else {
                if (userMenuLabel != null)
                    userMenuLabel.setText("Utente");
                if (userAvatarImageView != null) {
                    userAvatarImageView.setImage(null);
                    userAvatarImageView.setVisible(false);
                    userAvatarImageView.setManaged(false);
                }
                if (userAvatarInitialsLabel != null) {
                    userAvatarInitialsLabel.setText("U");
                    userAvatarInitialsLabel.setVisible(true);
                    userAvatarInitialsLabel.setManaged(true);
                }
            }
        }
        if (notificationBellContainer != null)
            notificationBellContainer.setVisible(isLoggedIn);
        if (dashboardNavItem != null)
            dashboardNavItem.setVisible(isLoggedIn);
        if (travelerHistoryNavItem != null)
            travelerHistoryNavItem.setVisible(isTraveler);
        if (travelerFavoritesNavItem != null)
            travelerFavoritesNavItem.setVisible(isTraveler);
        if (organizerEventsNavItem != null)
            organizerEventsNavItem.setVisible(isOrganizer);
        if (organizerAttendeesNavItem != null)
            organizerAttendeesNavItem.setVisible(isOrganizer);
        if (paymentsMenuItem != null)
            paymentsMenuItem.setText(isOrganizer ? "Incassi & Payout" : "Metodi di Pagamento");

        if (isLoggedIn)
            startNotificationPolling();
        else
            stopNotificationPolling();
    }

    private void configureUserAvatar() {
        if (userAvatarImageView != null) {
            userAvatarImageView.setFitWidth(28);
            userAvatarImageView.setFitHeight(28);
            userAvatarImageView.setPreserveRatio(false);
            userAvatarImageView.setSmooth(true);
            userAvatarImageView.setClip(new Circle(14, 14, 14));
            userAvatarImageView.setVisible(false);
            userAvatarImageView.setManaged(false);
        }
    }

    /**
     * Keeps the navbar avatar in sync with UserSession after an upload, replacement
     * or removal.
     */
    private void updateUserAvatar() {
        long requestId = ++avatarLoadRequestId;
        UserSession session = UserSession.getInstance();
        String displayName = session.getFullName();
        if (displayName == null || displayName.isBlank())
            displayName = session.getUsername();
        if (userAvatarInitialsLabel != null) {
            userAvatarInitialsLabel.setText(makeInitials(displayName));
            userAvatarInitialsLabel.setVisible(true);
            userAvatarInitialsLabel.setManaged(true);
        }
        if (userAvatarImageView != null) {
            userAvatarImageView.setImage(null);
            userAvatarImageView.setVisible(false);
            userAvatarImageView.setManaged(false);
        }

        String photoUrl = session.getProfilePhotoUrl();
        if (photoUrl == null || photoUrl.isBlank() || userAvatarImageView == null)
            return;

        String resolvedUrl = ApiService.resolveUrl(photoUrl) + "?v=" + System.currentTimeMillis();
        Image image = new Image(resolvedUrl, 28, 28, true, true, true);
        image.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (hasError)
                Platform.runLater(() -> {
                    if (requestId == avatarLoadRequestId)
                        showAvatarImage(null);
                });
        });
        image.progressProperty().addListener((obs, oldValue, progress) -> {
            if (progress.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    if (requestId == avatarLoadRequestId)
                        showAvatarImage(image);
                });
            }
        });
        if (image.getProgress() >= 1.0 && !image.isError() && requestId == avatarLoadRequestId) {
            showAvatarImage(image);
        }
    }

    private void showAvatarImage(Image image) {
        boolean loaded = image != null && !image.isError();
        if (userAvatarImageView != null) {
            if (loaded) {
                double width = image.getWidth();
                double height = image.getHeight();
                if (width > 0 && height > 0) {
                    double side = Math.min(width, height);
                    userAvatarImageView.setViewport(new Rectangle2D(
                            (width - side) / 2.0, (height - side) / 2.0, side, side));
                } else {
                    userAvatarImageView.setViewport(null);
                }
                userAvatarImageView.setFitWidth(28);
                userAvatarImageView.setFitHeight(28);
                userAvatarImageView.setPreserveRatio(false);
            } else {
                userAvatarImageView.setViewport(null);
            }
            userAvatarImageView.setImage(loaded ? image : null);
            userAvatarImageView.setVisible(loaded);
            userAvatarImageView.setManaged(loaded);
        }
        if (userAvatarInitialsLabel != null) {
            userAvatarInitialsLabel.setVisible(!loaded);
            userAvatarInitialsLabel.setManaged(!loaded);
        }
    }

    private String makeInitials(String name) {
        if (name == null || name.isBlank())
            return "U";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        if (parts.length == 1)
            return first;
        return first + parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void startNotificationPolling() {
        if (notificationSubscription != null || !UserSession.getInstance().isLoggedIn()
                || notificationBellContainer == null || notificationBellContainer.getScene() == null)
            return;
        notificationSubscription = NotificationPollingService.getInstance().subscribe(snapshot -> {
            updateNotificationBadge(snapshot.unreadCount());
            if (snapshot.newNotification() != null) {
                if (notificationPopup != null && notificationPopup.isShowing())
                    NotificationPollingService.getInstance().fetchUnreadForTray(this::showNotificationTray, null);
                else
                    showToast(snapshot.newNotification());
            }
        });
    }

    private void stopNotificationPolling() {
        if (notificationSubscription != null) {
            notificationSubscription.close();
            notificationSubscription = null;
        }
        if (notificationPopup != null) {
            notificationPopup.hide();
            notificationPopup = null;
        }
        if (toastPopup != null) {
            toastPopup.hide();
            toastPopup = null;
        }
        if (toastTimeout != null) {
            toastTimeout.stop();
            toastTimeout = null;
        }
        notificationTrayLoading = false;
        updateNotificationBadge(0);
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadge == null)
            return;
        notificationBadge.setText(count > 99 ? "99+" : Integer.toString(count));
        notificationBadge.setVisible(count > 0);
        notificationBadge.setManaged(count > 0);
    }

    @FXML
    private void handleNotifications(MouseEvent event) {
        event.consume();
        if (System.nanoTime() - lastTrayAutoHide < 250_000_000L)
            return;
        if (notificationTrayLoading) {
            notificationTrayLoading = false;
            trayRequest++;
            return;
        }
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
            notificationPopup = null;
            return;
        }
        openNotificationTray();
    }

    private void openNotificationTray() {
        if (notificationBellContainer == null || notificationBellContainer.getScene() == null
                || notificationTrayLoading)
            return;
        notificationTrayLoading = true;
        long request = ++trayRequest;
        NotificationPollingService.getInstance().fetchUnreadForTray(
                notifications -> {
                    if (request != trayRequest)
                        return;
                    notificationTrayLoading = false;
                    if (notificationBellContainer != null && notificationBellContainer.getScene() != null) {
                        showNotificationTray(notifications);
                    }
                },
                message -> {
                    if (request != trayRequest)
                        return;
                    notificationTrayLoading = false;
                    if (notificationBellContainer != null && notificationBellContainer.getScene() != null) {
                        showToast("Notifiche", message);
                    }
                });
    }

    private void showNotificationTray(JsonArray notifications) {
        notificationTrayLoading = false;
        if (notificationPopup != null)
            notificationPopup.hide();
        VBox root = new VBox(10);
        root.getStyleClass().add("notification-tray");
        root.setPadding(new Insets(16));
        root.setPrefWidth(360);
        root.setMaxHeight(430);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Notifiche");
        title.getStyleClass().add("notification-tray-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAll = new Button("Segna tutte come lette");
        markAll.getStyleClass().add("notification-mark-all");
        markAll.setOnAction(e -> ApiService.call(NOTIFICATIONS_URL + "/read-all", "{}", ApiService.Method.POST,
                response -> {
                    if (notificationPopup != null)
                        notificationPopup.hide();
                    notificationPopup = null;
                    openNotificationTray();
                },
                () -> showToast("Notifiche", "Non è stato possibile aggiornare le notifiche."), null));
        header.getChildren().addAll(title, spacer, markAll);
        root.getChildren().addAll(header, new Separator());

        VBox cards = new VBox(9);
        int shown = Math.min(notifications.size(), 15);
        for (int i = 0; i < shown; i++) {
            JsonElement element = notifications.get(i);
            if (element.isJsonObject())
                cards.getChildren().add(createNotificationCard(element.getAsJsonObject()));
        }
        if (cards.getChildren().isEmpty()) {
            Label empty = new Label("Nessuna notifica");
            empty.getStyleClass().add("notification-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(24, 8, 24, 8));
            cards.setAlignment(Pos.CENTER);
            cards.setPrefHeight(300);
            cards.getChildren().add(empty);
        }
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(330);
        scroll.setMaxHeight(350);
        scroll.getStyleClass().add("notification-scroll");
        root.getChildren().add(scroll);

        notificationPopup = new Popup();
        notificationPopup.setOnShown(
                e -> UiEffects.useWallpaper(notificationPopup.getScene(), null));
        notificationPopup.setAutoHide(true);
        notificationPopup.setOnAutoHide(e -> lastTrayAutoHide = System.nanoTime());
        notificationPopup.setHideOnEscape(true);
        notificationPopup.getContent().add(root);
        positionPopup(notificationPopup, popupAnchor(), 360);
    }

    private VBox createNotificationCard(JsonObject notification) {
        VBox card = new VBox(6);
        card.getStyleClass().add("notification-card");
        if (notification.has("read") && !notification.get("read").isJsonNull()
                && notification.get("read").getAsBoolean())
            card.getStyleClass().add("notification-card-read");
        String titleText = JsonCodec.string(notification, "title", "Notifica");
        String messageText = JsonCodec.string(notification, "message", "");
        String type = JsonCodec.string(notification, "type", "NOTICE");
        long id = notification.has("notificationId") ? notification.get("notificationId").getAsLong() : -1;
        Label title = new Label(titleText);
        title.getStyleClass().add("notification-card-title");
        title.setWrapText(true);
        Label message = new Label(messageText);
        message.getStyleClass().add("notification-card-message");
        message.setWrapText(true);
        card.getChildren().addAll(title, message);
        if ("CHAT".equals(JsonCodec.string(notification, "relatedEntityType", ""))) {
            Button open = new Button("Apri conversazione →");
            open.getStyleClass().add("notification-mark-all");
            open.setOnAction(e -> {
                if (notificationPopup != null)
                    notificationPopup.hide();
                long conversation = notification.get("relatedEntityId").getAsLong();
                SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/chat/chat.fxml", c -> {
                    if (c instanceof ChatController chat)
                        chat.openConversation(conversation);
                });
            });
            card.getChildren().add(open);
        }

        if ("LIST_INVITATION".equals(type) && id > 0) {
            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            Button preview = new Button("Visualizza dettagli");
            preview.getStyleClass().add("notification-mark-all");
            Button accept = new Button("Accetta");
            accept.getStyleClass().add("notification-accept");
            Button reject = new Button("Rifiuta");
            reject.getStyleClass().add("notification-decline");
            preview.setOnAction(e -> previewInvitation(id));
            accept.setOnAction(e -> respondToInvitation(id, true));
            reject.setOnAction(e -> respondToInvitation(id, false));
            HBox previewRow = new HBox(preview);
            previewRow.setAlignment(Pos.CENTER_LEFT);
            actions.getChildren().addAll(accept, reject);
            card.getChildren().addAll(previewRow, actions);
        } else if (id > 0 && !(notification.has("read") && notification.get("read").getAsBoolean())) {
            Button markRead = new Button("Segna come letta");
            markRead.getStyleClass().add("notification-mark-all");
            markRead.setOnAction(
                    e -> ApiService.call(NOTIFICATIONS_URL + "/" + id + "/read", "{}", ApiService.Method.POST,
                            response -> {
                                if (notificationPopup != null) {
                                    notificationPopup.hide();
                                    notificationPopup = null;
                                }
                                openNotificationTray();
                            },
                            () -> showToast("Notifiche", "Non è stato possibile segnare la notifica come letta."),
                            null));
            HBox row = new HBox(markRead);
            row.setAlignment(Pos.CENTER_RIGHT);
            card.getChildren().add(row);
        }
        return card;
    }

    private void previewInvitation(long notificationId) {
        ApiService.call(NOTIFICATIONS_URL + "/" + notificationId + "/invitation-preview", null, ApiService.Method.GET,
                response -> {
                    try {
                        JsonObject list = JsonParser.parseString(response.body()).getAsJsonObject();
                        StringBuilder details = new StringBuilder();
                        details.append("Lista: ").append(JsonCodec.string(list, "listName", "Lista condivisa"))
                                .append('\n');
                        details.append("Organizzatore della lista: @")
                                .append(JsonCodec.string(list, "organizerUsername", "non disponibile")).append('\n');
                        details.append("Permesso previsto: ").append(JsonCodec.string(list, "permission", "VIEW"))
                                .append("\n\n");
                        JsonArray trips = list.has("trips") && list.get("trips").isJsonArray()
                                ? list.getAsJsonArray("trips")
                                : new JsonArray();
                        if (trips.size() == 0)
                            details.append("La lista non contiene ancora viaggi.\n");
                        for (JsonElement tripElement : trips) {
                            if (!tripElement.isJsonObject())
                                continue;
                            JsonObject trip = tripElement.getAsJsonObject();
                            details.append("✈ ").append(JsonCodec.string(trip, "title", "Viaggio senza titolo"));
                            String city = JsonCodec.string(trip, "destinationCity", "");
                            String country = JsonCodec.string(trip, "destinationCountry", "");
                            String destination = city + (city.isBlank() || country.isBlank() ? "" : ", ") + country;
                            if (!destination.isBlank())
                                details.append(" — ").append(destination);
                            details.append('\n').append("  Organizzatore del viaggio: @")
                                    .append(JsonCodec.string(trip, "organizerUsername", "non disponibile"))
                                    .append('\n');
                            String start = JsonCodec.string(trip, "startDate", "");
                            String end = JsonCodec.string(trip, "endDate", "");
                            if (!start.isBlank() || !end.isBlank())
                                details.append("  Date: ").append(start.isBlank() ? "?" : start)
                                        .append(" → ").append(end.isBlank() ? "?" : end).append('\n');
                            JsonArray stages = trip.has("stages") && trip.get("stages").isJsonArray()
                                    ? trip.getAsJsonArray("stages")
                                    : new JsonArray();
                            if (stages.size() == 0)
                                details.append("  Nessuna tappa inserita.\n");
                            for (JsonElement stageElement : stages) {
                                if (!stageElement.isJsonObject())
                                    continue;
                                JsonObject stage = stageElement.getAsJsonObject();
                                details.append("    Giorno ").append(JsonCodec.string(stage, "day", "?"))
                                        .append(" · tappa ").append(JsonCodec.string(stage, "orderInDay", "?"))
                                        .append(": ").append(JsonCodec.string(stage, "title", "Tappa"));
                                String location = JsonCodec.string(stage, "locationName", "");
                                if (location.isBlank())
                                    location = JsonCodec.string(stage, "city", "");
                                String stageCountry = JsonCodec.string(stage, "country", "");
                                if (!location.isBlank())
                                    details.append(" — ").append(location);
                                if (!stageCountry.isBlank() && !stageCountry.equals(location))
                                    details.append(", ").append(stageCountry);
                                String timeStart = JsonCodec.string(stage, "startTime", "");
                                String timeEnd = JsonCodec.string(stage, "endTime", "");
                                if (!timeStart.isBlank() || !timeEnd.isBlank())
                                    details.append(" ( ")
                                            .append(timeStart.isBlank() ? "--:--" : timeStart).append("–")
                                            .append(timeEnd.isBlank() ? "--:--" : timeEnd).append(" )");
                                details.append('\n');
                                String description = JsonCodec.string(stage, "description", "");
                                if (!description.isBlank())
                                    details.append("      ").append(description).append('\n');
                            }
                            details.append('\n');
                        }
                        VBox panel = new VBox(14);
                        panel.setMaxWidth(760);
                        panel.setMaxHeight(700);
                        Label heading = new Label("Anteprima della lista condivisa");
                        heading.getStyleClass().add("app-modal-title");
                        Label subtitle = new Label(JsonCodec.string(list, "listName", "Lista condivisa"));
                        subtitle.getStyleClass().add("app-modal-subtitle");
                        TextArea previewText = new TextArea(details.toString());
                        previewText.setEditable(false);
                        previewText.setWrapText(true);
                        previewText.setFocusTraversable(false);
                        previewText.setPrefRowCount(18);
                        previewText.setMaxHeight(Double.MAX_VALUE);
                        VBox.setVgrow(previewText, Priority.ALWAYS);
                        Button close = new Button("Chiudi");
                        close.getStyleClass().add("btn-secondary");
                        HBox actions = new HBox(close);
                        actions.setAlignment(Pos.CENTER_RIGHT);
                        panel.getChildren().addAll(heading, subtitle, previewText, actions);
                        StackPane popupRoot = PopupFactory.resolveRoot(notificationBellContainer);
                        if (popupRoot != null) {
                            PopupFactory.Handle handle = PopupFactory.show(popupRoot, panel, PopupFactory.Size.LARGE);
                            close.setOnAction(event -> handle.close());
                        }
                    } catch (Exception ex) {
                        showToast("Invito alla lista", "Non è stato possibile leggere i dettagli della lista.");
                    }
                },
                () -> showToast("Invito alla lista", "Non è stato possibile caricare l’anteprima della lista."), null);
    }

    private void respondToInvitation(long id, boolean accept) {
        String action = accept ? "accept" : "reject";
        ApiService.call(NOTIFICATIONS_URL + "/" + id + "/" + action, "{}", ApiService.Method.POST,
                response -> {
                    if (notificationPopup != null) {
                        notificationPopup.hide();
                        notificationPopup = null;
                    }
                    showToast("Invito alla lista", accept ? "Invito accettato." : "Invito rifiutato.");
                    openNotificationTray();
                },
                () -> showToast("Invito alla lista", accept ? "Non è stato possibile accettare l’invito."
                        : "Non è stato possibile rifiutare l’invito."),
                null);
    }

    private void showToast(JsonObject notification) {
        showToast(JsonCodec.string(notification, "title", "Nuova notifica"),
                JsonCodec.string(notification, "message", "Hai ricevuto una nuova notifica."));
    }

    private void showToast(String titleText, String messageText) {
        if (notificationBellContainer == null || notificationBellContainer.getScene() == null)
            return;
        if (toastPopup != null)
            toastPopup.hide();
        if (toastTimeout != null)
            toastTimeout.stop();
        VBox toast = new VBox(5);
        toast.getStyleClass().add("notification-toast");
        toast.setPadding(new Insets(14));
        toast.setPrefWidth(330);
        Label title = new Label(titleText);
        title.getStyleClass().add("notification-toast-title");
        title.setWrapText(true);
        Label message = new Label(messageText);
        message.getStyleClass().add("notification-toast-message");
        message.setWrapText(true);
        toast.getChildren().addAll(title, message);
        toastPopup = new Popup();
        toastPopup
                .setOnShown(e -> UiEffects.useWallpaper(toastPopup.getScene(), null));
        toastPopup.setAutoHide(false);
        toastPopup.getContent().add(toast);
        positionPopup(toastPopup, popupAnchor(), 330);
        toastTimeout = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (toastPopup != null) {
                toastPopup.hide();
                toastPopup = null;
            }
        }));
        toastTimeout.setCycleCount(1);
        toastTimeout.play();
    }

    private Node popupAnchor() {
        return notificationBellContainer != null ? notificationBellContainer : userMenuButton;
    }

    private void positionPopup(Popup popup, Node anchor, double width) {
        if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null)
            return;
        Window owner = anchor.getScene().getWindow();
        Runnable reposition = () -> {
            if (!popup.isShowing() || anchor.getScene() == null)
                return;
            Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
            if (bounds == null)
                return;
            Bounds viewport = anchor.getScene().getRoot().localToScreen(anchor.getScene().getRoot().getBoundsInLocal());
            if (viewport == null)
                return;
            double w = Math.min(width, Math.max(180, viewport.getWidth() - 80));
            double h = popup.getHeight();
            if (!popup.getContent().isEmpty()
                    && popup.getContent().getFirst() instanceof Region panel) {
                panel.setPrefWidth(w);
                panel.setMaxHeight(Math.min(430, Math.max(160, viewport.getHeight() - 80)));
            }
            double actualWidth = Math.max(w, popup.getWidth());
            popup.setX(Math.max(viewport.getMinX() + 16,
                    Math.min(bounds.getCenterX() - actualWidth / 2, viewport.getMaxX() - actualWidth - 16)));
            popup.setY(Math.max(viewport.getMinY() + 12, Math.min(bounds.getMaxY() + 12, viewport.getMaxY() - h - 16)));
        };
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null)
            return;
        popup.setAutoFix(false);
        popup.show(owner, bounds.getMinX(), bounds.getMaxY() + 12);
        Platform.runLater(reposition);

        // A Popup JavaFX is una finestra separata: seguiamo spostamento e
        // ridimensionamento
        // della finestra principale, così la tendina resta ancorata alla navbar.
        ChangeListener<Number> windowListener = (obs, oldValue, newValue) -> reposition.run();
        owner.xProperty().addListener(windowListener);
        owner.yProperty().addListener(windowListener);
        owner.widthProperty().addListener(windowListener);
        owner.heightProperty().addListener(windowListener);
        popup.widthProperty().addListener(windowListener);
        popup.heightProperty().addListener(windowListener);
        ChangeListener<Bounds> boundsListener = (obs, oldValue, newValue) -> reposition.run();
        ChangeListener<Transform> transformListener = (obs, oldValue,
                newValue) -> reposition.run();
        anchor.boundsInLocalProperty().addListener(boundsListener);
        anchor.localToSceneTransformProperty().addListener(transformListener);
        popup.setOnHidden(event -> {
            owner.xProperty().removeListener(windowListener);
            owner.yProperty().removeListener(windowListener);
            owner.widthProperty().removeListener(windowListener);
            owner.heightProperty().removeListener(windowListener);
            popup.widthProperty().removeListener(windowListener);
            popup.heightProperty().removeListener(windowListener);
            anchor.boundsInLocalProperty().removeListener(boundsListener);
            anchor.localToSceneTransformProperty().removeListener(transformListener);
        });
        if (popup.getScene() != null) {
            String css = getClass().getResource("/it/unical/ea_project_javafx/css/common/notifications.css")
                    .toExternalForm();
            if (!popup.getScene().getStylesheets().contains(css))
                popup.getScene().getStylesheets().add(css);
        }
    }

    @FXML
    void handleHome(MouseEvent event) {
        SceneNavigator.getInstance().loadScene(MAIN_VIEW_FXML, null);
    }

    @FXML
    void handleLogin(MouseEvent event) {
        SceneNavigator.getInstance().loadScene(LOGIN_FXML, null);
    }

    @FXML
    private void handleDashboard(MouseEvent event) {
        SceneNavigator.getInstance().loadScene(DASHBOARD_VIEW_FXML, null);
    }

    @FXML
    private void handleBookingHistory(MouseEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/booking-history.fxml", null);
    }

    @FXML
    private void handleFavorites(MouseEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/favorites.fxml", null);
    }

    @FXML
    private void handleOrganizerEvents(MouseEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/organizer/events.fxml", null);
    }

    @FXML
    private void handleOrganizerAttendees(MouseEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/organizer/attendees.fxml", null);
    }

    @FXML
    void handleProfileInfos(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/profile.fxml", null);
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        SceneNavigator.getInstance().loadScene(SETTINGS_FXML, null);
    }

    @FXML
    private void handlePaymentMethods(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/payment-methods.fxml", null);
    }

    @FXML
    private void handleSupport(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/common/support.fxml", null);
    }

    @FXML
    private void handleChat(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/chat/chat.fxml", null);
    }

    @FXML
    private void handleCollaborativeItinerary() {
        SceneNavigator.getInstance()
                .loadScene("/it/unical/ea_project_javafx/fxml/traveler/collaborative-itinerary.fxml", null);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        long generation = UserSession.getInstance().getGeneration();
        Runnable finish = () -> {
            if (generation == UserSession.getInstance().getGeneration())
                finishLogout();
        };
        ApiService.callDetailed(ApiService.BASE_URL + "/api/users/logout", null, ApiService.Method.POST,
                r -> finish.run(), message -> finish.run(), null);
    }

    private void finishLogout() {
        stopNotificationPolling();
        UserSession.getInstance().clear();
        NotificationPollingService.getInstance().clearSessionState();
        TokenStorage.clear();
        SceneNavigator.getInstance().loadScene(MAIN_VIEW_FXML, null);
    }
}
