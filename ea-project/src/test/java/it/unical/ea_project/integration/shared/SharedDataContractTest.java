package it.unical.ea_project.integration.shared;

import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.favorites.FavoriteList;
import it.unical.ea_project.domain.favorites.ListSharing;
import it.unical.ea_project.domain.review.Response;
import it.unical.ea_project.domain.review.Review;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.favorites.FavoriteListRepository;
import it.unical.ea_project.repository.favorites.ListSharingRepository;
import it.unical.ea_project.repository.review.ResponseRepository;
import it.unical.ea_project.repository.review.ReviewRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.favorites.FavoriteListAccess;
import it.unical.ea_project.service.review.impl.ReviewServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@Import({ ReviewServiceImpl.class, FavoriteListAccess.class })
class SharedDataContractTest {
    @Autowired
    ReviewServiceImpl service;
    @Autowired
    FavoriteListAccess access;
    @Autowired
    UserRepository users;
    @Autowired
    TripRepository trips;
    @Autowired
    BookingRepository bookings;
    @Autowired
    ReviewRepository reviews;
    @Autowired
    ResponseRepository responses;
    @Autowired
    FavoriteListRepository lists;
    @Autowired
    ListSharingRepository shares;

    private User user(String name) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.test");
        user.setFullName(name);
        user.setRole(User.Role.TRAVELER);
        return users.saveAndFlush(user);
    }

    private Booking booking(User user, Trip trip) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setBookingType(Booking.BookingType.TRIP);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPrice(BigDecimal.TEN);
        booking.setSeats(1);
        return bookings.saveAndFlush(booking);
    }

    @Test
    void pagedReviewsKeepCompleteDataAndCannotReadAnotherUsersBookings() {
        User traveler = user("traveler"), other = user("other"), organizer = user("organizer");
        Trip trip = new Trip();
        trip.setTitle("Roma");
        trip.setCreatedBy(organizer);
        trip.setStatus(Trip.TripStatus.COMPLETED);
        trip = trips.saveAndFlush(trip);
        Booking own = booking(traveler, trip), foreign = booking(other, trip);
        Review review = new Review();
        review.setUser(traveler);
        review.setTrip(trip);
        review.setRating(5);
        review.setComment("Prima versione");
        review = reviews.saveAndFlush(review);
        review.setComment("Commento aggiornato");
        reviews.saveAndFlush(review);
        Response response = new Response();
        response.setOrganizer(organizer);
        response.setReview(review);
        response.setComment("Grazie!");
        response.setCreatedAt(LocalDateTime.now());
        responses.saveAndFlush(response);

        var result = service.findByBookings(traveler.getId(), List.of(own.getBookingId()));
        assertEquals(1, result.size());
        var dto = result.getFirst();
        assertEquals(traveler.getUsername(), dto.getUsername());
        assertEquals(review.getUpdatedAt(), dto.getUpdatedAt());
        assertEquals("Commento aggiornato", dto.getComment());
        assertEquals("Grazie!", dto.getResponse().getComment());
        assertEquals(organizer.getId(), dto.getResponse().getOrganizerId());
        assertTrue(service.findByBookings(traveler.getId(), List.of(foreign.getBookingId())).isEmpty());
        assertTrue(service.findByBookings(traveler.getId(), List.of()).isEmpty());
        review.setDeleted(true);
        reviews.saveAndFlush(review);
        assertTrue(service.findByBookings(traveler.getId(), List.of(own.getBookingId())).isEmpty());
    }

    @Test
    void favoriteListsShareOnePermissionPolicyForOwnerViewerEditorAndDeletedLists() {
        User owner = user("owner"), collaborator = user("collaborator"), stranger = user("stranger");
        FavoriteList list = new FavoriteList();
        list.setName("Vacanza");
        list.setUser(owner);
        list.setVisibility(FavoriteList.Visibility.SHARED);
        list = lists.saveAndFlush(list);
        ListSharing share = new ListSharing();
        share.setFavoriteList(list);
        share.setUser(collaborator);
        share.setPermission(ListSharing.SharingPermission.VIEW);
        shares.saveAndFlush(share);
        assertTrue(access.canEdit(list.getId(), owner.getId()));
        assertTrue(access.canView(list.getId(), collaborator.getId()));
        assertFalse(access.canEdit(list.getId(), collaborator.getId()));
        assertFalse(access.canView(list.getId(), stranger.getId()));
        share.setPermission(ListSharing.SharingPermission.EDIT);
        shares.saveAndFlush(share);
        assertTrue(access.canEdit(list.getId(), collaborator.getId()));
        list.setVisibility(FavoriteList.Visibility.PRIVATE);
        lists.saveAndFlush(list);
        assertFalse(access.canView(list.getId(), collaborator.getId()));
        list.setDeleted(true);
        lists.saveAndFlush(list);
        assertFalse(access.canView(list.getId(), owner.getId()));
        assertFalse(access.canEdit(list.getId(), owner.getId()));
        assertFalse(access.canView(list, null));
    }
}
