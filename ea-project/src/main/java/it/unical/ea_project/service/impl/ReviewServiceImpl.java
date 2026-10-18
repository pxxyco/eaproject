package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Response;
import it.unical.ea_project.domain.Review;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.ReviewDTO;
import it.unical.ea_project.mapper.ReviewMapper;
import it.unical.ea_project.repository.*;
import it.unical.ea_project.service.ReviewService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final ResponseRepository responseRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             UserRepository userRepository,
                             TripRepository tripRepository,
                             ActivityRepository activityRepository,
                             ResponseRepository responseRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional
    public ReviewDTO create(ReviewDTO review) {
        // TODO: add data validation

        Review reviewEntity = new Review();
        reviewEntity.setUser(userRepository.getReferenceById(review.getUserId()));

        if(review.getTripId() != null){
            reviewEntity.setTrip(tripRepository.getReferenceById(review.getTripId()));
        }
        if(review.getActivityId() != null){
            reviewEntity.setActivity(activityRepository.getReferenceById(review.getActivityId()));
        }

        reviewEntity.setRating(review.getRating());
        reviewEntity.setComment(review.getComment());

        Review saved = reviewRepository.save(reviewEntity);

        updateAverageRating(review.getTripId(), review.getActivityId());

        return ReviewMapper.toReviewDTO(saved, null);
    }

    @Transactional(readOnly = true)
    public Review findById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review non trovata"));
    }

    @Transactional(readOnly = true)
    public List<Review> findByUser(Long userId) {
        return reviewRepository.findAllByUserId(userId);
    }


    @Nullable
    @Transactional(readOnly = true)
    public List<ReviewDTO> findByTrip(Long tripId) {

        List<Review> reviews = reviewRepository.findAllByTripTripIdOrderByCreatedAtDesc(tripId);

        List<Long> ids = reviews.stream().map(Review::getReviewId).toList();

        List<Response> responses = responseRepository.findByReview_ReviewIdInAndDeletedFalse(ids);

        Map<Long, Response> responseIdMap = responses.stream().
                collect(Collectors.toMap(r-> r.getReview().getReviewId(),
                        response -> response));

        return reviews.stream()
                .map(review ->  ReviewMapper.toReviewDTO(review,
                        responseIdMap.get(review.getReviewId())))
                .collect(Collectors.toList());
    }

    @Nullable
    @Transactional(readOnly = true)
    public List<ReviewDTO> findByActivity(Long activityId) {

        List<Review> reviews = reviewRepository.findAllByActivityActivityIdOrderByCreatedAtDesc(activityId);

        List<Long> ids = reviews.stream().map(Review::getReviewId).toList();

        List<Response> responses = responseRepository.findByReview_ReviewIdInAndDeletedFalse(ids);

        Map<Long, Response> responseIdMap = responses.stream()
                .collect(Collectors.toMap(r-> r.getReview().getReviewId(),
                        response -> response));

        return reviews.stream()
                .map(review -> ReviewMapper.toReviewDTO(review,
                        responseIdMap.get(review.getReviewId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDTO update(Long id, ReviewDTO data, Long userId) {
        Review review = findOwnedReview(id, userId);

        if (data.getRating() != null) review.setRating(data.getRating());
        if (data.getComment() != null) review.setComment(data.getComment());

        Long tripId = review.getTrip() != null ? review.getTrip().getTripId() : null;
        Long activityId = review.getActivity() != null ? review.getActivity().getActivityId() : null;

        updateAverageRating(tripId, activityId);

        Response response = responseRepository.findByReview_ReviewIdInAndDeletedFalse(List.of(id))
                .stream().findFirst().orElse(null);

        return ReviewMapper.toReviewDTO(review, response);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Review review = findOwnedReview(id, userId);

        Long tripId = review.getTrip() != null ? review.getTrip().getTripId() : null;
        Long activityId = review.getActivity() != null ? review.getActivity().getActivityId() : null;

        reviewRepository.softDeleteById(id);

        updateAverageRating(tripId, activityId);
    }

    private void updateAverageRating(Long tripId, Long activityId) {
        if(activityId != null){
            Activity activity = activityRepository.findById(activityId)
                    .orElseThrow(() -> new RuntimeException("Activity non trovata"));

            Double avg = reviewRepository.averageRatingByActivity(activityId);
            activity.setAverageRating(avg != null ? Math.round(avg * 100.0) / 100.0 : null);
        }
        if (tripId != null) {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new RuntimeException("Trip non trovato"));
            Double avg = reviewRepository.averageRatingByTrip(tripId);
            trip.setAverageRating(avg != null
                    ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                    : null);
        }
    }

    private Review findOwnedReview(Long id, Long userId) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review non trovata"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("La recensione non appartiene all'utente autenticato.");
        }
        return review;
    }
}
