package it.unical.ea_project.integration.organizer;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.dto.booking.BookingDTO;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.payment.PaymentRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.activity.impl.ActivityServiceImpl;
import it.unical.ea_project.service.booking.impl.BookingServiceImpl;
import it.unical.ea_project.service.payment.impl.PaymentServiceImpl;
import it.unical.ea_project.service.trip.impl.TripServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@Import({TripServiceImpl.class, ActivityServiceImpl.class, BookingServiceImpl.class, PaymentServiceImpl.class})
class OrganizerWorkflowTest {
    @Autowired TripServiceImpl trips;
    @Autowired ActivityServiceImpl activities;
    @Autowired BookingServiceImpl bookings;
    @Autowired PaymentServiceImpl payments;
    @Autowired UserRepository users;
    @Autowired BookingRepository bookingRepository;
    @Autowired PaymentRepository paymentRepository;

    private User user(String name, User.Role role) {
        User user = new User(); user.setUsername(name); user.setEmail(name + "@example.test");
        user.setFullName(name); user.setRole(role); return users.saveAndFlush(user);
    }
    private Trip trip(User owner) {
        Trip trip = new Trip(); trip.setTitle("Tour"); trip.setDestinationCity("Roma");
        trip.setDestinationCountry("Italia"); trip.setTotalPrice(new BigDecimal("25.00"));
        trip.setStartDate(LocalDate.of(2027, 6, 1)); trip.setEndDate(LocalDate.of(2027, 6, 3));
        trip.setMaxSeats(10); trip.setAvailableSeats(10); trip.setStatus(Trip.TripStatus.PUBLISHED);
        return trips.createTrip(trip, owner.getId());
    }
    private Trip tripUpdate(Trip trip, int seats) {
        Trip details = new Trip(); details.setTitle(trip.getTitle()); details.setDestinationCity(trip.getDestinationCity());
        details.setDestinationCountry(trip.getDestinationCountry()); details.setTotalPrice(trip.getTotalPrice());
        details.setStartDate(trip.getStartDate()); details.setEndDate(trip.getEndDate());
        details.setStatus(trip.getStatus()); details.setMaxSeats(seats);
        details.setAvailableSeats(999); return details;
    }
    private Activity activity(User owner) {
        Activity activity = new Activity(); activity.setTitle("Visita"); activity.setCity("Roma");
        activity.setCategory(Activity.Category.VISIT); activity.setPrice(10.0); activity.setMaxSeats(10);
        activity.setDurationMinutes(120); activity.setAvailableSeats(10); activity.setStatus(Activity.Status.PUBLISHED);
        activity.setStartDate(LocalDateTime.of(2027, 6, 1, 10, 0)); activity.setEndDate(LocalDateTime.of(2027, 6, 1, 12, 0));
        return activities.createActivity(activity, owner.getId());
    }
    private Stage stage(String title) {
        Stage stage = new Stage(); stage.setTitle(title); stage.setDay(1); stage.setOrderInDay(1);
        stage.setCategory(Stage.StageCategory.VISIT); return stage;
    }

    @Test void capacityChangesUseCurrentReservationsAndCancellationRestoresSeats() {
        User owner = user("owner", User.Role.ORGANIZER); User traveler = user("traveler", User.Role.TRAVELER);
        Trip trip = trip(owner);
        Trip stale = tripUpdate(trip, 12);
        BookingDTO booking = bookings.creaPrenotazioneTrip(traveler.getId(), trip.getTripId(), 4);
        assertEquals(6, trip.getAvailableSeats());
        trips.updateTrip(trip.getTripId(), stale);
        assertEquals(8, trip.getAvailableSeats());
        bookings.cancella(booking.getBookingId(), traveler.getId());
        bookings.cancella(booking.getBookingId(), traveler.getId());
        assertEquals(12, trip.getAvailableSeats());
    }

    @Test void activityCapacityCannotDropBelowReservations() {
        User owner = user("owner", User.Role.ORGANIZER); User traveler = user("traveler", User.Role.TRAVELER);
        Activity activity = activity(owner);
        bookings.creaPrenotazioneActivity(traveler.getId(), activity.getActivityId(), 4);
        Activity details = new Activity(); details.setTitle("Visita"); details.setCity("Roma");
        details.setCategory(Activity.Category.VISIT); details.setPrice(10.0); details.setDurationMinutes(120);
        details.setStartDate(activity.getStartDate()); details.setEndDate(activity.getEndDate()); details.setMaxSeats(3);
        assertThrows(ResponseStatusException.class, () -> activities.updateActivity(activity.getActivityId(), details));
        assertEquals(10, activity.getMaxSeats());
        assertEquals(6, activity.getAvailableSeats());
    }

