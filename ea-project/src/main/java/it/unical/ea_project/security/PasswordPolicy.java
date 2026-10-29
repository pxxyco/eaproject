package it.unical.ea_project.security;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    private PasswordPolicy() {
    }

    public static Optional<String> validate(String password) {
        if (password == null || password.isBlank()) {
            return Optional.of("La password è obbligatoria.");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return Optional.of(
                    "La password deve contenere almeno 8 caratteri, una lettera, un numero e un carattere speciale.");
        }
        return Optional.empty();
    }
}
