package it.unical.ea_project.service.security;

public interface LoginAttemptService {
    void assertAllowed(String identifier);

    void loginFailed(String identifier);

    void loginSucceeded(String identifier);
}
