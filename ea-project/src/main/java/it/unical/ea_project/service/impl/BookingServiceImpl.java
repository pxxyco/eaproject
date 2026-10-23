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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

        if (b.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return;
        }

        b.setBookingStatus(Booking.BookingStatus.CANCELLED);
        if (b.getTrip() != null) {
            b.getTrip()
            .setAvailableSeats(b.getTrip().getAvailableSeats() + b.getSeats());
        }

        if (b.getActivity() != null) {
            b.getActivity()
            .setAvailableSeats(b.getActivity().getAvailableSeats() + b.getSeats());
        }
        
        bookingRepository.save(b);

    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getPrenotazioniUtente(Long userId) {

        return bookingRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getPrenotazioniOrganizzatore(Long organizerId) {

        return bookingRepository.findActiveBookingsForOrganizer(organizerId)
                .stream()
                .map(this::toDto)
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
            throw new org.springframework.security.access.AccessDeniedException(
                    "Puoi esportare soltanto le tue prenotazioni");
        }
        
        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Non è possibile esportare una prenotazione cancellata");
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
            description = "Prenotazione viaggio. Posti prenotati: " + (booking.getSeats() == null ? 1 : booking.getSeats());
            start = tripStart.format(DateTimeFormatter.BASIC_ISO_DATE);
            end = tripEnd.format(DateTimeFormatter.BASIC_ISO_DATE);
            allDay = true;
            stableId = "trip-" + trip.getTripId();
        } else if (booking.getActivity() != null) {
            it.unical.ea_project.domain.Activity activity = booking.getActivity();
            if (activity.getDeletedAt() != null || activity.getStartDate() == null) {
                throw new IllegalArgumentException("L'attività non ha date valide per il calendario");
            }
            LocalDateTime activityStart = activity.getStartDate();
            LocalDateTime activityEnd = activity.getEndDate() != null ? activity.getEndDate()
                    : activityStart.plusMinutes(activity.getDurationMinutes() == null ? 60 : activity.getDurationMinutes());
            title = safeTitle(activity.getTitle());
            location = joinLocation(activity.getPlaceName(), activity.getCity());
            description = "Prenotazione attività. Posti prenotati: " + (booking.getSeats() == null ? 1 : booking.getSeats());
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
        String now = LocalDateTime.now(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));

        return "BEGIN:VCALENDAR\r\n" +
                "VERSION:2.0\r\n" +
                "PRODID:-//EA Project//Prenotazioni//IT\r\n" +
                "CALSCALE:GREGORIAN\r\n" +
                "METHOD:PUBLISH\r\n" +
                "BEGIN:VEVENT\r\n" +
                "UID:" + escapeIcs(uid) + "\r\n" +
                "DTSTAMP:" + now + "\r\n" +
                dtStart + "\r\n" +
                dtEnd + "\r\n" +
                "SUMMARY:" + escapeIcs(title) + "\r\n" +
                (location.isBlank() ? "" : "LOCATION:" + escapeIcs(location) + "\r\n") +
                "DESCRIPTION:" + escapeIcs(description) + "\r\n" +
                "STATUS:CONFIRMED\r\n" +
                "END:VEVENT\r\n" +
                "END:VCALENDAR\r\n";

    }

    private String escapeIcs(String value) {

        if (value == null) {
            return "";
        }
        
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "");

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
