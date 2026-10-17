package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import it.unical.ea_project_javafx.util.TokenStorage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainViewController {

    @FXML private ImageView bgImageView;
    @FXML private VBox cardsContainer;
    @FXML private StackPane navbarContainer;
    @FXML private SearchBarController searchBarController;
    @FXML private ExperienceListController experienceListController;

    @FXML
    public void initialize() {

        BackgroundHelper.setupBackground(bgImageView);
        
        restoreSession();
        if (searchBarController != null && experienceListController != null) {
            searchBarController.setExperienceListController(experienceListController);
            experienceListController.setSearchBarController(searchBarController);
        }

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

    private void loadNavbar() {

        if (navbarContainer == null) {
            return;
        }

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbarView = loader.load();

            navbarContainer.getChildren().setAll(navbarView);
            if (experienceListController != null) {
                experienceListController.loadHomeForCurrentUser();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}