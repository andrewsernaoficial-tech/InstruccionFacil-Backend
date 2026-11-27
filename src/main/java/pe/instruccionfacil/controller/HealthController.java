package pe.instruccionfacil.controller;

import org.springframework.web.bind.annotation.*;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.service.service.HealthService;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final HealthService salud;

    public HealthController(HealthService salud) {
        this.salud = salud;
    }

    @GetMapping("/health")
    EstadoSaludResponseDTO estado() {
        salud.verificarConexion();
        return new EstadoSaludResponseDTO("UP");
    }
}
