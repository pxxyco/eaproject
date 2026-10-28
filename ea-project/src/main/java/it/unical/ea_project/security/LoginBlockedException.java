package it.unical.ea_project.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;

/** Account cooldown, distinct from the HTTP request-rate bucket (ERR-104). */
public final class LoginBlockedException extends ResponseStatusException {
    private final long retryAfterSeconds;

    public LoginBlockedException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Troppi tentativi di accesso falliti. Riprova tra " + Math.max(1, retryAfterSeconds) + " secondi.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        headers.set(HttpHeaders.CACHE_CONTROL, "no-store");
        return headers;
    }
}
