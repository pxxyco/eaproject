package it.unical.ea_project_javafx.controller.activity.reviews;

import it.unical.ea_project_javafx.controller.activity.ActivitySectionController;
import it.unical.ea_project_javafx.controller.activity.TripSectionController;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import it.unical.ea_project_javafx.service.ReviewService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.util.List;

public class ReviewsContainerController implements ActivitySectionController, TripSectionController{

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

    @FXML
    public void initialize(){
        page = 0;
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
                Node r = loader.load();

                ReviewItemController controller = loader.getController();
                controller.setData(reviews.get(i));

                bodyContainer.getChildren().add(r);

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
}
