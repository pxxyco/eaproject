package it.unical.ea_project_javafx.controller.organizer;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/** The navbar's offers entry uses the same CRUD workflow as the dashboard. */
public class EventsController {
    @FXML
    private ImageView bgImageView;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private StackPane offersContainer;

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        ViewSupport.loadNavbar(navbarContainer);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/organizer/organizer-dashboard.fxml"));
            Parent dashboard = loader.load();
            OrganizerDashboardController controller = loader.getController();
            controller.setUser(UserSession.getInstance());
            offersContainer.getChildren().setAll(dashboard);
        } catch (IOException exception) {
            offersContainer.getChildren()
                    .setAll(new Label("Impossibile caricare le offerte. Torna alla dashboard e riprova."));
        }
    }
}
