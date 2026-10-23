package it.unical.ea_project.controller;

import it.unical.ea_project.dto.BookingDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/mine")
    public ResponseEntity<List<BookingDTO>> getMine(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(bookingService.getPrenotazioniUtente(user.id()));
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
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename("prenotazione_" + bookingId + ".ics", java.nio.charset.StandardCharsets.UTF_8).build());
            headers.setCacheControl("no-store");
            return new ResponseEntity<>(content.getBytes(java.nio.charset.StandardCharsets.UTF_8), headers, HttpStatus.OK);
        
        } catch (org.springframework.security.access.AccessDeniedException ex) {
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
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Puoi cancellare soltanto le tue prenotazioni."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage() == null
                            ? "Prenotazione non valida." : exception.getMessage()));
        }
    }

}
