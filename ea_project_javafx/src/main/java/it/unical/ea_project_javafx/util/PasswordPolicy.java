package it.unical.ea_project_javafx.util;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,128}$"
        );

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
}