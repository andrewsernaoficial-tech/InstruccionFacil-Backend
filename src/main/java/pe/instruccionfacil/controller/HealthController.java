package pe.instruccionfacil.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.*;
import pe.instruccionfacil.service.HealthService;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final HealthService salud;

    public HealthController(HealthService salud) {
        this.salud = salud;
    }

    @GetMapping("/health")
    Map<String, String> estado() {
        salud.verificarConexion();
        return Map.of("status", "UP");
    }
}
