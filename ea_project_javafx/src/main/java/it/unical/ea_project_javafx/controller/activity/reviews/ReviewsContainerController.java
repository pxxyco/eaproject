package it.unical.ea_project_javafx.controller.activity.reviews;

import it.unical.ea_project_javafx.controller.activity.ActivitySectionController;
import it.unical.ea_project_javafx.controller.activity.TripSectionController;
import it.unical.ea_project_javafx.dto.activity.ActivityDTO;
import it.unical.ea_project_javafx.dto.review.ReviewDTO;
import it.unical.ea_project_javafx.dto.trip.TripDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.service.review.ReviewService;
import it.unical.ea_project_javafx.ui.dialog.PopupFactory;
import java.util.List;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import java.util.Objects;
import javafx.scene.layout.VBox;
import lombok.Setter;

public class ReviewsContainerController implements ActivitySectionController, TripSectionController {

    @FXML
    private Button btnNew;
    @FXML
    private VBox bodyContainer;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;

    final private String path = "/it/unical/ea_project_javafx/fxml";
    private TripDTO trip;
    private ActivityDTO activity;
    List<ReviewDTO> reviews;

    private int page;
    private boolean reviewEligible;
    private ReviewDTO ownReview;
    final private int PAGE_SIZE = 5;

    @Setter
    private Runnable onNewReviewRequested;
    @Setter
    private Consumer<ReviewDTO> onEditReviewRequested;
    @Setter
    private Consumer<ReviewDTO> onReplyRequested;
    @Setter
    private Runnable onReviewsChanged;

    @FXML
    public void initialize() {
        page = 0;

        UserSession session = UserSession.getInstance();
        boolean canReview = session.isLoggedIn() && "TRAVELER".equalsIgnoreCase(session.getRole());
        btnNew.setVisible(canReview);
        btnNew.setManaged(canReview);
        updateReviewAction();
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
        if (trip != null) {
            ReviewService.getTripReview(
                    trip.getTripId(),
                    reviews -> {
                        this.reviews = reviews;
                        if (reviews != null)
                            setReviews();
                    },
                    null,
                    null);
        }
        if (activity != null) {
            ReviewService.getActivityReview(
                    activity.getActivityId(),
                    reviews -> {
                        this.reviews = reviews;
                        if (reviews != null)
                            setReviews();
                    },
                    null,
                    null);
        }
    }

    private void setReviews() {

        ownReview = reviews.stream().filter(review -> Objects.equals(review.getUserId(), UserSession.getInstance().getId())).findFirst().orElse(null);
        updateReviewAction();
        bodyContainer.getChildren().clear();

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, reviews.size());

        boolean isFirstPage = page <= 0;
        boolean isLastPage = endIndex >= reviews.size();

        previousButton.setDisable(isFirstPage);
        previousButton.setVisible(!isFirstPage);

        nextButton.setDisable(isLastPage);
        nextButton.setVisible(!isLastPage);

        for (int i = startIndex; i < endIndex; i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/activity/reviews/review-item.fxml"));
                Node node = loader.load();

                ReviewItemController controller = loader.getController();
                controller.setData(reviews.get(i), getPlaceOwnerId());
                controller.setOnEdit(r -> {
                    if (onEditReviewRequested != null)
                        onEditReviewRequested.accept(r);
                });
                controller.setOnDelete(this::deleteReview);

                bodyContainer.getChildren().add(node);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setReviewEligible(boolean eligible) {
        reviewEligible = eligible;
        updateReviewAction();
    }

    private void updateReviewAction() {
        if (btnNew == null) return;
        btnNew.setText("+");
        btnNew.setAccessibleText(ownReview != null ? "Modifica la tua recensione" : "Aggiungi recensione");
        btnNew.setDisable(ownReview == null && !reviewEligible);
        btnNew.setTooltip(new Tooltip(ownReview != null ? "Puoi aggiornare la tua recensione anche dopo altre partecipazioni."
            : "Serve una prenotazione confermata e non annullata per questa esperienza."));
    }

    @FXML
    public void handleNewButton(ActionEvent actionEvent) {
        if (ownReview != null && onEditReviewRequested != null) { onEditReviewRequested.accept(ownReview); return; }
        if (!reviewEligible) return;
        if (onNewReviewRequested != null) {
            onNewReviewRequested.run();
        }
    }

    @FXML
    public void handlePreviousPage(ActionEvent actionEvent) {
        if (page > 0)
            page--;
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
        var root = PopupFactory.resolveRoot(bodyContainer);
        if (root == null)
            return;
        PopupFactory.showConfirmation(root, "Elimina recensione",
                "Vuoi eliminare questa recensione? L'operazione non può essere annullata.",
                PopupFactory.Size.COMPACT,
                () -> ReviewService.deleteReview(
                        review.getReviewId(),
                        () -> {
                            reviews.removeIf(r -> r.getReviewId().equals(review.getReviewId()));
                            if (page > 0 && page * PAGE_SIZE >= reviews.size())
                                page--;
                            setReviews();
                            if (onReviewsChanged != null)
                                onReviewsChanged.run();
                        },
                        null,
                        null));
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
        if (activity != null)
            return activity.getCreatedByUserId();
        if (trip != null)
            return trip.getCreatedByUserId();
        return null;
    }
}
