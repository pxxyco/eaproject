package it.unical.ea_project_javafx.controller.organizer;

import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class EventsController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private FlowPane offersFlowPane;

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();

        typeFilterCombo.getItems().setAll("Tutti", "Viaggi", "Attività");
        typeFilterCombo.getSelectionModel().selectFirst();

        statusFilterCombo.getItems().setAll("Tutti", "PUBLISHED", "DRAFT", "CANCELLED");
        statusFilterCombo.getSelectionModel().selectFirst();
    }

    private void loadNavbar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/common/navbar.fxml"));
            Parent navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateNewOffer(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/organizer/create-offer.fxml"));
            Parent modal = loader.load();
            StackPane root = (StackPane) navbarContainer.getScene().getRoot();
            root.getChildren().add(modal);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}