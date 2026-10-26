package it.unical.ea_project.service.review;

import it.unical.ea_project.domain.review.Review;
import it.unical.ea_project.dto.review.ReviewDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface ReviewService {

    ReviewDTO create(ReviewDTO review);

    Review findById(Long id);

    List<Review> findByUser(Long userId);

    /**
     * Returns only the authenticated user's non-deleted reviews, with organizer
     * responses when present.
     */
    List<ReviewDTO> findByUserAsDto(Long userId);

    @Nullable
    List<ReviewDTO> findByActivity(Long activityId);

    @Nullable
    List<ReviewDTO> findByTrip(Long tripId);

    List<ReviewDTO> findByBookings(Long userId, List<Long> bookingIds);

    ReviewDTO update(Long id, ReviewDTO review, Long userId);

    void delete(Long id, Long userId);

}
