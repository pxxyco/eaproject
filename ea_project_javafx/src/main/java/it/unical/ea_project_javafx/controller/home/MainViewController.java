package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.model.MainNavigator;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.SceneNavigator;
import it.unical.ea_project_javafx.util.TokenStorage;
import it.unical.ea_project_javafx.util.WallpaperService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainViewController implements MainNavigator {

    @FXML private ImageView bgImageView;
    @FXML private VBox cardsContainer;
    @FXML private StackPane navbarContainer;
    @FXML private SearchBarController searchBarController;
    @FXML private ExperienceListController experienceListController;

    @FXML
    public void initialize() {
        if (bgImageView != null) {
            bgImageView.setEffect(new GaussianBlur(18));
        }

        loadBackground();
        setupBackgroundResize();

        if (searchBarController != null && experienceListController != null) {
            searchBarController.setExperienceListController(experienceListController);
            experienceListController.setSearchBarController(searchBarController);
        }

        restoreSession();
    }

    private void restoreSession() {
        String refreshToken = TokenStorage.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            loadNavbar();
            return;
        }

        ApiService.restoreSessionFromStoredRefreshToken(
                this::loadNavbar,
                () -> {
                    UserSession.getInstance().clear();
                    TokenStorage.clear();
                    loadNavbar();
                },
                this::loadNavbar
        );
    }

    private void loadBackground() {
        if (bgImageView == null) return;

        Thread thread = new Thread(() -> {
            String imageUrl = WallpaperService.getDailyWallpaperUrl();

            Platform.runLater(() -> {
                if (imageUrl == null || imageUrl.isBlank()) return;

                Image background = new Image(
                        imageUrl,
                        1920,
                        1080,
                        true,
                        true,
                        true
                );

                bgImageView.setImage(background);
            });
        }, "wallpaper-loader");

        thread.setDaemon(true);
        thread.start();
    }

    private void loadNavbar() {
        if (navbarContainer == null) return;

        try {
            boolean isLoggedIn = UserSession.getInstance().isLoggedIn();
            String fxmlFile = isLoggedIn
                    ? "/it/unical/ea_project_javafx/fxml/home/navbar-logged-in.fxml"
                    : "/it/unical/ea_project_javafx/fxml/home/navbar-logged-out.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent navbarView = loader.load();

            NavbarController navbarController = loader.getController();
            if (navbarController != null) {
                navbarController.setNavigator(this);
            }

            navbarContainer.getChildren().setAll(navbarView);
            if (experienceListController != null) {
                experienceListController.loadHomeForCurrentUser();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupBackgroundResize() {
        if (bgImageView == null) return;
        bgImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                bindBackgroundToParent();
            }
        });
        if (bgImageView.getScene() != null) {
            bindBackgroundToParent();
        }
    }

    private void bindBackgroundToParent() {
        if (bgImageView.getParent() instanceof StackPane parentPane) {
            bgImageView.fitWidthProperty().unbind();
            bgImageView.fitHeightProperty().unbind();

            bgImageView.fitWidthProperty().bind(parentPane.widthProperty());
            bgImageView.fitHeightProperty().bind(parentPane.heightProperty());
        }
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
        SceneNavigator.getInstance().loadScene("/it/unical/ea_project_javafx/fxml/profile/profile.fxml");
    }
}