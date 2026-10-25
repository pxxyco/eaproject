package it.unical.ea_project.service;

import it.unical.ea_project.dto.BookingDTO;
import it.unical.ea_project.dto.ReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The database filters and pages history; only the selected bookings and
 * reviews are materialized.
 */
@Service
@RequiredArgsConstructor
public class BookingHistoryService {
    private final NamedParameterJdbcTemplate jdbc;
    private final BookingService bookings;

    public record HistoryPage(List<BookingDTO> items, List<ReviewDTO> reviews, boolean hasMore,
            long total, long completed, long cancelled, long reviewable, long matched) {
    }

    private static final String BASE = """
            WITH entries AS (
              SELECT b.*, b.id AS booking_id, b.status AS booking_status, b.type AS booking_type, coalesce(t.title,a.title,'') AS title,
              concat_ws(' ',t.destination_city,t.destination_country,a.city,a.place_name) AS location,
              coalesce(cast(t.start_date AS timestamp),a.start_date) AS starts,
              coalesce(cast(coalesce(t.end_date,t.start_date) AS timestamp)+interval '1 day',a.end_date,a.start_date) AS ends,
              (b.status='CANCELLED' OR coalesce(t.status,a.status,'')='CANCELLED') AS cancelled,
              (b.status='COMPLETED' OR coalesce(t.status,a.status,'')='COMPLETED'
                OR coalesce(cast(coalesce(t.end_date,t.start_date) AS timestamp)+interval '1 day',a.end_date,a.start_date)<=:now) AS ended,
              EXISTS(SELECT 1 FROM "REVIEW" r WHERE r.user_id=:user AND r.deleted_at=false
                     AND (r.trip_id=b.trip_id OR r.activity_id=b.activity_id)) AS reviewed
              FROM "BOOKING" b LEFT JOIN "TRIP" t ON t.trip_id=b.trip_id
              LEFT JOIN "ACTIVITY" a ON a.activity_id=b.activity_id
              WHERE b.user_id=:user AND b.deleted_at IS NULL
            ), history AS (
              SELECT *, (NOT cancelled AND ended AND booking_status IN ('CONFIRMED','COMPLETED')) AS eligible
              FROM entries WHERE cancelled OR ended
            )
            """;

    @Transactional(readOnly = true)
    public HistoryPage page(Long user, String query, String filter, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > 20)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Pagina non valida");
        String text = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (text.length() > 200)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Ricerca troppo lunga");
        var parameters = new MapSqlParameterSource("user", user).addValue("now", LocalDateTime.now())
                .addValue("query", "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%")
                .addValue("limit", size).addValue("offset", (long) page * size);
        String condition = switch (filter == null ? "ALL" : filter) {
            case "COMPLETED" -> "NOT cancelled AND ended";
            case "CANCELLED" -> "cancelled";
            case "REVIEWABLE" -> "eligible AND NOT reviewed";
            case "REVIEWED" -> "reviewed";
            case "ALL" -> "true";
            default -> throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Filtro non valido");
        };
        String where = " WHERE (" + condition
                + ") AND lower(concat_ws(' ',title,location,to_char(starts,'DD/MM/YYYY'),to_char(ends,'DD/MM/YYYY'),booking_type)) LIKE :query";
        Map<String, Object> counts = jdbc.queryForMap(
                BASE + """
                        SELECT count(*) AS total, count(*) FILTER(WHERE NOT cancelled AND ended) AS completed,
                          count(*) FILTER(WHERE cancelled) AS cancelled,
                          count(DISTINCT coalesce('T'||trip_id::text,'A'||activity_id::text)) FILTER(WHERE eligible AND NOT reviewed) AS reviewable
                        FROM history
                        """,
                parameters);
        long matched = jdbc.queryForObject(BASE + "SELECT count(*) FROM history" + where, parameters, Long.class);
        List<Long> ids = jdbc.queryForList(BASE + "SELECT booking_id FROM history" + where +
                " ORDER BY ends DESC NULLS LAST, created_at DESC, booking_id DESC LIMIT :limit OFFSET :offset",
                parameters, Long.class);
        List<BookingDTO> items = bookings.getBookingsByIds(user, ids);
        List<ReviewDTO> reviews = ids.isEmpty() ? List.of()
                : jdbc.query(
                        """
                                SELECT DISTINCT r.* FROM "REVIEW" r JOIN "BOOKING" b ON (r.trip_id=b.trip_id OR r.activity_id=b.activity_id)
                                WHERE r.user_id=:user AND b.user_id=:user AND r.deleted_at=false AND b.id IN (:ids)
                                """,
                        new MapSqlParameterSource("user", user).addValue("ids", ids), (rs, n) -> ReviewDTO.builder()
                                .reviewId(rs.getLong("review_id")).userId(user).tripId((Long) rs.getObject("trip_id"))
                                .activityId((Long) rs.getObject("activity_id")).rating(rs.getInt("rating"))
                                .comment(rs.getString("comment"))
                                .createdAt(rs.getTimestamp("created_at").toLocalDateTime()).deleted(false).build());
        return new HistoryPage(items, reviews, (long) page * size + ids.size() < matched,
                ((Number) counts.get("total")).longValue(), ((Number) counts.get("completed")).longValue(),
                ((Number) counts.get("cancelled")).longValue(), ((Number) counts.get("reviewable")).longValue(),
                matched);
    }
}
