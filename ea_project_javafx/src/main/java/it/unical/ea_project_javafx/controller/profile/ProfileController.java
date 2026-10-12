package it.unical.ea_project_javafx.controller.profile;

import it.unical.ea_project_javafx.controller.home.NavbarController;
import it.unical.ea_project_javafx.model.MainNavigator;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.WallpaperService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ProfileController implements MainNavigator {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private VBox dashboardContainer;
    @FXML private Label greetingLabel;
    @FXML private NavbarController navbarController;

    private final UserSession userSession = UserSession.getInstance();

    @FXML
    public void initialize() {
        bgImageView.setEffect(new GaussianBlur(18));
        loadBackground();
        setupBackgroundResize();
        populateUserDetails();
        loadNavbar();
        loadRoleDashboard();
    }

    private void populateUserDetails() {
        String displayName = firstNonBlank(userSession.getFullName(), userSession.getUsername(), "Utente");
        greetingLabel.setText("È bello rivederti, " + displayName + "!");
    }

    private void loadRoleDashboard() {
        String role = userSession.getRole();
        String dashboardFile = "ORGANIZER".equalsIgnoreCase(role)
                ? "organizer_dashboard.fxml"
                : "traveler_dashboard.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/profile/" + dashboardFile));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/it/unical/ea_project_javafx/fxml/home/navbar-logged-in.fxml"));
            Parent navbar = loader.load();
            navbarController = loader.getController();
            if (navbarController != null) {
                navbarController.setNavigator(this);
            }
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBackground() {
        new Thread(() -> {
            String imageUrl = WallpaperService.getDailyWallpaperUrl();
            Platform.runLater(() -> bgImageView.setImage(new Image(
                    imageUrl, 1920, 1080, true, true, true)));
        }).start();
    }

    private void setupBackgroundResize() {
        bgImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && bgImageView.getParent() instanceof StackPane parentPane) {
                bgImageView.fitWidthProperty().bind(parentPane.widthProperty());
                bgImageView.fitHeightProperty().bind(parentPane.heightProperty());
            }
        });
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return fallback;
    }

    private String firstNonBlank(String first, String fallback) {
        return firstNonBlank(first, null, fallback);
    }

    @Override
    public void goToHome(Node sourceNode) {
        SceneNavigator.getInstance().goToHome("/it/unical/ea_project_javafx/fxml/home/mainview.fxml");
    }

    @Override
    public void goToLogin(Node sourceNode) {
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/login/login.fxml");
    }

    @Override
    public void goToProfile(Node sourceNode) {
        // La pagina corrente e gia il profilo dell'utente autenticato.
    }
}