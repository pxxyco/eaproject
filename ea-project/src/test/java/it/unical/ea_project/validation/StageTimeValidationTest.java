package it.unical.ea_project.validation;

import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.trip.Trip;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class StageTimeValidationTest {
    private void validate(LocalTime start, LocalTime end) {
        Trip trip = new Trip(); trip.setStartDate(LocalDate.of(2027,6,1)); trip.setEndDate(LocalDate.of(2027,6,2));
        Stage s = new Stage(); s.setTitle("Museo"); s.setDay(1); s.setOrderInDay(1); s.setCategory(Stage.StageCategory.VISIT);
        s.setStartTime(start); s.setEndTime(end); ExperienceValidation.stage(trip,s);
    }
    @Test void validAndUnscheduledStagesRemainSupported() {
        assertDoesNotThrow(() -> validate(LocalTime.of(9,0),LocalTime.of(12,0)));
        assertDoesNotThrow(() -> validate(null,null));
        assertDoesNotThrow(() -> validate(LocalTime.of(9,0),null));
    }
    @Test void equalAndReversedTimesReturnBadRequest() {
        var reversed = assertThrows(ResponseStatusException.class, () -> validate(LocalTime.of(18,0),LocalTime.of(10,0)));
        assertEquals(400,reversed.getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> validate(LocalTime.NOON,LocalTime.NOON));
    }
}
