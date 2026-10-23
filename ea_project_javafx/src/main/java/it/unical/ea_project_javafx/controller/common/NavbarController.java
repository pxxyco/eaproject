package it.unical.ea_project_javafx.controller.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.TokenStorage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NavbarController {

    private static final String MAIN_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/home/mainview.fxml";
    private static final String LOGIN_FXML = "/it/unical/ea_project_javafx/fxml/login/login.fxml";
    private static final String DASHBOARD_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/profile/dashboard-view.fxml";
    private static final String SETTINGS_FXML = "/it/unical/ea_project_javafx/fxml/profile/settings.fxml";
    private static final String NOTIFICATIONS_URL = ApiService.BASE_URL + "/api/notifications";

    @FXML private Label loginLabel;
    @FXML private MenuButton userMenuButton;
    @FXML private ImageView userAvatarImageView;
    @FXML private Label userAvatarInitialsLabel;
    @FXML private Label userMenuLabel;
    @FXML private javafx.scene.layout.StackPane userAvatarContainer;
    @FXML private javafx.scene.layout.StackPane notificationBellContainer;
    @FXML private Label notificationBadge;

    @FXML private Label dashboardNavItem;
    @FXML private Label travelerBookingsNavItem;
    @FXML private Label travelerFavoritesNavItem;
    @FXML private Label organizerEventsNavItem;
    @FXML private Label organizerAttendeesNavItem;
    @FXML private MenuItem paymentsMenuItem;

    private Timeline notificationPolling;
    private Timeline toastTimeout;
    private Popup notificationPopup;
    private Popup toastPopup;
    private final Set<Long> seenNotificationIds = new HashSet<>();
    private boolean initialNotificationPoll = true;
    private long avatarLoadRequestId = 0;

    @FXML
    public void initialize() {
        bindVisibilityToManaged(loginLabel);
        bindVisibilityToManaged(userMenuButton);
        bindVisibilityToManaged(notificationBellContainer);
        bindVisibilityToManaged(dashboardNavItem);
        bindVisibilityToManaged(travelerBookingsNavItem);
        bindVisibilityToManaged(travelerFavoritesNavItem);
        bindVisibilityToManaged(organizerEventsNavItem);
        bindVisibilityToManaged(organizerAttendeesNavItem);
        configureUserAvatar();
        updateNavbarState();
        if (notificationBellContainer != null) {
            notificationBellContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) stopNotificationPolling();
                else if (UserSession.getInstance().isLoggedIn()) startNotificationPolling();
            });
        }
    }

    private void bindVisibilityToManaged(Node node) {
        if (node != null) node.managedProperty().bind(node.visibleProperty());
    }

    public void updateNavbarState() {
        boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
        boolean isOrganizer = isLoggedIn && "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        boolean isTraveler = isLoggedIn && !isOrganizer;

        if (loginLabel != null) loginLabel.setVisible(!isLoggedIn);
        if (userMenuButton != null) {
            userMenuButton.setVisible(isLoggedIn);
            if (isLoggedIn) {
                String username = UserSession.getInstance().getUsername();
                if (userMenuLabel != null) userMenuLabel.setText(username == null || username.isBlank() ? "Utente" : username);
                userMenuButton.setAccessibleText("Menu utente " + (username == null ? "" : username));
                updateUserAvatar();
            } else {
                if (userMenuLabel != null) userMenuLabel.setText("Utente");
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
        if (notificationBellContainer != null) notificationBellContainer.setVisible(isLoggedIn);
        if (dashboardNavItem != null) dashboardNavItem.setVisible(isLoggedIn);
        if (travelerBookingsNavItem != null) travelerBookingsNavItem.setVisible(isTraveler);
        if (travelerFavoritesNavItem != null) travelerFavoritesNavItem.setVisible(isTraveler);
        if (organizerEventsNavItem != null) organizerEventsNavItem.setVisible(isOrganizer);
        if (organizerAttendeesNavItem != null) organizerAttendeesNavItem.setVisible(isOrganizer);
        if (paymentsMenuItem != null) paymentsMenuItem.setText(isOrganizer ? "Incassi & Payout" : "Metodi di Pagamento");

        if (isLoggedIn) startNotificationPolling(); else stopNotificationPolling();
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

    /** Keeps the navbar avatar in sync with UserSession after an upload, replacement or removal. */
    private void updateUserAvatar() {
        long requestId = ++avatarLoadRequestId;
        UserSession session = UserSession.getInstance();
        String displayName = session.getFullName();
        if (displayName == null || displayName.isBlank()) displayName = session.getUsername();
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
        if (photoUrl == null || photoUrl.isBlank() || userAvatarImageView == null) return;

        String resolvedUrl = ApiService.resolveUrl(photoUrl) + "?v=" + System.currentTimeMillis();
        Image image = new Image(resolvedUrl, 28, 28, true, true, true);
        image.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (hasError) Platform.runLater(() -> {
                if (requestId == avatarLoadRequestId) showAvatarImage(null);
            });
        });
        image.progressProperty().addListener((obs, oldValue, progress) -> {
            if (progress.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    if (requestId == avatarLoadRequestId) showAvatarImage(image);
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
        if (name == null || name.isBlank()) return "U";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        if (parts.length == 1) return first;
        return first + parts[parts.length - 1].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }

    private void startNotificationPolling() {
        if (notificationPolling != null || !UserSession.getInstance().isLoggedIn()) return;
        pollNotifications();
        notificationPolling = new Timeline(new KeyFrame(Duration.seconds(20), event -> pollNotifications()));
        notificationPolling.setCycleCount(Timeline.INDEFINITE);
        notificationPolling.play();
    }

    private void stopNotificationPolling() {
        if (notificationPolling != null) { notificationPolling.stop(); notificationPolling = null; }
        if (notificationPopup != null) { notificationPopup.hide(); notificationPopup = null; }
        if (toastPopup != null) { toastPopup.hide(); toastPopup = null; }
        if (toastTimeout != null) { toastTimeout.stop(); toastTimeout = null; }
    }

    private void pollNotifications() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        ApiService.call(NOTIFICATIONS_URL + "/unread", null, ApiService.Method.GET, response -> {
            try {
                JsonArray unread = JsonParser.parseString(response.body()).getAsJsonArray();
                updateNotificationBadge(unread.size());
                ArrayList<JsonObject> newlyReceived = new ArrayList<>();
                for (JsonElement element : unread) {
                    if (!element.isJsonObject()) continue;
                    JsonObject notification = element.getAsJsonObject();
                    if (!notification.has("notificationId") || notification.get("notificationId").isJsonNull()) continue;
                    long id = notification.get("notificationId").getAsLong();
                    if (initialNotificationPoll) seenNotificationIds.add(id);
                    else if (seenNotificationIds.add(id)) newlyReceived.add(notification);
                }
                if (initialNotificationPoll) initialNotificationPoll = false;
                else if (!newlyReceived.isEmpty()) showToast(newlyReceived.get(0));
                if (seenNotificationIds.size() > 600) {
                    seenNotificationIds.clear();
                    for (JsonElement element : unread) if (element.isJsonObject() && element.getAsJsonObject().has("notificationId"))
                        seenNotificationIds.add(element.getAsJsonObject().get("notificationId").getAsLong());
                }
            } catch (Exception ignored) { /* Un errore temporaneo non deve interrompere la navbar. */ }
        }, () -> { /* La navbar resta utilizzabile anche se il server è temporaneamente irraggiungibile. */ }, null);
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadge == null) return;
        notificationBadge.setText(count > 99 ? "99+" : Integer.toString(count));
        notificationBadge.setVisible(count > 0);
        notificationBadge.setManaged(count > 0);
    }

    @FXML
    private void handleNotifications(MouseEvent event) {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide(); notificationPopup = null; return;
        }
        openNotificationTray();
    }

    private void openNotificationTray() {
        if (notificationBellContainer == null || notificationBellContainer.getScene() == null) return;
        ApiService.call(NOTIFICATIONS_URL + "/unread", null, ApiService.Method.GET, response -> {
            try {
                JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                showNotificationTray(array);
            } catch (Exception ex) { showToast("Notifications", "Unable to read notifications."); }
        }, () -> showToast("Notifications", "Unable to load notifications."), null);
    }

    private void showNotificationTray(JsonArray notifications) {
        if (notificationPopup != null) notificationPopup.hide();
        VBox root = new VBox(10);
        root.getStyleClass().add("notification-tray");
        root.setPadding(new Insets(16));
        root.setPrefWidth(360);
        root.setMaxHeight(430);

        HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Notifications"); title.getStyleClass().add("notification-tray-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAll = new Button("Mark all read"); markAll.getStyleClass().add("notification-mark-all");
        markAll.setOnAction(e -> ApiService.call(NOTIFICATIONS_URL + "/read-all", "{}", ApiService.Method.POST,
                response -> { if (notificationPopup != null) notificationPopup.hide(); notificationPopup = null; pollNotifications(); openNotificationTray(); },
                () -> showToast("Notifications", "Unable to update notifications."), null));
        header.getChildren().addAll(title, spacer, markAll);
        root.getChildren().addAll(header, new Separator());

        VBox cards = new VBox(9);
        int shown = Math.min(notifications.size(), 15);
        for (int i = 0; i < shown; i++) {
            JsonElement element = notifications.get(i);
            if (element.isJsonObject()) cards.getChildren().add(createNotificationCard(element.getAsJsonObject()));
        }
        if (cards.getChildren().isEmpty()) {
            Label empty = new Label("No notifications");
            empty.getStyleClass().add("notification-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(24, 8, 24, 8));
            cards.setAlignment(Pos.CENTER);
            cards.setPrefHeight(300);
            cards.getChildren().add(empty);
        }
        ScrollPane scroll = new ScrollPane(cards); scroll.setFitToWidth(true); scroll.setPrefViewportHeight(330);
        scroll.setMaxHeight(350); scroll.getStyleClass().add("notification-scroll");
        root.getChildren().add(scroll);

        notificationPopup = new Popup(); notificationPopup.setAutoHide(true); notificationPopup.setHideOnEscape(true);
        notificationPopup.getContent().add(root);
        positionPopup(notificationPopup, popupAnchor(), 360);
    }

    private VBox createNotificationCard(JsonObject notification) {
        VBox card = new VBox(6); card.getStyleClass().add("notification-card");
        if (notification.has("read") && !notification.get("read").isJsonNull() && notification.get("read").getAsBoolean())
            card.getStyleClass().add("notification-card-read");
        String titleText = jsonString(notification, "title", "Notification");
        String messageText = jsonString(notification, "message", "");
        String type = jsonString(notification, "type", "NOTICE");
        long id = notification.has("notificationId") ? notification.get("notificationId").getAsLong() : -1;
        Label title = new Label(titleText); title.getStyleClass().add("notification-card-title"); title.setWrapText(true);
        Label message = new Label(messageText); message.getStyleClass().add("notification-card-message"); message.setWrapText(true);
        card.getChildren().addAll(title, message);
        if ("LIST_INVITATION".equals(type) && id > 0) {
            HBox actions = new HBox(8); actions.setAlignment(Pos.CENTER_RIGHT);
            Button preview = new Button("Visualizza dettagli"); preview.getStyleClass().add("notification-mark-all");
            Button accept = new Button("Accept"); accept.getStyleClass().add("notification-accept");
            Button reject = new Button("Decline"); reject.getStyleClass().add("notification-decline");
            preview.setOnAction(e -> previewInvitation(id));
            accept.setOnAction(e -> respondToInvitation(id, true));
            reject.setOnAction(e -> respondToInvitation(id, false));
            HBox previewRow = new HBox(preview); previewRow.setAlignment(Pos.CENTER_LEFT);
            actions.getChildren().addAll(accept, reject); card.getChildren().addAll(previewRow, actions);
        } else if (id > 0 && !(notification.has("read") && notification.get("read").getAsBoolean())) {
            Button markRead = new Button("Mark as read"); markRead.getStyleClass().add("notification-mark-all");
            markRead.setOnAction(e -> ApiService.call(NOTIFICATIONS_URL + "/" + id + "/read", "{}", ApiService.Method.POST,
                    response -> {
                        if (notificationPopup != null) { notificationPopup.hide(); notificationPopup = null; }
                        pollNotifications();
                        openNotificationTray();
                    },
                    () -> showToast("Notifications", "Unable to mark as read."), null));
            HBox row = new HBox(markRead); row.setAlignment(Pos.CENTER_RIGHT); card.getChildren().add(row);
        }
        return card;
    }

    private void previewInvitation(long notificationId) {
        ApiService.call(NOTIFICATIONS_URL + "/" + notificationId + "/invitation-preview", null, ApiService.Method.GET,
                response -> {
                    try {
                        JsonObject list = JsonParser.parseString(response.body()).getAsJsonObject();
                        StringBuilder details = new StringBuilder();
                        details.append("Lista: ").append(jsonString(list, "listName", "Lista condivisa")).append('\n');
                        details.append("Organizzatore della lista: @").append(jsonString(list, "organizerUsername", "non disponibile")).append('\n');
                        details.append("Permesso previsto: ").append(jsonString(list, "permission", "VIEW")).append("\n\n");
                        JsonArray trips = list.has("trips") && list.get("trips").isJsonArray() ? list.getAsJsonArray("trips") : new JsonArray();
                        if (trips.size() == 0) details.append("La lista non contiene ancora viaggi.\n");
                        for (JsonElement tripElement : trips) {
                            if (!tripElement.isJsonObject()) continue;
                            JsonObject trip = tripElement.getAsJsonObject();
                            details.append("✈ ").append(jsonString(trip, "title", "Viaggio senza titolo"));
                            String city = jsonString(trip, "destinationCity", "");
                            String country = jsonString(trip, "destinationCountry", "");
                            String destination = city + (city.isBlank() || country.isBlank() ? "" : ", ") + country;
                            if (!destination.isBlank()) details.append(" — ").append(destination);
                            details.append('\n').append("  Organizzatore del viaggio: @")
                                    .append(jsonString(trip, "organizerUsername", "non disponibile")).append('\n');
                            String start = jsonString(trip, "startDate", "");
                            String end = jsonString(trip, "endDate", "");
                            if (!start.isBlank() || !end.isBlank()) details.append("  Date: ").append(start.isBlank() ? "?" : start)
                                    .append(" → ").append(end.isBlank() ? "?" : end).append('\n');
                            JsonArray stages = trip.has("stages") && trip.get("stages").isJsonArray() ? trip.getAsJsonArray("stages") : new JsonArray();
                            if (stages.size() == 0) details.append("  Nessuna tappa inserita.\n");
                            for (JsonElement stageElement : stages) {
                                if (!stageElement.isJsonObject()) continue;
                                JsonObject stage = stageElement.getAsJsonObject();
                                details.append("    Giorno ").append(jsonString(stage, "day", "?"))
                                        .append(" · tappa ").append(jsonString(stage, "orderInDay", "?"))
                                        .append(": ").append(jsonString(stage, "title", "Tappa"));
                                String location = jsonString(stage, "locationName", "");
                                if (location.isBlank()) location = jsonString(stage, "city", "");
                                String stageCountry = jsonString(stage, "country", "");
                                if (!location.isBlank()) details.append(" — ").append(location);
                                if (!stageCountry.isBlank() && !stageCountry.equals(location)) details.append(", ").append(stageCountry);
                                String timeStart = jsonString(stage, "startTime", "");
                                String timeEnd = jsonString(stage, "endTime", "");
                                if (!timeStart.isBlank() || !timeEnd.isBlank()) details.append(" ( ")
                                        .append(timeStart.isBlank() ? "--:--" : timeStart).append("–")
                                        .append(timeEnd.isBlank() ? "--:--" : timeEnd).append(" )");
                                details.append('\n');
                                String description = jsonString(stage, "description", "");
                                if (!description.isBlank()) details.append("      ").append(description).append('\n');
                            }
                            details.append('\n');
                        }
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Dettagli invito");
                        alert.setHeaderText("Anteprima della lista condivisa");
                        alert.setContentText(details.toString());
                        alert.getDialogPane().setPrefWidth(600);
                        alert.getDialogPane().setPrefHeight(520);
                        alert.showAndWait();
                    } catch (Exception ex) {
                        showToast("List invitation", "Unable to read the list details.");
                    }
                },
                () -> showToast("List invitation", "Unable to load the list preview."), null);
    }

    private void respondToInvitation(long id, boolean accept) {
        String action = accept ? "accept" : "reject";
        ApiService.call(NOTIFICATIONS_URL + "/" + id + "/" + action, "{}", ApiService.Method.POST,
                response -> {
                    if (notificationPopup != null) { notificationPopup.hide(); notificationPopup = null; }
                    pollNotifications();
                    showToast("List invitation", accept ? "Invitation accepted." : "Invitation declined.");
                    openNotificationTray();
                },
                () -> showToast("List invitation", accept ? "Unable to accept the invitation." : "Unable to decline the invitation."), null);
    }

    private void showToast(JsonObject notification) {
        showToast(jsonString(notification, "title", "New notification"), jsonString(notification, "message", "You have a new notification."));
    }

    private void showToast(String titleText, String messageText) {
        if (notificationBellContainer == null || notificationBellContainer.getScene() == null) return;
        if (toastPopup != null) toastPopup.hide();
        if (toastTimeout != null) toastTimeout.stop();
        VBox toast = new VBox(5); toast.getStyleClass().add("notification-toast"); toast.setPadding(new Insets(14)); toast.setPrefWidth(330);
        Label title = new Label(titleText); title.getStyleClass().add("notification-toast-title"); title.setWrapText(true);
        Label message = new Label(messageText); message.getStyleClass().add("notification-toast-message"); message.setWrapText(true);
        toast.getChildren().addAll(title, message);
        toastPopup = new Popup(); toastPopup.setAutoHide(false); toastPopup.getContent().add(toast);
        positionPopup(toastPopup, popupAnchor(), 330);
        toastTimeout = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (toastPopup != null) { toastPopup.hide(); toastPopup = null; }
        }));
        toastTimeout.setCycleCount(1); toastTimeout.play();
    }

    private Node popupAnchor() {
        return userMenuButton != null ? userMenuButton : notificationBellContainer;
    }

    private void positionPopup(Popup popup, Node anchor, double width) {
        if (anchor == null || anchor.getScene() == null || anchor.getScene().getWindow() == null) return;
        Window owner = anchor.getScene().getWindow();
        Runnable reposition = () -> {
            if (!popup.isShowing() || anchor.getScene() == null) return;
            Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
            if (bounds == null) return;
            popup.setX(Math.max(8, bounds.getMaxX() - width));
            popup.setY(bounds.getMaxY() + 8);
        };
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) return;
        popup.show(owner, Math.max(8, bounds.getMaxX() - width), bounds.getMaxY() + 8);

        // A Popup JavaFX is una finestra separata: seguiamo spostamento e ridimensionamento
        // della finestra principale, così la tendina resta ancorata alla navbar.
        javafx.beans.value.ChangeListener<Number> windowListener = (obs, oldValue, newValue) -> reposition.run();
        owner.xProperty().addListener(windowListener);
        owner.yProperty().addListener(windowListener);
        owner.widthProperty().addListener(windowListener);
        owner.heightProperty().addListener(windowListener);
        javafx.beans.value.ChangeListener<Bounds> boundsListener = (obs, oldValue, newValue) -> reposition.run();
        javafx.beans.value.ChangeListener<javafx.scene.transform.Transform> transformListener = (obs, oldValue, newValue) -> reposition.run();
        anchor.boundsInLocalProperty().addListener(boundsListener);
        anchor.localToSceneTransformProperty().addListener(transformListener);
        popup.setOnHidden(event -> {
            owner.xProperty().removeListener(windowListener);
            owner.yProperty().removeListener(windowListener);
            owner.widthProperty().removeListener(windowListener);
            owner.heightProperty().removeListener(windowListener);
            anchor.boundsInLocalProperty().removeListener(boundsListener);
            anchor.localToSceneTransformProperty().removeListener(transformListener);
        });
        if (popup.getScene() != null) {
            String css = getClass().getResource("/it/unical/ea_project_javafx/css/common/notifications.css").toExternalForm();
            if (!popup.getScene().getStylesheets().contains(css)) popup.getScene().getStylesheets().add(css);
        }
    }

    private String jsonString(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }

    @FXML void handleHome(MouseEvent event) { SceneNavigator.getInstance().loadScene(MAIN_VIEW_FXML, null); }
    @FXML void handleLogin(MouseEvent event) { SceneNavigator.getInstance().loadScene(LOGIN_FXML, null); }
    @FXML private void handleDashboard(MouseEvent event) { SceneNavigator.getInstance().loadScene(DASHBOARD_VIEW_FXML, null); }
    @FXML private void handleBookings(MouseEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/bookings.fxml", null); }
    @FXML private void handleFavorites(MouseEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/favorites.fxml", null); }
    @FXML private void handleOrganizerEvents(MouseEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/organizer/events.fxml", null); }
    @FXML private void handleOrganizerAttendees(MouseEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/organizer/attendees.fxml", null); }
    @FXML void handleProfileInfos(ActionEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/profile.fxml", null); }
    @FXML private void handleSettings(ActionEvent event) { SceneNavigator.getInstance().loadScene(SETTINGS_FXML, null); }
    @FXML private void handlePaymentMethods(ActionEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/payment-methods.fxml", null); }
    @FXML private void handleSupport(ActionEvent event) { SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/common/support.fxml", null); }

    @FXML
    void handleLogout(ActionEvent event) {
        stopNotificationPolling();
        UserSession.getInstance().clear();
        TokenStorage.clear();
        SceneNavigator.getInstance().loadScene(MAIN_VIEW_FXML, null);
    }
}
