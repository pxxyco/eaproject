package it.unical.ea_project_javafx.controller.activity.reviews;

import it.unical.ea_project_javafx.controller.activity.ActivitySectionController;
import it.unical.ea_project_javafx.controller.activity.TripSectionController;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.dto.TripDTO;

import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.ReviewService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

public class ReviewsContainerController implements ActivitySectionController, TripSectionController{

    @FXML private Button btnNew;
    @FXML private VBox bodyContainer;
    @FXML private Button previousButton;
    @FXML private Button nextButton;

    final private String path = "/it/unical/ea_project_javafx/fxml";
    private TripDTO trip;
    private ActivityDTO activity;
    List<ReviewDTO> reviews;

    private int page;
    final private int PAGE_SIZE = 5;

    @Setter
    private Runnable onNewReviewRequested;
    @Setter private Consumer<ReviewDTO> onEditReviewRequested;
    @Setter private Consumer<ReviewDTO> onReplyRequested;
    @Setter private Runnable onReviewsChanged;

    @FXML
    public void initialize(){
        page = 0;

        UserSession session = UserSession.getInstance();
        boolean canReview = session.isLoggedIn() && "TRAVELER".equalsIgnoreCase(session.getRole());
        btnNew.setVisible(canReview);
        btnNew.setManaged(canReview);
    }

    @Override
    public void setData(ActivityDTO activity) {
        this.activity = activity;
        getReviews();
    }

    @Override
    public void setData(TripDTO trip) {
        this.trip = trip;
        getReviews();
    }

    private void getReviews() {
        if(trip != null){
            ReviewService.getTripReview(
                    trip.getTripId(),
                    reviews -> {
                        this.reviews = reviews;
                        if (reviews != null) setReviews();
                    },
                    null,
                    null
            );
        }
        if(activity != null){
            ReviewService.getActivityReview(
                    activity.getActivityId(),
                    reviews -> {
                        this.reviews = reviews;
                        if(reviews != null) setReviews();
                    },
                    null,
                    null
            );
        }
    }

    private void setReviews() {

        bodyContainer.getChildren().clear();

        int startIndex = page*PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, reviews.size());

        boolean isFirstPage = page <= 0;
        boolean isLastPage = endIndex >= reviews.size();

        previousButton.setDisable(isFirstPage);
        previousButton.setVisible(!isFirstPage);

        nextButton.setDisable(isLastPage);
        nextButton.setVisible(!isLastPage);


        for(int i = startIndex; i < endIndex; i++){
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/activity/reviews/review-item.fxml"));
                Node node = loader.load();

                ReviewItemController controller = loader.getController();
                controller.setData(reviews.get(i), getPlaceOwnerId());
                controller.setOnEdit(r -> { if (onEditReviewRequested != null) onEditReviewRequested.accept(r); });
                controller.setOnDelete(this::deleteReview);

                bodyContainer.getChildren().add(node);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    public void handleNewButton(ActionEvent actionEvent) {
        if(onNewReviewRequested!=null){
            onNewReviewRequested.run();
        }
    }

    @FXML
    public void handlePreviousPage(ActionEvent actionEvent) {
        if(page > 0) page--;
        setReviews();
    }

    @FXML
    public void handleNextPage(ActionEvent actionEvent) {
        page++;
        setReviews();
    }

    public void addReview(ReviewDTO review) {
        reviews.addFirst(review);
        page = 0;
        setReviews();
    }

    private void deleteReview(ReviewDTO review) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi eliminare questa recensione?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Elimina recensione");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        ReviewService.deleteReview(
                review.getReviewId(),
                () -> {
                    reviews.removeIf(r -> r.getReviewId().equals(review.getReviewId()));
                    if (page > 0 && page * PAGE_SIZE >= reviews.size()) page--;
                    setReviews();
                    if (onReviewsChanged != null) onReviewsChanged.run();
                },
                null,
                null
        );
    }

    public void updateReview(ReviewDTO updated) {
        for (int i = 0; i < reviews.size(); i++) {
            if (reviews.get(i).getReviewId().equals(updated.getReviewId())) {
                reviews.set(i, updated);
                break;
            }
        }
        setReviews();
    }

    private Long getPlaceOwnerId() {
        if (activity != null) return activity.getCreatedByUserId();
        if (trip != null) return trip.getCreatedByUserId();
        return null;
    }
}
