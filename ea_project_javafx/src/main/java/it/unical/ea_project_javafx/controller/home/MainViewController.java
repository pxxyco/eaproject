package it.unical.ea_project_javafx.controller.home;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;

public class MainViewController implements AutoCloseable {
    @FXML
    private ImageView bgImageView;
    @FXML
    private ScrollPane homeScroll;
    @FXML
    private HBox homeHero;
    @FXML
    private VBox discoverySections, stickySearchDock;
    @FXML
    private Parent experienceList;
    @FXML
    private StackPane navbarContainer;
    @FXML
    private SearchBarController searchBarController;
    @FXML
    private ExperienceListController experienceListController;
    private boolean closed;
    private DiscoveryFeed feed;
    private StickySearch stickySearch;

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        navbarContainer.heightProperty().addListener((o, a, b) -> ((VBox) homeScroll.getContent()).setPadding(
                new javafx.geometry.Insets(b.doubleValue() + 24, 20, 48, 20)));
        searchBarController.setExperienceListController(experienceListController);
        experienceListController.setSearchBarController(searchBarController);
        experienceListController.attachScroll(homeScroll);
        // Home selections are independent of personal bookings and available to guests.
        experienceListController.setEditorialHome(true);
        experienceListController.setBrowsingModeChanged(show -> {
            homeHero.setVisible(show);
            homeHero.setManaged(show);
            discoverySections.setVisible(show);
            discoverySections.setManaged(show);
            experienceList.setVisible(!show);
            experienceList.setManaged(!show);
            if (show) {
                homeScroll.setVvalue(0);
                feed.schedule();
            } else {
                Platform.runLater(() -> ScrollSupport.reveal(experienceList,
                        navbarContainer.getHeight() + searchBarController.controls().getHeight() + 38));
            }
        });
        experienceList.setVisible(false);
        experienceList.setManaged(false);
        feed = new DiscoveryFeed(homeScroll, discoverySections, this::browse);
        stickySearch = new StickySearch(homeScroll, stickySearchDock, searchBarController, navbarContainer);
        restoreSession();
    }

    private void browse(String category) {
        searchBarController.reset();
        searchBarController.selectCategory(category);
        experienceListController.search("TRIP".equals(category) ? ExperienceListController.SearchMode.TRIP
                : ExperienceListController.SearchMode.ACTIVITY, "", "TRIP".equals(category) ? null : category, null);
    }

    @FXML
    private void handleExploreTrips() {
        browse("TRIP");
    }

    @FXML
    private void handleExploreNature() {
        browse("EXCURSION");
    }

    @FXML
    private void handleFocusSearch() {
        ScrollSupport.reveal(searchBarController.slot(), 18);
        searchBarController.focusSearch();
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
        ApiService.restoreSessionFromStoredRefreshToken(this::loadNavbar, () -> {
            UserSession.getInstance().clear();
            TokenStorage.clear();
            loadNavbar();
        }, this::loadNavbar);
    }

    private void loadNavbar() {
        if (closed)
            return;
        try {
            Parent view = FXMLLoader
                    .load(getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            navbarContainer.getChildren().setAll(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        feed.start();
    }

    @Override
    public void close() {
        closed = true;
        if (feed != null)
            feed.close();
        if (stickySearch != null)
            stickySearch.close();
        experienceListController.close();
        searchBarController.close();
    }
}
