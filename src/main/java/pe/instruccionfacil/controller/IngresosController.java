package pe.instruccionfacil.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.service.service.IngresosService;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class IngresosController {
    private final IngresosService ingresos;

    public IngresosController(IngresosService ingresos) { this.ingresos = ingresos; }

    @GetMapping("/api/v1/admin/ingresos")
    @PreAuthorize("hasRole('ADMIN')")
    IngresosResponseDTO consultar(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ingresos.consultar(desde, hasta);
    }
}
