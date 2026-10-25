package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Booking;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final ResponseRepository responseRepository;
    private final BookingRepository bookingRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             UserRepository userRepository,
                             TripRepository tripRepository,
                             ActivityRepository activityRepository,
                             ResponseRepository responseRepository,
                             BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
        this.responseRepository = responseRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public ReviewDTO create(ReviewDTO review) {
        if (review == null) throw new IllegalArgumentException("Dati della recensione mancanti.");
        if (review.getUserId() == null) throw new IllegalArgumentException("Utente non autenticato.");
        validateReviewContent(review.getRating(), review.getComment());

        boolean hasTrip = review.getTripId() != null;
        boolean hasActivity = review.getActivityId() != null;
        if (hasTrip == hasActivity) {
            throw new IllegalArgumentException("La recensione deve riferirsi a un solo viaggio o a una sola attività.");
        }

        if (!hasEligibleCompletedBooking(review.getUserId(), review.getTripId(), review.getActivityId())) {
            throw new IllegalArgumentException("Puoi recensire soltanto un'esperienza prenotata e terminata, non cancellata.");
        }

        boolean alreadyReviewed = reviewRepository.findAllByUserId(review.getUserId()).stream()
                .anyMatch(existing -> hasTrip
                        ? existing.getTrip() != null && review.getTripId().equals(existing.getTrip().getTripId())
                        : existing.getActivity() != null && review.getActivityId().equals(existing.getActivity().getActivityId()));
        if (alreadyReviewed) {
            throw new IllegalArgumentException("Hai già recensito questa esperienza. Puoi modificare la recensione esistente.");
        }

        Review reviewEntity = new Review();
        reviewEntity.setUser(userRepository.getReferenceById(review.getUserId()));
        if (hasTrip) reviewEntity.setTrip(tripRepository.findById(review.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato.")));
        if (hasActivity) reviewEntity.setActivity(activityRepository.findById(review.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Attività non trovata.")));
        reviewEntity.setRating(review.getRating());
        reviewEntity.setComment(normalizeComment(review.getComment()));

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

    @Transactional(readOnly = true)
    public List<ReviewDTO> findByUserAsDto(Long userId) {
        List<Review> reviews = reviewRepository.findAllByUserId(userId);
        if (reviews.isEmpty()) return List.of();
        List<Long> ids = reviews.stream().map(Review::getReviewId).toList();
        List<Response> responses = responseRepository.findByReview_ReviewIdInAndDeletedFalse(ids);
        Map<Long, Response> responseByReviewId = responses.stream()
                .collect(Collectors.toMap(response -> response.getReview().getReviewId(), response -> response,
                        (first, ignored) -> first));
        return reviews.stream()
                .map(review -> ReviewMapper.toReviewDTO(review, responseByReviewId.get(review.getReviewId())))
                .toList();
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
        if (data == null) throw new IllegalArgumentException("Dati della recensione mancanti.");
        if (data.getRating() != null) validateRating(data.getRating());
        if (data.getComment() != null) validateComment(data.getComment());

        if (data.getRating() != null) review.setRating(data.getRating());
        // PUT represents the current review form: a null/blank comment means the author cleared it.
        review.setComment(normalizeComment(data.getComment()));

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

    private void validateReviewContent(Integer rating, String comment) {
        validateRating(rating);
        validateComment(comment);
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("La valutazione deve essere compresa tra 1 e 5 stelle.");
        }
    }

    private void validateComment(String comment) {
        if (comment != null && comment.length() > 200) {
            throw new IllegalArgumentException("La recensione può contenere al massimo 200 caratteri.");
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null) return null;
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean hasEligibleCompletedBooking(Long userId, Long tripId, Long activityId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .filter(booking -> booking.getBookingStatus() != Booking.BookingStatus.CANCELLED)
                .filter(booking -> booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED
                        || booking.getBookingStatus() == Booking.BookingStatus.COMPLETED)
                .filter(booking -> !isEventCancelled(booking))
                .filter(booking -> tripId != null
                        ? booking.getTrip() != null && tripId.equals(booking.getTrip().getTripId())
                        : booking.getActivity() != null && activityId.equals(booking.getActivity().getActivityId()))
                .anyMatch(booking -> booking.getBookingStatus() == Booking.BookingStatus.COMPLETED
                        || isEventCompleted(booking)
                        || eventEndDateTime(booking) != null && !eventEndDateTime(booking).isAfter(now));
    }

    private boolean isEventCompleted(Booking booking) {
        if (booking.getTrip() != null) {
            return booking.getTrip().getStatus() == Trip.TripStatus.COMPLETED;
        }
        return booking.getActivity() != null
                && booking.getActivity().getStatus() == Activity.Status.COMPLETED;
    }

    private boolean isEventCancelled(Booking booking) {
        if (booking.getTrip() != null) {
            return booking.getTrip().getStatus() == Trip.TripStatus.CANCELLED;
        }
        return booking.getActivity() != null
                && booking.getActivity().getStatus() == Activity.Status.CANCELLED;
    }

    private LocalDateTime eventEndDateTime(Booking booking) {
        if (booking.getTrip() != null) {
            LocalDate end = booking.getTrip().getEndDate() != null
                    ? booking.getTrip().getEndDate() : booking.getTrip().getStartDate();
            return end == null ? null : end.atTime(LocalTime.MAX);
        }
        if (booking.getActivity() != null) {
            Activity activity = booking.getActivity();
            if (activity.getEndDate() != null) return activity.getEndDate();
            if (activity.getStartDate() == null) return null;
            int minutes = activity.getDurationMinutes() == null || activity.getDurationMinutes() < 1
                    ? 60 : activity.getDurationMinutes();
            return activity.getStartDate().plusMinutes(minutes);
        }
        return null;
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
