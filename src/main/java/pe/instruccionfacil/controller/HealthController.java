package pe.instruccionfacil.controller;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    Map<String, String> estado() {
        jdbc.queryForObject("SELECT 1 FROM dual", Integer.class);
        return Map.of("status", "UP");
    }
}
