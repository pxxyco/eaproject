package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.security.TokenStorage;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.api.ApiService;
import it.unical.ea_project_javafx.ui.ViewSupport;
import it.unical.ea_project_javafx.ui.background.BackgroundHelper;
import it.unical.ea_project_javafx.ui.scroll.StickySearch;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainViewController implements AutoCloseable {
    @FXML
    private ScrollPane homeScroll;
    @FXML
    private VBox stickySearchDock;
    private StickySearch stickySearch;
    private boolean closed;

    @FXML
    private ImageView bgImageView;

    @FXML
    private StackPane navbarContainer;
    @FXML
    private SearchBarController searchBarController;
    @FXML
    private ExperienceListController experienceListController;

    @FXML
    public void initialize() {

        BackgroundHelper.setupBackground(bgImageView);

        if (searchBarController != null && experienceListController != null) {
            searchBarController.setExperienceListController(experienceListController);
            experienceListController.setSearchBarController(searchBarController);
            stickySearch = new StickySearch(homeScroll, stickySearchDock, searchBarController, navbarContainer);
        }
        restoreSession();

    }

    private void restoreSession() {

        if (UserSession.getInstance().isLoggedIn()) {
            loadNavbar();
            return;
        }
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
                this::loadNavbar);

    }

    private void loadNavbar() {
        if (closed || navbarContainer == null)
            return;
        if (ViewSupport.loadNavbar(navbarContainer) != null && experienceListController != null) {
            experienceListController.loadHomeForCurrentUser();
        }
    }

    @Override
    public void close() {
        closed = true;
        if (stickySearch != null)
            stickySearch.close();
        if (searchBarController != null)
            searchBarController.close();
        if (experienceListController != null)
            experienceListController.close();
    }
}
