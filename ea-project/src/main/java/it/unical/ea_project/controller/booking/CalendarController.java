package it.unical.ea_project.controller.booking;

import it.unical.ea_project.service.trip.TripService;
import it.unical.ea_project.security.AuthUser;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
public class CalendarController {

    private final TripService tripService;

    public CalendarController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/{tripId}/export/ics")
    public ResponseEntity<byte[]> exportTripToIcs(@PathVariable Long tripId, @AuthenticationPrincipal AuthUser user) {
        String icsContent = tripService.exportTripToIcs(tripId, user.id());
        byte[] bytes = icsContent.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("viaggio_" + tripId + ".ics").build());
        headers.setCacheControl("no-store");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
