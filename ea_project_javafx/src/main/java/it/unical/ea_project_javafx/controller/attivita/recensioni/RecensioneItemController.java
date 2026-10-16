package it.unical.ea_project_javafx.controller.attivita.recensioni;

import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.DateTimeUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RecensioneItemController {

    @FXML private VBox containerRisposta;

    @FXML private MenuButton btnActions;

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

    private ReviewDTO review;

    @Setter private Consumer<ReviewDTO> onEdit ;
    @Setter private Consumer <ReviewDTO> onDelete;
    @Setter private Consumer<ReviewDTO> onReply;


    @FXML
    public void initialize(){
        // TODO: fix answer logic, containerRisposta hidden temporarily for UI testing
        containerRisposta.setManaged(false);
        containerRisposta.setVisible(false);

        stars = List.of(star1, star2, star3, star4, star5);
    }

    public void setData(ReviewDTO review, Long placeOwnerId) {
        this.review = review;
        lblTesto.setText(review.getComment());
        lblNomeUtente.setText(review.getUsername());
        handleStar(review.getRating());

        String data = DateTimeUtils.formatDateTime(review.getCreatedAt().toString());
        if (review.getUpdatedAt() != null) data += " (modificata)";
        lblData.setText(data);

        setPermissions(placeOwnerId);
    }


    private void handleStar(int value) {

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
    public void handleEdit(ActionEvent actionEvent) {
        if (onEdit != null) onEdit.accept(review);
    }

    @FXML
    public void handleDelete(ActionEvent actionEvent) {
        if(onDelete != null) onDelete.accept(review);
    }

    @FXML
    public void handleReply(ActionEvent actionEvent) {
        if (onReply != null) onReply.accept(review);
    }

    private void setPermissions(Long placeOwnerId) {
        UserSession session = UserSession.getInstance();
        Long myId = session.isLoggedIn() ? session.getId() : null;

        boolean isAuthor = myId != null && Objects.equals(review.getUserId(), myId);
        boolean isPlaceOwner = myId != null
                && "ORGANIZER".equalsIgnoreCase(session.getRole())
                && Objects.equals(placeOwnerId, myId);
        boolean canReply = isPlaceOwner && review.getResponse() == null;

        btnActions.setVisible(isAuthor);
        btnActions.setManaged(isAuthor);

        btnRispondi.setVisible(canReply);
        btnRispondi.setManaged(canReply);
    }
}
