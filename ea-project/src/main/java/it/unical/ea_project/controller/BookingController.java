package it.unical.ea_project.controller;

import it.unical.ea_project.dto.BookingDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancel(@PathVariable Long bookingId,
                                       @AuthenticationPrincipal AuthUser user) {
        bookingService.cancella(bookingId, user.id());
        return ResponseEntity.noContent().build();
    }

}
