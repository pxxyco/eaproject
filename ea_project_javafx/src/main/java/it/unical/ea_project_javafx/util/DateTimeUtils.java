package it.unical.ea_project_javafx.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTimeUtils {

    private static final Pattern DATE_TIME_PATTERN = Pattern
            .compile("^(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}(?::\\d{2})?)");

    private DateTimeUtils() {
    }

    public static String formatDateTime(String input) {
        Matcher matcher = DATE_TIME_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2);
        }
        return input;
    }
}
