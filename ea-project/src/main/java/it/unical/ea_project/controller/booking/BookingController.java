package it.unical.ea_project.controller.booking;

import it.unical.ea_project.dto.booking.BookingDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.booking.BookingHistoryService;
import it.unical.ea_project.service.booking.BookingService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingHistoryService history;

    @GetMapping("/mine/history")
    public BookingHistoryService.HistoryPage history(
            @AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "ALL") String filter, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        return history.page(user.id(), query, filter, page, size);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BookingDTO>> getMine(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(bookingService.getPrenotazioniUtente(user.id()));
    }

    @GetMapping("/mine/experience")
    public List<BookingDTO> experienceBookings(@AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long tripId, @RequestParam(required = false) Long activityId) {
        return bookingService.getExperienceBookings(user.id(), tripId, activityId);
    }

    @GetMapping("/organizer/mine")
    public ResponseEntity<List<BookingDTO>> getOrganizerBookings(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(bookingService.getPrenotazioniOrganizzatore(user.id()));
    }

    @PostMapping("/trips/{tripId}")
    public ResponseEntity<BookingDTO> bookTrip(@PathVariable Long tripId,
            @RequestParam Integer seats,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(bookingService.creaPrenotazioneTrip(user.id(), tripId, seats));
    }

    @PostMapping("/activities/{activityId}")
    public ResponseEntity<BookingDTO> bookActivity(@PathVariable Long activityId,
            @RequestParam Integer seats,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(bookingService.creaPrenotazioneActivity(user.id(), activityId, seats));
    }

    @GetMapping(value = "/{bookingId}/export/ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<byte[]> exportBookingToIcs(@PathVariable Long bookingId,
            @AuthenticationPrincipal AuthUser user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        try {

            String content = bookingService.exportBookingToIcs(bookingId, user.id());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("prenotazione_" + bookingId + ".ics", StandardCharsets.UTF_8).build());
            headers.setCacheControl("no-store");
            return new ResponseEntity<>(content.getBytes(StandardCharsets.UTF_8), headers,
                    HttpStatus.OK);

        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .build();
        }
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable Long bookingId,
            @AuthenticationPrincipal AuthUser user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Devi accedere per cancellare una prenotazione."));
        }
        try {
            bookingService.cancella(bookingId, user.id());
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Puoi cancellare soltanto le tue prenotazioni."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage() == null
                            ? "Prenotazione non valida."
                            : exception.getMessage()));
        }
    }

}
