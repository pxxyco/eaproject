package it.unical.ea_project_javafx.controller.traveler;

import it.unical.ea_project_javafx.util.ViewSupport;
import com.google.gson.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.controller.home.ExperienceCardController;
import it.unical.ea_project_javafx.dto.home.*;

public class RecommendationsController {
    @FXML
    private StackPane navbarContainer;
    @FXML
    private TilePane tripCards, activityCards;
    @FXML
    private Label statusLabel, subtitle;
    @FXML
    private Button refreshButton;

    @FXML
    public void initialize() {
        ViewSupport.loadNavbar(navbarContainer, message -> ViewSupport.show(statusLabel, message));
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        refreshButton.setDisable(true);
        statusLabel.setText("Cerco esperienze per te…");
        ApiService.callDetailed(ApiService.BASE_URL + "/api/recommendations/mine?limit=6", null, ApiService.Method.GET,
                r -> {
                    refreshButton.setDisable(false);
                    try {
                        var picks = JsonParser.parseString(r.body()).getAsJsonObject();
                        tripCards.getChildren().clear();
                        activityCards.getChildren().clear();
                        subtitle.setText(picks.get("personalized").getAsBoolean()
                                ? "I tuoi interessi, preferiti e viaggi precedenti guidano queste scelte."
                                : "Comincia a esplorare: i consigli miglioreranno con i tuoi preferiti e le tue esperienze.");
                        for (var item : picks.getAsJsonArray("trips"))
                            add(tripCards, item.getAsJsonObject(), true);
                        for (var item : picks.getAsJsonArray("activities"))
                            add(activityCards, item.getAsJsonObject(), false);
                        statusLabel.setText(tripCards.getChildren().isEmpty() && activityCards.getChildren().isEmpty()
                                ? "Al momento non ci sono nuove esperienze disponibili. Riprova più tardi."
                                : "");
                    } catch (Exception ex) {
                        statusLabel.setText("Non è stato possibile leggere i consigli. Riprova.");
                    }
                }, message -> {
                    refreshButton.setDisable(false);
                    statusLabel.setText(message);
                }, null);
    }

    private void add(TilePane target, JsonObject pick, boolean trip) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/it/unical/ea_project_javafx/fxml/home/experience-card.fxml"));
        Parent card = loader.load();
        ExperienceCardController controller = loader.getController();
        if (trip)
            controller.setData((TripHomeDTO) ApiService.fromJson(pick.get("item").toString(), TripHomeDTO.class));
        else
            controller
                    .setData((ActivityHomeDTO) ApiService.fromJson(pick.get("item").toString(), ActivityHomeDTO.class));
        Label reason = new Label(pick.get("reason").getAsString());
        reason.setWrapText(true);
        reason.setMaxWidth(315);
        reason.getStyleClass().add("chat-meta");
        target.getChildren().add(new VBox(10, card, reason));
    }
}
