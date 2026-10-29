package it.unical.ea_project.repository.review;

import it.unical.ea_project.domain.review.Response;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    List<Response> findByReview_ReviewIdInAndDeletedFalse(List<Long> reviewIds);

    // trova le risposte associate a una specifica recensione e non eliminate
    List<Response> findByReviewReviewIdAndDeletedFalse(Long reviewId);
}
