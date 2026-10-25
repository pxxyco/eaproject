package it.unical.ea_project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {TripController.class, ActivityController.class, BookingController.class, StageController.class})
public class OfferInputExceptionHandler {
    public record InputError(String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<InputError> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new InputError("Dati non validi o offerta non prenotabile. Controlla i valori e aggiorna i dati."));
    }
}
