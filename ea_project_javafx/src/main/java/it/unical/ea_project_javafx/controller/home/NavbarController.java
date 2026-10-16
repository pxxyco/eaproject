package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.model.MainNavigator;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import lombok.Setter;

//sarebbe la top bar della home
public class NavbarController {

    @Setter
    private MainNavigator navigator;

    @FXML private MenuButton userMenuButton;
    @FXML private Label organizerNavItem;

    @FXML
    public void initialize() {
        if (userMenuButton != null && UserSession.getInstance().isLoggedIn()) {
            userMenuButton.setText(UserSession.getInstance().getUsername());
        }
        if (organizerNavItem != null) {
            boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
            organizerNavItem.setVisible(isOrganizer);
            organizerNavItem.setManaged(isOrganizer);
        }
    }

    @FXML
    void handleHome(MouseEvent event) {
        if (navigator != null) {
            navigator.goToHome((Node) event.getSource());
        }
    }

    @FXML
    void handleLogin(MouseEvent event) {
        if (navigator != null) {
            navigator.goToLogin((Node) event.getSource());
        }
    }

    @FXML
    void handleOrganizer(MouseEvent event) {
        if (navigator != null && "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            navigator.goToProfile((Node) event.getSource());
        }
    }

    @FXML
    void handleProfilo(ActionEvent event) {
        if (navigator != null) {
            Node sourceNode = (userMenuButton != null) ? userMenuButton : (Node) event.getSource();
            navigator.goToProfile(sourceNode);
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().clear();
        it.unical.ea_project_javafx.util.TokenStorage.clear();

        if (userMenuButton != null) {
            SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/home/mainview.fxml");
        }
    }
}