    @Test void organizerSeesOnlyOwnBookingsAndPaidRevenueIncludingDeletedOffers() {
        User owner = user("owner", User.Role.ORGANIZER); User other = user("other", User.Role.ORGANIZER);
        User traveler = user("traveler", User.Role.TRAVELER);
        Trip trip = trip(owner);
        BookingDTO booking = bookings.creaPrenotazioneTrip(traveler.getId(), trip.getTripId(), 2);
        Payment paid = new Payment(); paid.setBooking(bookingRepository.findById(booking.getBookingId()).orElseThrow());
        paid.setAmount(new BigDecimal("50.00")); paid.setStatus(Payment.PaymentStatus.COMPLETED); paymentRepository.saveAndFlush(paid);
        BookingDTO pending = bookings.creaPrenotazioneActivity(traveler.getId(), activity(owner).getActivityId(), 1);
        Payment unpaid = new Payment(); unpaid.setBooking(bookingRepository.findById(pending.getBookingId()).orElseThrow());
        unpaid.setAmount(new BigDecimal("10.00")); unpaid.setStatus(Payment.PaymentStatus.PENDING); paymentRepository.saveAndFlush(unpaid);
        assertEquals(0, payments.getPaidAmountForOrganizer(owner.getId()).compareTo(new BigDecimal("50.00")));
        assertTrue(bookings.getPrenotazioniOrganizzatore(other.getId()).isEmpty());
        trips.deleteTrip(trip.getTripId());
        assertEquals(2, bookings.getPrenotazioniOrganizzatore(owner.getId()).size());
        assertEquals(0, payments.getPaidAmountForOrganizer(owner.getId()).compareTo(new BigDecimal("50.00")));
        // Deleting an offer marks it CANCELLED: the existing booking cancellation policy still applies.
        assertThrows(IllegalArgumentException.class, () -> bookings.cancella(booking.getBookingId(), traveler.getId()));
        assertEquals(8, trip.getAvailableSeats());
    }

    @Test void itineraryRoundTripsAndOmittedStagesArePreserved() {
        User owner = user("owner", User.Role.ORGANIZER);
        Trip trip = trip(owner); trip.addStage(stage("Museo"));
        trips.saveTrip(trip);
        var dto = trips.getTripDtoById(trip.getTripId()).orElseThrow();
        assertEquals(1, dto.getStages().size());
        assertEquals("Museo", dto.getStages().getFirst().getTitle());
        trips.updateTrip(trip.getTripId(), tripUpdate(trip, 10));
        assertEquals(1, trip.getStages().size());
        Trip withExistingStage = tripUpdate(trip, 10);
        Stage changed = stage("Museo aggiornato"); changed.setStageId(dto.getStages().getFirst().getStageId());
        withExistingStage.setStages(new ArrayList<>(List.of(changed)));
        trips.updateTrip(trip.getTripId(), withExistingStage);
        assertEquals(dto.getStages().getFirst().getStageId(), trip.getStages().getFirst().getStageId());
        assertEquals("Museo aggiornato", trip.getStages().getFirst().getTitle());
        Trip edited = tripUpdate(trip, 10);
        edited.setStages(new ArrayList<>(List.of(stage("Centro storico"))));
        trips.updateTrip(trip.getTripId(), edited);
        assertEquals("Centro storico", trip.getStages().getFirst().getTitle());
        Trip cleared = tripUpdate(trip, 10); cleared.setStages(new ArrayList<>());
        trips.updateTrip(trip.getTripId(), cleared);
        assertTrue(trip.getStages().isEmpty());
    }

    @Test void nonPublishedOffersCannotReceiveBookings() {
        User owner = user("owner", User.Role.ORGANIZER); User traveler = user("traveler", User.Role.TRAVELER);
        Trip trip = trip(owner); trip.setStatus(Trip.TripStatus.DRAFT);
        assertThrows(IllegalArgumentException.class, () -> bookings.creaPrenotazioneTrip(traveler.getId(), trip.getTripId(), 1));
        Activity activity = activity(owner); activity.setStatus(Activity.Status.CANCELLED);
        assertThrows(IllegalArgumentException.class, () -> bookings.creaPrenotazioneActivity(traveler.getId(), activity.getActivityId(), 1));
    }
}
