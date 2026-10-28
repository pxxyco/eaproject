package it.unical.ea_project.service.security.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.security.LoginBlockedException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceImplTest {
    private final AtomicLong time = new AtomicLong();
    private LoginAttemptServiceImpl service;

    @BeforeEach
    void setup() {
        var users = mock(UserRepository.class);
        var user = new User();
        user.setId(42L);
        when(users.findByEmailIgnoreCase(anyString())).thenAnswer(call ->
                "alice@example.test".equalsIgnoreCase(call.getArgument(0)) ? Optional.of(user) : Optional.empty());
        when(users.findByUsernameIgnoreCase(anyString())).thenAnswer(call ->
                "alice".equalsIgnoreCase(call.getArgument(0)) ? Optional.of(user) : Optional.empty());
        service = new LoginAttemptServiceImpl(users, time::get);
    }

    @Test
    void fiveFailuresShareOneCounterAcrossUsernameEmailAndCase() {
        for (String identifier : new String[] {"alice", "ALICE@example.test", " ALICE ", "alice@example.test"}) {
            service.loginFailed(identifier);
            assertFalse(service.isBlocked("alice"));
        }
        service.loginFailed("alice");
        assertTrue(service.isBlocked("ALICE@example.test"));
        var blocked = assertThrows(LoginBlockedException.class, () -> service.assertAllowed("alice"));
        assertEquals(60, blocked.retryAfterSeconds());
    }

    @Test
    void oneMinuteCooldownExpiresAndRetryAfterCountsDown() {
        for (int i = 0; i < 5; i++) service.loginFailed("alice");
        time.addAndGet(Duration.ofSeconds(30).toNanos());
        assertEquals(30, service.retryAfterSeconds("alice"));
        time.addAndGet(Duration.ofSeconds(30).toNanos());
        assertFalse(service.isBlocked("alice"));
        service.loginFailed("alice");
        assertFalse(service.isBlocked("alice"));
    }

    @Test
    void successfulLoginClearsAttemptsForBothAliases() {
        for (int i = 0; i < 4; i++) service.loginFailed("alice");
        service.loginSucceeded("alice@example.test");
        service.loginFailed("alice");
        assertFalse(service.isBlocked("alice@example.test"));
    }

    @Test
    void concurrentFailuresAreNotLost() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Future<?>>();
            for (int i = 0; i < 5; i++) tasks.add(pool.submit(() -> service.loginFailed("alice")));
            for (var task : tasks) task.get();
        }
        assertTrue(service.isBlocked("alice@example.test"));
    }

    @Test
    void unknownIdentifierCannotCollideWithAnAccountId() {
        for (int i = 0; i < 5; i++) service.loginFailed("42");
        assertTrue(service.isBlocked("42"));
        assertFalse(service.isBlocked("alice"));
    }
}
