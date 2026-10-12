package it.unical.ea_project.service;


import it.unical.ea_project.dto.BookingDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BookingService {

    BookingDTO creaPrenotazioneTrip(Long userId, Long tripId, Integer seats);

    BookingDTO creaPrenotazioneActivity(Long userId, Long activityId, Integer seats);

    List<BookingDTO> getPrenotazioniUtente(Long userId);

    List<BookingDTO> getPrenotazioniOrganizzatore(Long organizerId);

    void cancella(Long bookingId, Long userId);

}
