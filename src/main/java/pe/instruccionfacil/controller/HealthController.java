package pe.instruccionfacil.controller;

import org.springframework.web.bind.annotation.*;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.service.HealthService;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final HealthService salud;

    public HealthController(HealthService salud) {
        this.salud = salud;
    }

    @GetMapping("/health")
    Respuestas.EstadoSalud estado() {
        salud.verificarConexion();
        return new Respuestas.EstadoSalud("UP");
    }
}
