package it.unical.ea_project_javafx.controller.activity.reviews;

import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.util.DateTimeUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ReviewItemController {

    @FXML private VBox containerRisposta;

    // TODO: implement button's action
    @FXML private Button btnAzioni;
    @FXML private Button btnRispondi;
    @FXML private HBox containerStelle;
    @FXML private Label lblData;
    @FXML private Label lblNomeUtente;
    @FXML private Label lblTesto;
    @FXML private Button star1;
    @FXML private Button star2;
    @FXML private Button star3;
    @FXML private Button star4;
    @FXML private Button star5;

    List<Button> stars ;
    final private String FULL_STAR = "★";
    final private String EMPTY_STAR = "☆";


    @FXML
    public void initialize() {

        // TODO: fix answer logic, containerRisposta hidden temporarily for UI testing
        containerRisposta.setManaged(false);
        containerRisposta.setVisible(false);

        stars = List.of(star1, star2, star3, star4, star5);

    }

    public void setData(ReviewDTO review) {

        lblTesto.setText(review.getComment());
        lblData.setText(DateTimeUtils.formatDateTime(review.getCreatedAt().toString()));
        lblNomeUtente.setText(review.getUsername());
        handleStar(review.getRating());

    }


    private void handleStar(int value) {

        for(int i = 0; i < stars.size(); i++) {

            if(i+1 <= value){
                stars.get(i).getStyleClass().add("stella-piena");
                stars.get(i).setText(FULL_STAR);
            } else {
                stars.get(i).getStyleClass().clear();
                stars.get(i).getStyleClass().add("stella");
                stars.get(i).setText(EMPTY_STAR);
            }

        }

    }

}
