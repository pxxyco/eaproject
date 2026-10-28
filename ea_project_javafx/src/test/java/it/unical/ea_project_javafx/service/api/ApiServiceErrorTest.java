package it.unical.ea_project_javafx.service.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ApiServiceErrorTest {
    @Test
    void accountCooldownKeepsItsDistinctCodeAndServerMessage() {
        assertEquals("ERR-105 — Riprova tra 31 secondi.", ApiService.extractErrorMessage(
                "{\"code\":\"ERR-105\",\"message\":\"Riprova tra 31 secondi.\"}", 429));
    }

    @Test
    void requestRateLimitKeepsItsOriginalCode() {
        assertEquals("ERR-104 — Troppe richieste.", ApiService.extractErrorMessage(
                "{\"code\":\"ERR-104\",\"message\":\"Troppe richieste.\"}", 429));
    }
}
