package it.unical.ea_project_javafx.controller.activity.reviews;

import it.unical.ea_project_javafx.dto.review.ResponseDTO;
import it.unical.ea_project_javafx.dto.review.ReviewDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.review.ResponseService;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import it.unical.ea_project_javafx.util.DateTimeUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Setter;

public class ReviewItemController {

    @FXML
    private VBox containerRisposta;
    @FXML
    private VBox containerFormRisposta;
    @FXML
    private TextArea txtRisposta;

    @FXML
    private MenuButton btnActions;
    @FXML
    private Button btnRispondi;

    @FXML
    private Label lblData;
    @FXML
    private Label lblNomeUtente;
    @FXML
    private Label lblTesto;
    @FXML
    private Button star1;
    @FXML
    private Button star2;
    @FXML
    private Button star3;
    @FXML
    private Button star4;
    @FXML
    private Button star5;

    List<Button> stars;
    final private String FULL_STAR = "★";
    final private String EMPTY_STAR = "☆";
    final private String RESPONSE_FXML = "/it/unical/ea_project_javafx/fxml/activity/reviews/review-response-item.fxml";

    private ReviewDTO review;
    private boolean isPlaceOwner;
    private boolean editingResponse;

    @Setter
    private Consumer<ReviewDTO> onEdit;
    @Setter
    private Consumer<ReviewDTO> onDelete;

    @FXML
    public void initialize() {
        stars = List.of(star1, star2, star3, star4, star5);
        showForm(false);

    }

    public void setData(ReviewDTO review, Long placeOwnerId) {
        this.review = review;
        lblTesto.setText(review.getComment());
        lblData.setText(DateTimeUtils.formatDateTime(review.getCreatedAt().toString()));
        lblNomeUtente.setText(review.getUsername());
        handleStar(review.getRating());

        String data = DateTimeUtils.formatDateTime(review.getCreatedAt().toString());
        if (review.getUpdatedAt() != null)
            data += " (modificata)";
        lblData.setText(data);

        setPermissions(placeOwnerId);
    }

    private void handleStar(int value) {

        for (int i = 0; i < stars.size(); i++) {

            if (i + 1 <= value) {
                if (!stars.get(i).getStyleClass().contains("stella-piena")) {
                    stars.get(i).getStyleClass().add("stella-piena");
                }
                stars.get(i).setText(FULL_STAR);
            } else {
                stars.get(i).getStyleClass().remove("stella-piena");
                stars.get(i).setText(EMPTY_STAR);
            }

        }

    }

    @FXML
    public void handleEdit(ActionEvent actionEvent) {
        if (onEdit != null)
            onEdit.accept(review);
    }

    @FXML
    public void handleDelete(ActionEvent actionEvent) {
        if (onDelete != null)
            onDelete.accept(review);
    }

    private void setPermissions(Long placeOwnerId) {
        UserSession session = UserSession.getInstance();
        Long myId = session.isLoggedIn() ? session.getId() : null;

        boolean isAuthor = myId != null && Objects.equals(review.getUserId(), myId);
        isPlaceOwner = myId != null
                && "ORGANIZER".equalsIgnoreCase(session.getRole())
                && Objects.equals(placeOwnerId, myId);

        btnActions.setVisible(isAuthor);
        btnActions.setManaged(isAuthor);

        refreshResponse();
    }

    private void refreshResponse() {
        ResponseDTO response = review.getResponse();
        boolean hasResponse = response != null;

        containerRisposta.getChildren().clear();
        if (hasResponse) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(RESPONSE_FXML));
                Node node = loader.load();

                ResponseItemController controller = loader.getController();
                controller.setData(response);
                controller.setOnEdit(this::editResponse);
                controller.setOnDelete(this::deleteResponse);

                containerRisposta.getChildren().add(node);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        containerRisposta.setVisible(hasResponse);
        containerRisposta.setManaged(hasResponse);

        boolean canReply = isPlaceOwner && !hasResponse;
        btnRispondi.setVisible(canReply);
        btnRispondi.setManaged(canReply);

        showForm(false);
    }

    private void showForm(boolean show) {
        containerFormRisposta.setVisible(show);
        containerFormRisposta.setManaged(show);
    }

    @FXML
    public void handleReply(ActionEvent actionEvent) {
        editingResponse = false;
        txtRisposta.clear();
        btnRispondi.setVisible(false);
        btnRispondi.setManaged(false);
        showForm(true);
    }

    private void editResponse(ResponseDTO response) {
        editingResponse = true;
        txtRisposta.setText(response.getComment());
        containerRisposta.setVisible(false);
        containerRisposta.setManaged(false);
        showForm(true);
    }

    private void deleteResponse(ResponseDTO response) {
        var root = PopupFactory.resolveRoot(containerRisposta);
        if (root == null)
            return;
        PopupFactory.showConfirmation(root, "Elimina risposta",
                "Vuoi eliminare questa risposta? L'operazione non può essere annullata.",
                PopupFactory.Size.COMPACT,
                () -> ResponseService.delete(
                        response.getId(),
                        () -> {
                            review.setResponse(null);
                            refreshResponse();
                        },
                        null));
    }

    @FXML
    public void handleCancelResponse(ActionEvent actionEvent) {
        refreshResponse();
    }

    @FXML
    public void handleSendResponse(ActionEvent actionEvent) {
        String text = txtRisposta.getText() == null ? "" : txtRisposta.getText().trim();
        if (text.isEmpty())
            return;

        if (editingResponse) {
            ResponseService.update(
                    review.getResponse().getId(),
                    text,
                    updated -> {
                        review.setResponse(updated);
                        refreshResponse();
                    },
                    null);
        } else {
            ResponseService.create(
                    review.getReviewId(),
                    text,
                    created -> {
                        review.setResponse(created);
                        refreshResponse();
                    },
                    null);
        }
    }
}
