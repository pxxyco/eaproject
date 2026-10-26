package it.unical.ea_project.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.LoginAttemptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;

    private final Cache<String, Integer> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @Override
    public boolean isBlocked(String identifier) {
        Integer attempts = attemptsCache.getIfPresent(normalize(identifier));
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }

    @Override
    public void loginFailed(String identifier) {
        String key = normalize(identifier);
        int attempts = attemptsCache.asMap().merge(key, 1, Integer::sum);
        log.warn("Login fallito (tentativo {}) per chiave hash={}", attempts, key.hashCode());
        if (attempts == MAX_ATTEMPTS) {
            log.error("AVVISO DI SICUREZZA: account bloccato per 1 minuto dopo {} tentativi", attempts);
        }
    }

    @Override
    public void loginSucceeded(String identifier) {
        attemptsCache.invalidate(normalize(identifier));
    }

    private String normalize(String identifier) {
        return identifier == null ? "unknown" : identifier.trim().toLowerCase();
    }
}