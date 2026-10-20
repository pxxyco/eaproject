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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        listsListView.setItems(FXCollections.observableArrayList());
        loadLists();
        listsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedListNameLabel.setText(newV);
                selectedListVisibilityLabel.setText("Lista selezionata: " + newV);
                shareListButton.setVisible(newV.contains("[SHARED]"));
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

    private void loadLists() {

        ApiService.call(
            ApiService.BASE_URL + "/api/favorite-lists/mine", 
            null, 
            ApiService.Method.GET, 
            response -> Platform.runLater(() -> {

                try {

                        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
                        listsListView.getItems().clear();
                        for (JsonElement el : arr) {

                            JsonObject obj = el.getAsJsonObject();
                            String name = obj.has("name") && !obj.get("name").isJsonNull()
                                    ? obj.get("name").getAsString() : "Lista #" + obj.get("id").getAsString();
                            String visibility = obj.has("visibility") && !obj.get("visibility").isJsonNull()
                                    ? obj.get("visibility").getAsString() : "PRIVATE";
                            listsListView.getItems().add(name + " [" + visibility + "]#" + obj.get("id").getAsString());

                        }

                        if (listsListView.getItems().isEmpty()) {
                            selectedListNameLabel.setText("Nessuna lista");
                            selectedListVisibilityLabel.setText("");
                            shareListButton.setVisible(false);
                        }

                } catch (Exception e) {
                    showError("Risposta non valida dal server durante il caricamento delle liste.");
                }

            }), 
            () -> Platform.runLater(() -> showError("Impossibile caricare le liste preferite.")), 
            null
        );

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
        payload.put("name", name.trim());
        payload.put("visibility", visibility);

        ApiService.call(
            ApiService.BASE_URL + "/api/favorite-lists", 
            ApiService.toJson(payload), 
            ApiService.Method.POST,
            response -> Platform.runLater(() -> {
                showSuccess("Lista '" + name + "' creata con successo!");
                newListNameField.clear();
                loadLists();
            }),
            () -> Platform.runLater(() -> showError("Impossibile creare la lista.")),
            null
        );

    }

    @FXML
    private void handleShareList() {
        showError("La condivisione richiede l'ID dell'utente destinatario; usa la gestione permessi dedicata quando disponibile.");
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