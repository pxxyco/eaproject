package it.unical.ea_project_javafx.controller.profile;

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

public class DashboardViewController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private VBox dashboardContent;

    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();
        loadRoleDashboard();
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

    private void loadRoleDashboard() {
        String role = userSession.getRole();
        String dashboardPath = "ORGANIZER".equalsIgnoreCase(role)
                ? "/it/unical/ea_project_javafx/fxml/organizer/organizer-dashboard.fxml"
                : "/it/unical/ea_project_javafx/fxml/traveler/traveler-dashboard.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(dashboardPath));
            Parent dashboard = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ProfileDashboardController profileDashboardController) {
                profileDashboardController.setUser(userSession);
            }
            dashboardContent.getChildren().setAll(dashboard);
        } catch (IOException e) {
            dashboardContent.getChildren().setAll(new Label("Impossibile caricare la dashboard dell'utente."));
            e.printStackTrace();
        }
    }
}