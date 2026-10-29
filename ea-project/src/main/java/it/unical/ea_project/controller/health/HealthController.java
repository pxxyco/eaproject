package it.unical.ea_project.controller.health;

import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final DataSource database;

    public HealthController(DataSource database) {
        this.database = database;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        try (var connection = database.getConnection()) {
            if (connection.isValid(2))
                return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (SQLException ignored) {
        }
        return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }
}
