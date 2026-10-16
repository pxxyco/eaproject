package it.unical.ea_project_javafx.controller.common;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.TokenStorage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;

public class NavbarController {

    private static final String MAIN_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/home/mainview.fxml";
    private static final String LOGIN_FXML = "/it/unical/ea_project_javafx/fxml/login/login.fxml";
    private static final String DASHBOARD_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/profile/dashboard-view.fxml";
    private static final String SETTINGS_FXML = "/it/unical/ea_project_javafx/fxml/profile/settings.fxml";

    @FXML private Label loginLabel;
    @FXML private MenuButton userMenuButton;

    // Elementi Navbar Traveler
    @FXML private Label dashboardNavItem;
    @FXML private Label travelerBookingsNavItem;
    @FXML private Label travelerFavoritesNavItem;

    // Elementi Navbar Organizer
    @FXML private Label organizerEventsNavItem;
    @FXML private Label organizerAttendeesNavItem;

    // Voce menu a tendina dinamica
    @FXML private MenuItem paymentsMenuItem;

    @FXML
    public void initialize() {

        bindVisibilityToManaged(loginLabel);
        bindVisibilityToManaged(userMenuButton);
        
        bindVisibilityToManaged(dashboardNavItem);
        bindVisibilityToManaged(travelerBookingsNavItem);
        bindVisibilityToManaged(travelerFavoritesNavItem);
        
        bindVisibilityToManaged(organizerEventsNavItem);
        bindVisibilityToManaged(organizerAttendeesNavItem);

        updateNavbarState();
    }

    private void bindVisibilityToManaged(Node node) {
        if (node != null) {
            node.managedProperty().bind(node.visibleProperty());
        }
    }

    public void updateNavbarState() {

        boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
        boolean isOrganizer = isLoggedIn && "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        boolean isTraveler = isLoggedIn && !isOrganizer;

        if (loginLabel != null) {
            loginLabel.setVisible(!isLoggedIn);
        }

        if (userMenuButton != null) {
            userMenuButton.setVisible(isLoggedIn);
            if (isLoggedIn) {
                userMenuButton.setText(UserSession.getInstance().getUsername());
            }
        }

        if (dashboardNavItem != null) {
            dashboardNavItem.setVisible(isLoggedIn);
        }

        if (travelerBookingsNavItem != null) {
            travelerBookingsNavItem.setVisible(isTraveler);
        }
        if (travelerFavoritesNavItem != null) {
            travelerFavoritesNavItem.setVisible(isTraveler);
        }

        if (organizerEventsNavItem != null) {
            organizerEventsNavItem.setVisible(isOrganizer);
        }
        if (organizerAttendeesNavItem != null) {
            organizerAttendeesNavItem.setVisible(isOrganizer);
        }

        if (paymentsMenuItem != null) {
            if (isOrganizer) {
                paymentsMenuItem.setText("Incassi & Payout");
            } else {
                paymentsMenuItem.setText("Metodi di Pagamento");
            }
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
    private void handleBookings(MouseEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/traveler/bookings.fxml", null);
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
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().clear();
        TokenStorage.clear();
        SceneNavigator.getInstance().loadScene(MAIN_VIEW_FXML, null);
    }
}