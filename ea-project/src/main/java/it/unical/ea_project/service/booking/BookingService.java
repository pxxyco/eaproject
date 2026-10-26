package it.unical.ea_project.service.booking;

import it.unical.ea_project.dto.booking.BookingDTO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface BookingService {

    BookingDTO creaPrenotazioneTrip(Long userId, Long tripId, Integer seats);

    BookingDTO creaPrenotazioneActivity(Long userId, Long activityId, Integer seats);

    List<BookingDTO> getPrenotazioniUtente(Long userId);

    List<BookingDTO> getBookingsByIds(Long userId, List<Long> ids);

    List<BookingDTO> getPrenotazioniOrganizzatore(Long organizerId);

    void cancella(Long bookingId, Long userId);

    String exportBookingToIcs(Long bookingId, Long userId);

}
