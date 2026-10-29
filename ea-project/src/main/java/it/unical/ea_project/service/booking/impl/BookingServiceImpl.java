package it.unical.ea_project.service.booking.impl;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.booking.Booking;
import it.unical.ea_project.domain.payment.Payment;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.dto.booking.BookingDTO;
import it.unical.ea_project.repository.activity.ActivityRepository;
import it.unical.ea_project.repository.booking.BookingRepository;
import it.unical.ea_project.repository.payment.PaymentRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.booking.BookingService;
import it.unical.ea_project.service.booking.BookingLockService;
import java.math.BigDecimal;
import it.unical.ea_project.util.CalendarWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingLockService bookingLocks;

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final PaymentRepository paymentRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
            UserRepository userRepository,
            TripRepository tripRepository,
            ActivityRepository activityRepository,
            PaymentRepository paymentRepository, BookingLockService bookingLocks) {

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.activityRepository = activityRepository;
        this.paymentRepository = paymentRepository;
        this.bookingLocks = bookingLocks;

    }

    @Transactional
    public BookingDTO creaPrenotazioneTrip(Long userId, Long tripId, Integer seats) {

        validateSeats(seats);
        User traveler = requireTraveler(userId);
        Trip trip = tripRepository.findActiveForUpdate(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato"));
        if (trip.getStatus() != Trip.TripStatus.PUBLISHED || trip.getStartDate() == null
                || !trip.getStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Il viaggio non è prenotabile: deve essere pubblicato e futuro.");
        }
        rejectDuplicate(userId, tripId, null);
        reserveSeats(trip.getAvailableSeats(), seats);
        trip.setAvailableSeats(trip.getAvailableSeats() - seats);

        Booking b = new Booking();
        b.setUser(traveler);
        b.setTrip(trip);
        b.setSeats(seats);
        b.setBookingType(Booking.BookingType.TRIP);
        b.setBookingStatus(trip.getTotalPrice().signum() == 0 ? Booking.BookingStatus.CONFIRMED : Booking.BookingStatus.PENDING);
        b.setPrice(trip.getTotalPrice().multiply(BigDecimal.valueOf(seats)));
        return toDto(bookingRepository.save(b));

    }

    @Transactional
    public BookingDTO creaPrenotazioneActivity(Long userId, Long activityId, Integer seats) {

        validateSeats(seats);
        User traveler = requireTraveler(userId);
        Activity activity = activityRepository.findActiveForUpdate(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Attività non trovata"));
        if (activity.getStatus() != Activity.Status.PUBLISHED || activity.getStartDate() == null
                || !activity.getStartDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("L'attività non è prenotabile: deve essere pubblicata e futura.");
        }
        rejectDuplicate(userId, null, activityId);
        reserveSeats(activity.getAvailableSeats(), seats);
        activity.setAvailableSeats(activity.getAvailableSeats() - seats);

        Booking b = new Booking();
        b.setUser(traveler);
        b.setActivity(activity);
        b.setSeats(seats);
        b.setBookingType(Booking.BookingType.ACTIVITY);
        b.setBookingStatus(activity.getPrice() == 0 ? Booking.BookingStatus.CONFIRMED : Booking.BookingStatus.PENDING);
        b.setPrice(BigDecimal.valueOf(activity.getPrice()).multiply(BigDecimal.valueOf(seats)));
        return toDto(bookingRepository.save(b));

    }

    @Transactional
    public void cancella(Long bookingId, Long userId) {

        Booking b = bookingLocks.owned(bookingId, userId);
        if (b.getUser() == null || !Objects.equals(b.getUser().getId(), userId)) {
            throw new AccessDeniedException(
                    "Prenotazione non appartenente all'utente");
        }

        if (b.getDeletedAt() != null) {
            throw new IllegalArgumentException("Prenotazione non disponibile.");
        }

        if (b.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return;
        }

        if (b.getBookingStatus() == Booking.BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Non puoi cancellare una prenotazione già completata.");
        }

        if (isAssociatedExperienceCancelled(b)) {
            throw new IllegalArgumentException(
                    "Non puoi cancellare una prenotazione associata a un'esperienza annullata.");
        }
        if (isAssociatedExperienceCompleted(b)) {
            throw new IllegalArgumentException(
                    "Non puoi cancellare una prenotazione relativa a un'esperienza già completata.");
        }

        LocalDateTime eventStart = getEventStartDateTime(b);
        if (eventStart != null && !eventStart.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Non puoi cancellare una prenotazione per un'esperienza già iniziata.");
        }

        b.setBookingStatus(Booking.BookingStatus.CANCELLED);
        int seatsToRestore = b.getSeats() == null ? 0 : Math.max(0, b.getSeats());
        if (seatsToRestore > 0 && b.getTrip() != null) {
            Trip locked = b.getTrip();
            locked.setAvailableSeats(restoredSeats(locked.getAvailableSeats(), locked.getMaxSeats(), seatsToRestore));
        } else if (seatsToRestore > 0 && b.getActivity() != null) {
            Activity locked = b.getActivity();
            locked.setAvailableSeats(restoredSeats(locked.getAvailableSeats(), locked.getMaxSeats(), seatsToRestore));
        }

        bookingRepository.save(b);

    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getPrenotazioniUtente(Long userId) {

        List<Booking> bookings = bookingRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        Map<Long, String> paymentStatuses = loadLatestPaymentStatuses(bookings);
        return bookings.stream()
                .map(booking -> toDto(booking, paymentStatuses.get(booking.getBookingId())))
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getExperienceBookings(Long userId, Long tripId, Long activityId) {
        if ((tripId == null) == (activityId == null))
            throw new IllegalArgumentException("Indica un solo viaggio o una sola attività.");
        List<Booking> bookings = bookingRepository.findForExperience(userId, tripId, activityId);
        Map<Long, String> payments = loadLatestPaymentStatuses(bookings);
        return bookings.stream().map(b -> toDto(b, payments.get(b.getBookingId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByIds(Long userId, List<Long> ids) {
        if (ids.isEmpty())
            return List.of();
        List<Booking> bookings = bookingRepository.findByUserIdAndBookingIdInAndDeletedAtIsNull(userId, ids);
        Map<Long, String> payments = loadLatestPaymentStatuses(bookings);
        Map<Long, BookingDTO> mapped = bookings.stream().collect(Collectors.toMap(
                Booking::getBookingId, booking -> toDto(booking, payments.get(booking.getBookingId()))));
        return ids.stream().map(mapped::get).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getPrenotazioniOrganizzatore(Long organizerId) {

        List<Booking> bookings = bookingRepository.findActiveBookingsForOrganizer(organizerId);
        Map<Long, String> paymentStatuses = loadLatestPaymentStatuses(bookings);
        return bookings.stream()
                .map(booking -> toDto(booking, paymentStatuses.get(booking.getBookingId())))
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public String exportBookingToIcs(Long bookingId, Long userId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Prenotazione non trovata"));

        if (booking.getDeletedAt() != null) {
            throw new IllegalArgumentException("Prenotazione non disponibile");
        }

        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Puoi esportare soltanto le tue prenotazioni");
        }

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Non è possibile esportare una prenotazione cancellata");
        }
        if (isAssociatedExperienceCancelled(booking)) {
            throw new IllegalArgumentException("Non è possibile esportare un'esperienza annullata.");
        }

        String title;
        String location;
        String description;
        String start;
        String end;
        boolean allDay;
        String stableId;

        if (booking.getTrip() != null) {
            Trip trip = booking.getTrip();
            if (trip.getDeletedAt() != null || trip.getStartDate() == null) {
                throw new IllegalArgumentException("Il viaggio non ha date valide per il calendario");
            }
            LocalDate tripStart = trip.getStartDate();
            LocalDate tripEnd = trip.getEndDate() == null ? tripStart.plusDays(1) : trip.getEndDate().plusDays(1);
            title = safeTitle(trip.getTitle());
            location = joinLocation(trip.getDestinationCity(), trip.getDestinationCountry());
            description = "Prenotazione viaggio. Posti prenotati: "
                    + (booking.getSeats() == null ? 1 : booking.getSeats());
            start = tripStart.format(DateTimeFormatter.BASIC_ISO_DATE);
            end = tripEnd.format(DateTimeFormatter.BASIC_ISO_DATE);
            allDay = true;
            stableId = "trip-" + trip.getTripId();
        } else if (booking.getActivity() != null) {
            Activity activity = booking.getActivity();
            if (activity.getDeletedAt() != null || activity.getStartDate() == null) {
                throw new IllegalArgumentException("L'attività non ha date valide per il calendario");
            }
            LocalDateTime activityStart = activity.getStartDate();
            LocalDateTime activityEnd = activity.getEndDate() != null ? activity.getEndDate()
                    : activityStart
                            .plusMinutes(activity.getDurationMinutes() == null ? 60 : activity.getDurationMinutes());
            title = safeTitle(activity.getTitle());
            location = joinLocation(activity.getPlaceName(), activity.getCity());
            description = "Prenotazione attività. Posti prenotati: "
                    + (booking.getSeats() == null ? 1 : booking.getSeats());
            start = activityStart.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            end = activityEnd.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
            allDay = false;
            stableId = "activity-" + activity.getActivityId();
        } else {
            throw new IllegalArgumentException("La prenotazione non è associata a un viaggio o a un'attività");
        }

        String dtStart = allDay ? "DTSTART;VALUE=DATE:" + start : "DTSTART;TZID=Europe/Rome:" + start;
        String dtEnd = allDay ? "DTEND;VALUE=DATE:" + end : "DTEND;TZID=Europe/Rome:" + end;
        String uid = "booking-" + booking.getBookingId() + "-" + stableId + "@ea-project";
        return CalendarWriter.event(uid, title, location, description, dtStart, dtEnd, "CONFIRMED");
    }

    private String safeTitle(String title) {
        return title == null || title.isBlank() ? "Prenotazione EA Project" : title;
    }

    private String joinLocation(String first, String second) {

        String a = first == null ? "" : first.trim();
        String b = second == null ? "" : second.trim();
        if (a.isBlank()) {
            return b;
        }

        if (b.isBlank() || a.equalsIgnoreCase(b)) {
            return a;
        }

        return a + ", " + b;

    }

    private LocalDateTime getEventStartDateTime(Booking booking) {
        if (booking.getTrip() != null && booking.getTrip().getStartDate() != null) {
            return booking.getTrip().getStartDate().atStartOfDay();
        }
        if (booking.getActivity() != null) {
            return booking.getActivity().getStartDate();
        }
        return null;
    }

    private int restoredSeats(Integer available, Integer capacity, int seats) {
        long restored = Math.max(0, available == null ? 0 : available) + (long) seats;
        return (int) Math.min(capacity == null ? Integer.MAX_VALUE : Math.max(0, capacity), restored);
    }

    private User requireTraveler(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non disponibile."));
        if (user.getDeletedAt() != null || user.getRole() != User.Role.TRAVELER)
            throw new AccessDeniedException("Le prenotazioni sono riservate agli account Traveler.");
        return user;
    }

    private void rejectDuplicate(Long userId, Long tripId, Long activityId) {
        if (bookingRepository.existsReservation(userId, tripId, activityId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hai già una prenotazione per questa esperienza e questa data. Gestiscila dalle tue prenotazioni.");
    }

    private void validateSeats(Integer seats) {

        if (seats == null || seats < 1) {
            throw new IllegalArgumentException("Il numero di posti deve essere positivo");
        }

    }

    private boolean isAssociatedExperienceCancelled(Booking booking) {
        return booking != null && (booking.getTrip() != null
                && booking.getTrip().getStatus() == Trip.TripStatus.CANCELLED
                || booking.getActivity() != null && booking.getActivity().getStatus() == Activity.Status.CANCELLED);
    }

    private boolean isAssociatedExperienceCompleted(Booking booking) {
        return booking != null && (booking.getTrip() != null
                && booking.getTrip().getStatus() == Trip.TripStatus.COMPLETED
                || booking.getActivity() != null && booking.getActivity().getStatus() == Activity.Status.COMPLETED);
    }

    private void reserveSeats(Integer availableSeats, int requestedSeats) {

        if (availableSeats == null || availableSeats < requestedSeats) {
            throw new IllegalArgumentException("Posti disponibili insufficienti");
        }

    }

    private Map<Long, String> loadLatestPaymentStatuses(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty())
            return Map.of();
        List<Long> ids = new ArrayList<>(bookings.size());
        for (Booking booking : bookings) {
            if (booking != null)
                ids.add(booking.getBookingId());
        }
        if (ids.isEmpty())
            return Map.of();
        Map<Long, String> statuses = new HashMap<>();
        for (Payment payment : paymentRepository
                .findBookingPaymentsBatch(ids)) {
            if (payment == null || payment.getBooking() == null)
                continue;
            Long bookingId = payment.getBooking().getBookingId();
            if (!statuses.containsKey(bookingId)) {
                statuses.put(bookingId, payment.getStatus() == null ? null : payment.getStatus().name());
            }
        }
        return statuses;
    }

    private BookingDTO toDto(Booking booking) {
        String paymentStatus = paymentRepository
                .findBookingPayments(booking.getBookingId())
                .stream()
                .findFirst()
                .map(payment -> payment.getStatus() != null ? payment.getStatus().name() : null)
                .orElse(null);
        return toDto(booking, paymentStatus);
    }

    private BookingDTO toDto(Booking booking, String paymentStatus) {
        String title = null;
        String date = null;
        String endDate = null;
        String itemStatus = null;
        String location = null;
        if (booking.getTrip() != null) {
            title = booking.getTrip().getTitle();
            itemStatus = booking.getTrip().getStatus() != null ? booking.getTrip().getStatus().name() : null;
            date = booking.getTrip().getStartDate() != null ? booking.getTrip().getStartDate().toString() : null;
            endDate = booking.getTrip().getEndDate() != null
                    ? booking.getTrip().getEndDate().toString()
                    : date;
            location = joinLocation(booking.getTrip().getDestinationCity(), booking.getTrip().getDestinationCountry());
        } else if (booking.getActivity() != null) {
            Activity activity = booking.getActivity();
            title = activity.getTitle();
            itemStatus = activity.getStatus() != null ? activity.getStatus().name() : null;
            date = activity.getStartDate() != null ? activity.getStartDate().toString() : null;
            LocalDateTime activityEnd = activity.getEndDate();
            if (activityEnd == null && activity.getStartDate() != null) {
                int duration = activity.getDurationMinutes() == null || activity.getDurationMinutes() < 1
                        ? 60
                        : activity.getDurationMinutes();
                activityEnd = activity.getStartDate().plusMinutes(duration);
            }
            endDate = activityEnd != null ? activityEnd.toString() : date;
            location = joinLocation(activity.getPlaceName(), activity.getCity());
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
                .itemEndDate(endDate)
                .itemStatus(itemStatus)
                .itemLocation(location)
                .seats(booking.getSeats())
                .bookingType(booking.getBookingType() != null ? booking.getBookingType().name() : null)
                .bookingStatus(booking.getBookingStatus() != null ? booking.getBookingStatus().name() : null)
                .price(booking.getPrice())
                .paymentStatus(paymentStatus)
                .createdAt(booking.getCreatedAt())
                .deletedAt(booking.getDeletedAt())
                .build();

    }

}
