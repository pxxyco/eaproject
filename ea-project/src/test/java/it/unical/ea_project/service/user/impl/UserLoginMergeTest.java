package it.unical.ea_project.service.user.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import it.unical.ea_project.controller.advice.ApiExceptionHandler;
import it.unical.ea_project.controller.user.UserController;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.security.LoginBlockedException;
import it.unical.ea_project.service.media.PhotoStorageService;
import it.unical.ea_project.service.security.JwtService;
import it.unical.ea_project.service.security.LoginAttemptService;
import it.unical.ea_project.service.security.impl.LoginAttemptServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserLoginMergeTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final LoginAttemptService attempts = mock(LoginAttemptService.class);
    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        when(passwords.encode(anyString())).thenReturn("dummy-hash");
        service = new UserServiceImpl(users, passwords, attempts, mock(PhotoStorageService.class));
        service.initDummyHash();
        clearInvocations(passwords);
    }

    private User user() {
        var user = new User();
        user.setId(42L);
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setRole(User.Role.TRAVELER);
        user.setHashedPassword("stored-hash");
        when(users.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void unknownAndDeletedAccountsStillPerformThePasswordCheck() {
        assertTrue(service.login("missing", "wrong").isEmpty());
        verify(passwords).matches("wrong", "dummy-hash");
        verify(attempts).loginFailed("missing");
        var deleted = user();
        deleted.setDeletedAt(LocalDateTime.now());
        assertTrue(service.login("alice", "other").isEmpty());
        verify(passwords).matches("other", "dummy-hash");
        verify(passwords, never()).matches("other", "stored-hash");
    }

    @Test
    void wrongPasswordIsCountedExactlyOnceByTheService() {
        user();
        assertTrue(service.login("alice", "wrong").isEmpty());
        verify(attempts).assertAllowed("alice");
        verify(attempts).loginFailed("alice");
        verify(attempts, never()).loginSucceeded(anyString());
    }

    @Test
    void correctPasswordClearsTheFailureCounter() {
        var user = user();
        when(passwords.matches("correct", "stored-hash")).thenReturn(true);
        assertSame(user, service.login("alice", "correct").orElseThrow());
        verify(attempts).loginSucceeded("alice");
        verify(attempts, never()).loginFailed(anyString());
    }

    @Test
    void blockedServiceCallCannotBypassTheLimit() {
        doThrow(new LoginBlockedException(24)).when(attempts).assertAllowed("alice");
        assertThrows(LoginBlockedException.class, () -> service.login("alice", "correct"));
        verifyNoInteractions(passwords);
    }

    @Test
    void httpLoginPreservesTheNewErrorCodeAndActualCooldown() throws Exception {
        user();
        var realService = new UserServiceImpl(users, passwords, new LoginAttemptServiceImpl(users),
                mock(PhotoStorageService.class));
        realService.initDummyHash();
        var jwt = mock(JwtService.class);
        var http = MockMvcBuilders.standaloneSetup(new UserController(realService, jwt, users))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        for (int i = 0; i < 5; i++) {
            http.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"identifier\":\"alice\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }
        when(passwords.matches("correct", "stored-hash")).thenReturn(true);
        var response = http.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"alice\",\"password\":\"correct\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("ERR-105"))
                .andExpect(header().doesNotExist("Authorization")).andReturn().getResponse();
        long retry = Long.parseLong(response.getHeader("Retry-After"));
        assertTrue(retry >= 1 && retry <= 60);
        verifyNoInteractions(jwt);
    }
}
