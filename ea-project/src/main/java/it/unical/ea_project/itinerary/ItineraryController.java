package it.unical.ea_project.itinerary;

import it.unical.ea_project.security.AuthUser;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/itineraries")
@lombok.RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ItineraryController {
    private final ItineraryService service;

    @GetMapping("/{list}")
    public ItineraryService.Plan get(@PathVariable Long list, @AuthenticationPrincipal AuthUser user) {
        return service.get(list, user.id());
    }

    @PostMapping("/{list}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    public ItineraryService.Stop add(@PathVariable Long list, @AuthenticationPrincipal AuthUser user,
            @RequestBody ItineraryService.Stop stop) {
        return service.save(list, null, user.id(), stop);
    }

    @PutMapping("/{list}/stops/{id}")
    public ItineraryService.Stop update(@PathVariable Long list, @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user, @RequestBody ItineraryService.Stop stop) {
        return service.save(list, id, user.id(), stop);
    }

    @DeleteMapping("/{list}/stops/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long list, @PathVariable Long id, @RequestParam long version,
            @AuthenticationPrincipal AuthUser user) {
        service.delete(list, id, user.id(), version);
    }
}
