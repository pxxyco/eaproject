package it.unical.ea_project.service.security;

import it.unical.ea_project.security.LoginBlockedException;

public interface LoginAttemptService {
    long retryAfterSeconds(String identifier);

    default boolean isBlocked(String identifier) {
        return retryAfterSeconds(identifier) > 0;
    }

    default void assertAllowed(String identifier) {
        long retry = retryAfterSeconds(identifier);
        if (retry > 0)
            throw new LoginBlockedException(retry);
    }

    void loginFailed(String identifier);

    void loginSucceeded(String identifier);
}
