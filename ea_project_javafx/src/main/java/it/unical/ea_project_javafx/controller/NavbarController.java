package it.unical.ea_project_javafx.controller;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.TokenStorage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;

public class NavbarController {

    private static final String MAIN_VIEW_FXML = "/it/unical/ea_project_javafx/fxml/home/mainview.fxml";
    private static final String LOGIN_FXML = "/it/unical/ea_project_javafx/fxml/login/login.fxml";
    private static final String DASHBOARD_FXML = "/it/unical/ea_project_javafx/fxml/profile/profile.fxml";

    @FXML private Label loginLabel;
    @FXML private Label organizerNavItem;
    @FXML private MenuButton userMenuButton;

    @FXML
    public void initialize() {

        bindVisibilityToManaged(loginLabel);
        bindVisibilityToManaged(userMenuButton);
        bindVisibilityToManaged(organizerNavItem);

        updateNavbarState();
    
    }

    /**
     * Collega la proprietà managed alla proprietà visible di un nodo.
     * Quando visible è false, il nodo non occuperà spazio nell'HBox.
     */
    private void bindVisibilityToManaged(Node node) {

        if (node != null) {
            node.managedProperty().bind(node.visibleProperty());
        }

    }

    /**
     * Configura la visibilità e il testo dei componenti in base al contesto dell'utente loggato.
     */
    public void updateNavbarState() {
        boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());

        if (loginLabel != null) {
            loginLabel.setVisible(!isLoggedIn);
        }

        if (userMenuButton != null) {
            userMenuButton.setVisible(isLoggedIn);
            if (isLoggedIn) {
                userMenuButton.setText(UserSession.getInstance().getUsername());
            }
        }

        // Visibilità "Area Organizzatore"
        if (organizerNavItem != null) {
            organizerNavItem.setVisible(isLoggedIn && isOrganizer);
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
    private void handleDashboard(ActionEvent event) {
        SceneNavigator.getInstance().loadScene(DASHBOARD_FXML, null);
    }

    @FXML
    void handleProfileInfos(ActionEvent event) {
        // SceneNavigator.getInstance().loadScene(null, null);
    }

    @FXML
    private void handleBookings(ActionEvent event) {
        // SceneNavigator.getInstance().loadScene(null, null);
    }

    @FXML
    private void handleFavorites(ActionEvent event) {
        // SceneNavigator.getInstance().loadScene(null, null);
    }

    @FXML
    private void handleSettings(ActionEvent event) {
        // SceneNavigator.getInstance().loadScene(null, null);
    }

    @FXML
    private void handlePaymentMethods(ActionEvent event) {
        // SceneNavigator.getInstance().loadScene(null, null);
    }

    @FXML
    private void handleSupport(ActionEvent event) {
        // SceneNavigator.getInstance().loadScene(null, null);
    }

    @FXML
    void handleLogout(ActionEvent event) {

        UserSession.getInstance().clear();
        TokenStorage.clear();
        SceneNavigator.getInstance().loadScene(MAIN_VIEW_FXML, null);

    }
}