package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.controller.NavbarController;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ProfileController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private VBox dashboardContainer;
    @FXML private Label greetingLabel;
    @FXML private NavbarController navbarController;

    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {

        BackgroundHelper.setupBackground(bgImageView);
        populateUserDetails();
        loadNavbar();
        loadRoleDashboard();

    }

    private void populateUserDetails() {

        greetingLabel.setText("È bello rivederti, " 
            + firstNonBlank(userSession.getFullName(), userSession.getUsername()) 
            + "!"
        );

    }

    private void loadRoleDashboard() {

        String role = userSession.getRole();
        String dashboardFile = "ORGANIZER".equalsIgnoreCase(role)
                ? "organizer-dashboard.fxml"
                : "traveler-dashboard.fxml";

        try {

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/it/unical/ea_project_javafx/fxml/profile/" + dashboardFile)
            );
            Parent dashboard = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ProfileDashboardController profileDashboardController) {
                profileDashboardController.setUser(userSession);
            }

            dashboardContainer.getChildren().setAll(dashboard);

        } catch (IOException e) {
            dashboardContainer.getChildren().setAll(new Label("Impossibile caricare la dashboard del profilo."));
            e.printStackTrace();
        }

    }

    private void loadNavbar() {

        try {

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/it/unical/ea_project_javafx/fxml/navbar.fxml")
            );
            Parent navbar = loader.load();
            navbarController = loader.getController();

            navbarContainer.getChildren().setAll(navbar);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String firstNonBlank(String first, String last) {
        
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (last != null && !last.isBlank()) {
            return last;
        }

        return "User";

    }

}