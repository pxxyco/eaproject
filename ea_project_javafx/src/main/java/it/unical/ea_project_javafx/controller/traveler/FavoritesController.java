package it.unical.ea_project_javafx.controller.traveler;

import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.BackgroundHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FavoritesController {

    @FXML private ImageView bgImageView;
    @FXML private StackPane navbarContainer;
    @FXML private Label statusMessageLabel;

    @FXML private TextField newListNameField;
    @FXML private ComboBox<String> newListVisibilityCombo;

    @FXML private ListView<String> listsListView;
    @FXML private Label selectedListNameLabel;
    @FXML private Label selectedListVisibilityLabel;
    @FXML private Button shareListButton;
    @FXML private FlowPane favoritesCardsContainer;

    @FXML
    public void initialize() {
        BackgroundHelper.setupBackground(bgImageView);
        loadNavbar();

        newListVisibilityCombo.getItems().setAll("PRIVATE", "SHARED");
        newListVisibilityCombo.getSelectionModel().selectFirst();

        listsListView.setItems(FXCollections.observableArrayList("Preferiti Generali (Privata)", "Vacanze Estate (Condivisa)"));
        listsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedListNameLabel.setText(newV);
                selectedListVisibilityLabel.setText(newV.contains("Condivisa") ? "Visibilità: CONDIVISA (Permessi specifici)" : "Visibilità: PRIVATA");
                shareListButton.setVisible(newV.contains("Condivisa"));
            }
        });
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
    private void handleCreateList() {
        String name = newListNameField.getText();
        String visibility = newListVisibilityCombo.getValue();

        if (name == null || name.isBlank()) {
            showError("Inserisci un nome per la lista.");
            return;
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("visibility", visibility);

        ApiService.call(ApiService.BASE_URL + "/api/favorites/lists", ApiService.toJson(payload), ApiService.Method.POST,
                response -> Platform.runLater(() -> {
                    showSuccess("Lista '" + name + "' creata con successo!");
                    listsListView.getItems().add(name + " (" + visibility + ")");
                    newListNameField.clear();
                }),
                () -> Platform.runLater(() -> showError("Impossibile creare la lista.")),
                null
        );
    }

    @FXML
    private void handleShareList() {
        showSuccess("Link di condivisione e gestione permessi (VIEW/EDIT) copiato negli appunti.");
    }

    private void showError(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-error");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.getStyleClass().setAll("status-message", "status-success");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
    }
}