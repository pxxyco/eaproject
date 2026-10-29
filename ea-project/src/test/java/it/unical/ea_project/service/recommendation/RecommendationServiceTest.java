package it.unical.ea_project.service.recommendation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import it.unical.ea_project.domain.account.AccountSettings;
import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.review.Review;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.account.AccountSettingsRepository;
import it.unical.ea_project.repository.activity.ActivityImageRepository;
import it.unical.ea_project.repository.activity.ActivityRepository;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.favorites.ListActivitiesRepository;
import it.unical.ea_project.repository.favorites.ListTripsRepository;
import it.unical.ea_project.repository.review.ReviewRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AccountSettingsRepository settings = mock(AccountSettingsRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final ListTripsRepository favorites = mock(ListTripsRepository.class);
    private final ListActivitiesRepository favoriteActivities = mock(ListActivitiesRepository.class);
    private final ActivityImageRepository images = mock(ActivityImageRepository.class);
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final RecommendationService service = new RecommendationService(users, settings, trips, activities,
            bookings, favorites, favoriteActivities, images, reviews);
    private final AccountSettings preferences = new AccountSettings(7L);

    @BeforeEach void setup() {
        var user = new User(); user.setId(7L); user.setRole(User.Role.TRAVELER);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(settings.findById(7L)).thenReturn(Optional.of(preferences));
        when(activities.recommendationCandidates(any(), eq(7L), any())).thenReturn(List.of(
                activity(1L, Activity.Category.VISIT, 1d), activity(2L, Activity.Category.MEAL, 5d)));
    }

    @Test void favoritesActuallyChangeRankingAndExplainTheInterest() {
        when(favoriteActivities.preferenceActivities(eq(7L), any())).thenReturn(List.of(activity(99L, Activity.Category.VISIT, 0d)));
        var picks = service.recommend(7L, 1);
        assertTrue(picks.personalized());
        assertEquals(1L, picks.activities().getFirst().item().getActivityId());
        assertTrue(picks.activities().getFirst().reason().contains("interessi"));
    }

    @Test void optingOutSkipsInterestSourcesAndUsesQualityRanking() {
        preferences.setPersonalized(false);
        var picks = service.recommend(7L, 1);
        assertFalse(picks.personalized());
        assertEquals(2L, picks.activities().getFirst().item().getActivityId());
        verifyNoInteractions(favorites, favoriteActivities, reviews);
    }

    @Test void negativeReviewPenalizesItsCategory() {
        var review = new Review(); review.setRating(1); review.setActivity(activity(99L, Activity.Category.MEAL, 0d));
        when(reviews.preferenceReviews(eq(7L), any())).thenReturn(List.of(review));
        var picks = service.recommend(7L, 1);
        assertTrue(picks.personalized());
        assertEquals(1L, picks.activities().getFirst().item().getActivityId());
    }

    private Activity activity(Long id, Activity.Category category, double rating) {
        var a = new Activity(); a.setActivityId(id); a.setTitle("Esperienza " + id); a.setCategory(category);
        a.setAverageRating(rating); return a;
    }
}
