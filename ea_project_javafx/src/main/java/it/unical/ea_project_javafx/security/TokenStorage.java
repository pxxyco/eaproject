package it.unical.ea_project_javafx.security;

import java.util.prefs.Preferences;

public final class TokenStorage {

    private static final Preferences PREFS = Preferences.userNodeForPackage(TokenStorage.class);
    private static final String REFRESH_TOKEN_KEY = "jwt_refresh_token";

    private TokenStorage() {
    }

    public static void saveRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            PREFS.remove(REFRESH_TOKEN_KEY);
            return;
        }
        PREFS.put(REFRESH_TOKEN_KEY, refreshToken);
    }

    public static String getRefreshToken() {
        return PREFS.get(REFRESH_TOKEN_KEY, null);
    }

    public static void clear() {
        PREFS.remove(REFRESH_TOKEN_KEY);
    }
}
