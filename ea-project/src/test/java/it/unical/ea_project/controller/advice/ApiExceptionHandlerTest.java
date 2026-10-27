package it.unical.ea_project.controller.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    private void verify(String path, String retry) {
        var request = new MockHttpServletRequest("POST", path);
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Temporaneamente occupato"), request);
        assertEquals(429, response.getStatusCode().value());
        assertEquals(retry, response.getHeaders().getFirst("Retry-After"));
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
    }

    @Test
    void busyPhotoDecoderAllowsAShortRetry() {
        verify("/api/trip-images/trip/1/photos", "2");
        verify("/api/activity-images/activity/1/cover", "2");
        verify("/api/users/me/profile-photo", "2");
        verify("/api/chat/conversations/1/photos", "2");
    }

    @Test
    void chatConnectionLimitKeepsItsOwnRetry() {
        verify("/api/chat/events", "30");
    }

    @Test
    void loginLockoutKeepsItsLongerRetry() {
        verify("/api/users/login", "900");
    }
}
