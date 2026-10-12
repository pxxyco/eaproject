package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Booking;
import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.BookingDTO;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.BookingRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.BookingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              TripRepository tripRepository,
                              ActivityRepository activityRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public BookingDTO creaPrenotazioneTrip(Long userId, Long tripId, Integer seats) {
        validateSeats(seats);
        Trip trip = tripRepository.findByTripIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato"));
        reserveSeats(trip.getAvailableSeats(), seats);
        trip.setAvailableSeats(trip.getAvailableSeats() - seats);

        Booking b = new Booking();
        b.setUser(userRepository.getReferenceById(userId));
        b.setTrip(trip);
        b.setSeats(seats);
        b.setBookingType(Booking.BookingType.TRIP);
        b.setBookingStatus(Booking.BookingStatus.PENDING);
        b.setPrice(trip.getTotalPrice().multiply(java.math.BigDecimal.valueOf(seats)));
        return toDto(bookingRepository.save(b));
    }

    @Transactional
    public BookingDTO creaPrenotazioneActivity(Long userId, Long activityId, Integer seats) {
        validateSeats(seats);
        Activity activity = activityRepository.findByActivityIdAndDeletedAtIsNull(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Attività non trovata"));
        reserveSeats(activity.getAvailableSeats(), seats);
        activity.setAvailableSeats(activity.getAvailableSeats() - seats);

        Booking b = new Booking();
        b.setUser(userRepository.getReferenceById(userId));
        b.setActivity(activity);
        b.setSeats(seats);
        b.setBookingType(Booking.BookingType.ACTIVITY);
        b.setBookingStatus(Booking.BookingStatus.PENDING);
        b.setPrice(java.math.BigDecimal.valueOf(activity.getPrice()).multiply(java.math.BigDecimal.valueOf(seats)));
        return toDto(bookingRepository.save(b));
    }

    @Transactional
    public void cancella(Long bookingId, Long userId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Non trovato"));
        if (b.getUser() == null || !b.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Prenotazione non appartenente all'utente");
        }
        if (b.getBookingStatus() == Booking.BookingStatus.CANCELLED) return;
        b.setBookingStatus(Booking.BookingStatus.CANCELLED);
        if (b.getTrip() != null) b.getTrip().setAvailableSeats(b.getTrip().getAvailableSeats() + b.getSeats());
        if (b.getActivity() != null) b.getActivity().setAvailableSeats(b.getActivity().getAvailableSeats() + b.getSeats());
        bookingRepository.save(b);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getPrenotazioniUtente(Long userId) {
        return bookingRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getPrenotazioniOrganizzatore(Long organizerId) {
        return bookingRepository.findActiveBookingsForOrganizer(organizerId)
                .stream().map(this::toDto).toList();
    }

    private void validateSeats(Integer seats) {
        if (seats == null || seats < 1) {
            throw new IllegalArgumentException("Il numero di posti deve essere positivo");
        }
    }

    private void reserveSeats(Integer availableSeats, int requestedSeats) {
        if (availableSeats == null || availableSeats < requestedSeats) {
            throw new IllegalArgumentException("Posti disponibili insufficienti");
        }
    }

    private BookingDTO toDto(Booking booking) {
        String title = null;
        String date = null;
        String location = null;
        if (booking.getTrip() != null) {
            title = booking.getTrip().getTitle();
            date = booking.getTrip().getStartDate() != null ? booking.getTrip().getStartDate().toString() : null;
            location = booking.getTrip().getDestinationCity();
        } else if (booking.getActivity() != null) {
            title = booking.getActivity().getTitle();
            date = booking.getActivity().getStartDate() != null ? booking.getActivity().getStartDate().toString() : null;
            location = booking.getActivity().getCity();
        }

        return BookingDTO.builder()
                .bookingId(booking.getBookingId())
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .tripId(booking.getTrip() != null ? booking.getTrip().getTripId() : null)
                .activityId(booking.getActivity() != null ? booking.getActivity().getActivityId() : null)
                .participantName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .participantEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .itemTitle(title)
                .itemDate(date)
                .itemLocation(location)
                .seats(booking.getSeats())
                .bookingType(booking.getBookingType() != null ? booking.getBookingType().name() : null)
                .bookingStatus(booking.getBookingStatus() != null ? booking.getBookingStatus().name() : null)
                .price(booking.getPrice())
                .createdAt(booking.getCreatedAt())
                .deletedAt(booking.getDeletedAt())
                .build();
    }

}
