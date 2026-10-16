package it.unical.ea_project_javafx.controller.attivita.recensioni;

import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.service.ReviewService;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class NewRecensioneController {

    @FXML private HBox containerStelle;

    @FXML private Button star1;

    @FXML private Button star2;

    @FXML private Button star3;

    @FXML private Button star4;

    @FXML private Button star5;

    @FXML private TextArea txtCommento;

    @FXML private StackPane root;

    private int value;
    private Long tripId;
    private Long activityId;

    @Setter
    private Runnable onClose;
    @Setter
    private Consumer<ReviewDTO> onReviewCreated;
    @Setter
    private Consumer<ReviewDTO> onReviewUpdated;

    private ReviewDTO editing;


    List<Button> stars ;
    final private String FULL_STAR = "★";
    final private String EMPTY_STAR = "☆";

    @FXML
    private void initialize() {
        stars = List.of(star1, star2, star3, star4, star5);
    }

    public void setType(Long tripId, Long activityId) {
        this.tripId = tripId;
        this.activityId = activityId;

        reset();
    }

    public void setEditMode(ReviewDTO review) {
        this.editing = review;
        handleStar(review.getRating());
        txtCommento.setText(review.getComment());
    }


    @FXML
    void handleCloseButton(ActionEvent event) {
        if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    void handleSendButton(ActionEvent event) {

        if(value < 1)
            return;

        if (editing != null) {
            editing.setRating(value);
            editing.setComment(txtCommento.getText());

            ReviewService.update(
                    editing,
                    updated -> {
                        if (onReviewUpdated != null) onReviewUpdated.accept(updated);
                        if (onClose != null) onClose.run();
                    },
                    null,
                    null
            );
            return;
        }


        ReviewDTO review = ReviewDTO.builder()
                .rating(value)
                .comment(txtCommento.getText())
                .activityId(activityId)
                .tripId(tripId)
                .build();

        ReviewService.create(
                review,
                created -> {
                    if(onReviewCreated != null) onReviewCreated.accept(created);
                    if(onClose != null) onClose.run();
                },
                () -> {
                    // TODO: set error message
                },
                null);
    }

    public void reset(){
        editing = null;
        handleStar(0);
        txtCommento.clear();
    }

    @FXML
    void star1Button(ActionEvent event) {
        handleStar(1);
    }

    @FXML
    void starButton2(ActionEvent event) {
        handleStar(2);
    }

    @FXML
    void starButton3(ActionEvent event) {
        handleStar(3);
    }

    @FXML
    void starButton4(ActionEvent event) {
        handleStar(4);
    }

    @FXML
    void starButton5(ActionEvent event) {
        handleStar(5);
    }

    private void handleStar(int value) {
        this.value = value;

        for(int i = 0; i < stars.size(); i++) {
            if(i+1 <= value){
                stars.get(i).getStyleClass().add("stella-piena");
                stars.get(i).setText(FULL_STAR);

            }
            else {
                stars.get(i).getStyleClass().clear();
                stars.get(i).getStyleClass().add("stella");
                stars.get(i).setText(EMPTY_STAR);
            }
        }
    }

    @FXML
    public void handleMainFocus(MouseEvent mouseEvent) {
        root.requestFocus();
    }
}
