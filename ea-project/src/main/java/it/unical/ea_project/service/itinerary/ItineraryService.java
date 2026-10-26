package it.unical.ea_project.service.itinerary;

import it.unical.ea_project.domain.favorites.FavoriteList;
import it.unical.ea_project.domain.itinerary.ItineraryStop;
import it.unical.ea_project.repository.favorites.FavoriteListRepository;
import it.unical.ea_project.repository.itinerary.ItineraryStopRepository;
import it.unical.ea_project.service.favorites.FavoriteListAccess;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Personal plans belong to a favorite list; collaborators never edit catalog
 * offers.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItineraryService {
    public record Stop(Long id, long version, String title, String city, String notes, int day, int position,
            LocalTime startTime, LocalTime endTime) {
    }

    public record Plan(Long listId, String name, boolean editable, List<Stop> stops) {
    }

    private final FavoriteListRepository lists;
    private final FavoriteListAccess permissions;
    private final ItineraryStopRepository stops;

    public Plan get(Long list, Long user) {
        var l = access(list, user, false);
        return new Plan(l.getId(), l.getName(), permissions.canEdit(l, user),
                stops.findByListIdOrderByDayAscPositionAscIdAsc(list).stream().map(this::view).toList());
    }

    @Transactional
    public Stop save(Long list, Long id, Long user, Stop input) {
        var l = access(list, user, true);
        validate(input);
        ItineraryStop stop;
        if (id == null) {
            // access() already locks this list, serializing the size check.
            if (stops.countByListId(list) >= 500)
                throw new IllegalArgumentException("Un itinerario può contenere al massimo 500 tappe.");
            stop = new ItineraryStop();
            stop.setList(l);
        } else {
            stop = stops.findById(id).filter(s -> s.getList().getId().equals(list))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tappa non trovata."));
            if (stop.getVersion() != input.version())
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La tappa è stata modificata da un collaboratore. Ricaricala prima di salvare.");
        }
        stop.setTitle(input.title().strip());
        stop.setCity(input.city() == null ? null : input.city().strip());
        stop.setNotes(input.notes());
        stop.setDay(input.day());
        stop.setPosition(input.position());
        stop.setStartTime(input.startTime());
        stop.setEndTime(input.endTime());
        return view(stops.saveAndFlush(stop));
    }

    @Transactional
    public void delete(Long list, Long id, Long user, long version) {
        access(list, user, true);
        var stop = stops.findById(id).filter(s -> s.getList().getId().equals(list))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tappa non trovata."));
        if (stop.getVersion() != version)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tappa aggiornata da un collaboratore. Ricarica l'itinerario.");
        stops.delete(stop);
        stops.flush();
    }

    private FavoriteList access(Long id, Long user, boolean write) {
        var list = (write ? lists.lockActive(id) : lists.findById(id)).filter(l -> !l.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista non disponibile."));
        if (!permissions.canView(list, user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Itinerario privato.");
        if (write && !permissions.canEdit(list, user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hai il permesso di sola lettura.");
        return list;
    }

    private void validate(Stop s) {
        if (s == null || s.title() == null || s.title().isBlank() || s.title().length() > 180 || s.day() < 1
                || s.day() > 365 || s.position() < 1 || s.position() > 500
                || s.city() != null && s.city().length() > 150 || s.notes() != null && s.notes().length() > 2000
                || s.startTime() != null && s.endTime() != null && !s.endTime().isAfter(s.startTime()))
            throw new IllegalArgumentException("Controlla titolo, giorno, ordine e orari della tappa.");
    }

    private Stop view(ItineraryStop s) {
        return new Stop(s.getId(), s.getVersion(), s.getTitle(), s.getCity(), s.getNotes(), s.getDay(), s.getPosition(),
                s.getStartTime(), s.getEndTime());
    }
}
