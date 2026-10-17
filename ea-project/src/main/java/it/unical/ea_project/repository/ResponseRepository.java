package it.unical.ea_project.repository;

import it.unical.ea_project.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {

    // find all responses associated with a review ID in the list and not deleted
    List<Response> findByReview_ReviewIdInAndDeletedFalse(List<Long> reviewIds);

    //trova le risposte associate a una specifica recensione e non eliminate
    List<Response> findByReviewReviewIdAndDeletedFalse(Long reviewId);
}