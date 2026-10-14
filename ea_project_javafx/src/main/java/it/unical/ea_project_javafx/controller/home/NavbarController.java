package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.model.MainNavigator;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.TokenStorage;
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
        // Naviga direttamente alla home principale
        SceneNavigator.getInstance().goToHome("/it/unical/ea_project_javafx/fxml/home/mainview.fxml");
    }

    @FXML
    void handleLogin(MouseEvent event) {
        // Naviga alla schermata di login
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/login/login.fxml");
    }

    @FXML
    void handleOrganizer(MouseEvent event) {
        if ("ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/profile.fxml");
        }
    }

    @FXML
    void handleProfilo(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/profile.fxml");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.getInstance().clear();
        TokenStorage.clear();

        // Ricarica la mainview resettando lo stato
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/home/mainview.fxml");
    }
}
