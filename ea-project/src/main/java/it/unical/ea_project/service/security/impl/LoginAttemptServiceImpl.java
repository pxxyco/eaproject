package it.unical.ea_project.service.security.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Five failed logins, one minute cooldown; username/email share the account key. */
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {
    private static final Logger AUDIT = LogManager.getLogger("AUDIT");
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final UserRepository users;
    private final Ticker ticker;
    private final Cache<String, Attempts> attempts;

    private record Attempts(int count, long lastFailure) {
    }

    @Autowired
    public LoginAttemptServiceImpl(UserRepository users) {
        this(users, Ticker.systemTicker());
    }

    LoginAttemptServiceImpl(UserRepository users, Ticker ticker) {
        this.users = users;
        this.ticker = ticker;
        attempts = Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(WINDOW).ticker(ticker).build();
    }

    private String key(String identifier) {
        String cleaned = identifier == null ? "" : identifier.strip().toLowerCase(Locale.ROOT);
        String canonical = users.findByEmailIgnoreCase(cleaned).or(() -> users.findByUsernameIgnoreCase(cleaned))
                .map(u -> "user:" + u.getId()).orElse("identifier:" + cleaned);
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @Override
    public long retryAfterSeconds(String identifier) {
        Attempts current = attempts.getIfPresent(key(identifier));
        if (current == null || current.count() < MAX_ATTEMPTS)
            return 0;
        long remaining = WINDOW.toNanos() - Math.max(0, ticker.read() - current.lastFailure());
        return Math.max(1, (remaining + 999_999_999L) / 1_000_000_000L);
    }

    @Override
    public void loginFailed(String identifier) {
        String key = key(identifier);
        Attempts updated = attempts.asMap().compute(key, (ignored, current) -> new Attempts(
                Math.min(MAX_ATTEMPTS, (current == null ? 0 : current.count()) + 1), ticker.read()));
        if (updated.count() == MAX_ATTEMPTS)
            AUDIT.warn("LOGIN_LOCKED accountHash={} cooldownSeconds={}", key.substring(0, 12), WINDOW.toSeconds());
        else
            AUDIT.info("LOGIN_FAILED accountHash={} attempt={}", key.substring(0, 12), updated.count());
    }

    @Override
    public void loginSucceeded(String identifier) {
        attempts.invalidate(key(identifier));
        AUDIT.info("LOGIN_SUCCESS");
    }
}
