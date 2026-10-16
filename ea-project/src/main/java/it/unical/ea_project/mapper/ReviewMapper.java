package it.unical.ea_project.mapper;

import it.unical.ea_project.domain.Response;
import it.unical.ea_project.domain.Review;
import it.unical.ea_project.dto.ReviewDTO;

public class ReviewMapper {

    public static ReviewDTO toReviewDTO(Review review, Response response) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .response(response != null ? ResponseMapper.toResponseDTO(response) : null)
                .tripId(review.getTrip() != null ? review.getTrip().getTripId() : null)
                .activityId(review.getActivity() != null ? review.getActivity().getActivityId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .deleted(review.getDeleted())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

}
