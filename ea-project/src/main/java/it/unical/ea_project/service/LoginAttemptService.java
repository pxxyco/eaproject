package it.unical.ea_project.service;

public interface LoginAttemptService {
    boolean isBlocked(String identifier);
    void loginFailed(String identifier);
    void loginSucceeded(String identifier);
}