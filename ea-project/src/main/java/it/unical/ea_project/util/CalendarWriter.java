package it.unical.ea_project.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * RFC 5545 escaping and folding, shared by offer and personal booking exports.
 */
public final class CalendarWriter {
    private CalendarWriter() {
    }

    public static String event(String uid, String title, String location, String description,
            String start, String end, String status) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC).format(Instant.now());
        String content = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//EA Project//Calendario//IT\r\n"
                + "CALSCALE:GREGORIAN\r\nMETHOD:PUBLISH\r\nBEGIN:VEVENT\r\n"
                + "UID:" + escape(uid) + "\r\nDTSTAMP:" + timestamp + "\r\n" + start + "\r\n" + end
                + "\r\nSUMMARY:" + escape(title) + "\r\nLOCATION:" + escape(location)
                + "\r\nDESCRIPTION:" + escape(description) + "\r\nSTATUS:" + status
                + "\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        StringBuilder folded = new StringBuilder();
        for (String line : content.split("\r\n")) {
            int bytes = 0;
            for (int offset = 0; offset < line.length();) {
                int cp = line.codePointAt(offset);
                String character = new String(Character.toChars(cp));
                int length = character.getBytes(StandardCharsets.UTF_8).length;
                if (bytes + length > 75) {
                    folded.append("\r\n ");
                    bytes = 1;
                }
                folded.append(character);
                bytes += length;
                offset += Character.charCount(cp);
            }
            folded.append("\r\n");
        }
        return folded.toString();
    }

    private static String escape(String value) {
        return value == null ? ""
                : value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                        .replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "");
    }
}
