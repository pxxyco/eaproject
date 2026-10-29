package it.unical.ea_project_javafx.ui;

import java.math.BigDecimal;
import java.util.Locale;

public final class UiText {

    private UiText() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ITALY).contains(query);
    }

    public static String money(BigDecimal value) {
        return value == null ? "Importo non disponibile" : "€ " + String.format(Locale.ITALY, "%,.2f", value);
    }

}
