package it.unical.ea_project.service.security.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.security.LoginAttemptService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {
    private static final Logger AUDIT = LogManager.getLogger("AUDIT");
    private final UserRepository users;
    private final Cache<String, Attempts> attempts = Caffeine.newBuilder().maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(15)).build();

    private static final class Attempts {
        int count;
    }

    public LoginAttemptServiceImpl(UserRepository users) {
        this.users = users;
    }

    private String key(String identifier) {
        String cleaned = identifier == null ? "" : identifier.strip().toLowerCase(Locale.ROOT);
        String canonical = users.findByEmailIgnoreCase(cleaned).or(() -> users.findByUsernameIgnoreCase(cleaned))
                .map(u -> u.getId().toString()).orElse(cleaned);
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void assertAllowed(String identifier) {
        Attempts a = attempts.getIfPresent(key(identifier));
        if (a != null)
            synchronized (a) {
                if (a.count >= 10)
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Troppi tentativi di accesso falliti. Riprova tra 15 minuti.");
            }
    }

    @Override
    public void loginFailed(String identifier) {
        String key = key(identifier);
        Attempts a = attempts.get(key, k -> new Attempts());
        synchronized (a) {
            a.count++;
            if (a.count == 10)
                AUDIT.warn("LOGIN_LOCKED accountHash={}", key.substring(0, 12));
            else
                AUDIT.info("LOGIN_FAILED accountHash={} attempt={}", key.substring(0, 12), a.count);
        }
    }

    @Override
    public void loginSucceeded(String identifier) {
        attempts.invalidate(key(identifier));
        AUDIT.info("LOGIN_SUCCESS");
    }
}
