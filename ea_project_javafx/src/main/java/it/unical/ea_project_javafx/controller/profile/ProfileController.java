package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class ProfileController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;

    @FXML private Label fullNameLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label usernameValueLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label roleValueLabel;

    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        populateUserData();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml")
            );
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateUserData() {
        String name = userSession.getFullName();
        if (name == null || name.isBlank()) {
            name = userSession.getUsername();
        }
        fullNameLabel.setText(name);
        
        String role = userSession.getRole() != null ? userSession.getRole().toUpperCase() : "UTENTE";
        roleBadgeLabel.setText(role);
        roleValueLabel.setText("ORGANIZER".equals(role) ? "Organizzatore / Partner" : "Viaggiatore (Traveler)");

        usernameValueLabel.setText(userSession.getUsername() != null ? userSession.getUsername() : "-");
        emailValueLabel.setText(userSession.getEmail() != null ? userSession.getEmail() : "-");
    }

    @FXML
    private void handleGoToSettings(ActionEvent event) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/settings.fxml", null);
    }
}