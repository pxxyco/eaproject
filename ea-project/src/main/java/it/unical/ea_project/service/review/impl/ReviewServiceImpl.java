package it.unical.ea_project.service.review.impl;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.review.Response;
import it.unical.ea_project.domain.review.Review;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.dto.review.ReviewDTO;
import it.unical.ea_project.mapper.review.ReviewMapper;
import it.unical.ea_project.repository.activity.ActivityRepository;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.review.ResponseRepository;
import it.unical.ea_project.repository.review.ReviewRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.review.ReviewService;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import it.unical.ea_project.service.booking.BookingLifecycle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final EntityManager entities;
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
            BookingRepository bookingRepository, EntityManager entities) {
        this.entities = entities;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
        this.responseRepository = responseRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public ReviewDTO create(ReviewDTO review) {
        if (review == null)
            throw new IllegalArgumentException("Dati della recensione mancanti.");
        if (review.getUserId() == null)
            throw new IllegalArgumentException("Utente non autenticato.");
        validateReviewContent(review.getRating(), review.getComment());

        boolean hasTrip = review.getTripId() != null;
        boolean hasActivity = review.getActivityId() != null;
        if (hasTrip == hasActivity) {
            throw new IllegalArgumentException("La recensione deve riferirsi a un solo viaggio o a una sola attività.");
        }

        lockOffer(review.getTripId(), review.getActivityId());
        if (!hasEligibleCompletedBooking(review.getUserId(), review.getTripId(), review.getActivityId())) {
            throw new IllegalArgumentException(
                    "Puoi recensire un'esperienza con prenotazione confermata, non annullata.");
        }

        boolean alreadyReviewed = reviewRepository.findAllByUserId(review.getUserId()).stream()
                .anyMatch(existing -> hasTrip
                        ? existing.getTrip() != null && review.getTripId().equals(existing.getTrip().getTripId())
                        : existing.getActivity() != null
                                && review.getActivityId().equals(existing.getActivity().getActivityId()));
        if (alreadyReviewed) {
            throw new IllegalArgumentException(
                    "Hai già recensito questa esperienza. Puoi modificare la recensione esistente.");
        }

        Review reviewEntity = new Review();
        reviewEntity.setUser(userRepository.getReferenceById(review.getUserId()));
        if (hasTrip)
            reviewEntity.setTrip(tripRepository.findById(review.getTripId())
                    .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato.")));
        if (hasActivity)
            reviewEntity.setActivity(activityRepository.findById(review.getActivityId())
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recensione non trovata."));
    }

    @Transactional(readOnly = true)
    public List<Review> findByUser(Long userId) {
        return reviewRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> findByUserAsDto(Long userId) {
        return toDtos(reviewRepository.findAllByUserId(userId));
    }

    @Nullable
    @Transactional(readOnly = true)
    public List<ReviewDTO> findByTrip(Long tripId) {
        return toDtos(reviewRepository.findAllByTripTripIdOrderByCreatedAtDesc(tripId));
    }

    @Nullable
    @Transactional(readOnly = true)
    public List<ReviewDTO> findByActivity(Long activityId) {
        return toDtos(reviewRepository.findAllByActivityActivityIdOrderByCreatedAtDesc(activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDTO> findByBookings(Long userId, List<Long> bookingIds) {
        return bookingIds.isEmpty() ? List.of() : toDtos(reviewRepository.findForBookings(userId, bookingIds));
    }

    private List<ReviewDTO> toDtos(List<Review> reviews) {
        if (reviews.isEmpty())
            return List.of();
        List<Long> ids = reviews.stream().map(Review::getReviewId).toList();
        Map<Long, Response> responses = responseRepository.findByReview_ReviewIdInAndDeletedFalse(ids).stream()
                .collect(Collectors.toMap(response -> response.getReview().getReviewId(), response -> response,
                        (first, ignored) -> first));
        return reviews.stream().map(review -> ReviewMapper.toReviewDTO(review, responses.get(review.getReviewId())))
                .toList();
    }

    @Transactional
    public ReviewDTO update(Long id, ReviewDTO data, Long userId) {
        Review review = findOwnedReview(id, userId);
        lockOffer(review.getTrip() == null ? null : review.getTrip().getTripId(),
                review.getActivity() == null ? null : review.getActivity().getActivityId());
        if (data == null)
            throw new IllegalArgumentException("Dati della recensione mancanti.");
        if (data.getRating() != null)
            validateRating(data.getRating());
        if (data.getComment() != null)
            validateComment(data.getComment());

        if (data.getRating() != null)
            review.setRating(data.getRating());
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
        lockOffer(review.getTrip() == null ? null : review.getTrip().getTripId(),
                review.getActivity() == null ? null : review.getActivity().getActivityId());

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
        if (comment == null)
            return null;
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean hasEligibleCompletedBooking(Long userId, Long tripId, Long activityId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findForExperience(userId, tripId, activityId).stream()
                .filter(booking -> tripId != null
                        ? booking.getTrip() != null && tripId.equals(booking.getTrip().getTripId())
                        : booking.getActivity() != null && activityId.equals(booking.getActivity().getActivityId()))
                .anyMatch(booking -> BookingLifecycle.reviewable(booking, now));
    }

    private void updateAverageRating(Long tripId, Long activityId) {
        if (activityId != null) {
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

    private void lockOffer(Long tripId, Long activityId) {
        Object offer = tripId != null ? entities.find(Trip.class, tripId, LockModeType.PESSIMISTIC_WRITE)
                : entities.find(Activity.class, activityId, LockModeType.PESSIMISTIC_WRITE);
        if (offer == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esperienza non trovata.");
    }

    private Review findOwnedReview(Long id, Long userId) {
        Review review = entities.find(Review.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (review == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recensione non trovata.");

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("La recensione non appartiene all'utente autenticato.");
        }
        return review;
    }
}
