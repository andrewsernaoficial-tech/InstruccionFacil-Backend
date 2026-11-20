package pe.instruccionfacil.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.dto.Respuestas.Disponibilidad;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.service.AgendaService;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class AgendaController {
    private final AgendaService agenda;

    public AgendaController(AgendaService agenda) {
        this.agenda = agenda;
    }

    @GetMapping("/disponibilidad")
    List<Disponibilidad> disponibilidad(@RequestParam long instructorId, @RequestParam long tipoClaseId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam Transmision transmision, @RequestParam(defaultValue = "false") boolean vehiculoPropio) {
        return agenda.disponibilidad(instructorId, tipoClaseId, fecha, transmision, vehiculoPropio);
    }

    @GetMapping("/citas")
    List<Cita> citas(@AuthenticationPrincipal Jwt jwt,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return agenda.listar(Long.parseLong(jwt.getSubject()), desde, hasta);
    }

    @GetMapping("/citas/{id}")
    Cita cita(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return agenda.autorizada(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping("/citas")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Cita crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitudes.Reserva datos) {
        return agenda.reservar(Long.parseLong(jwt.getSubject()), datos);
    }

    @PatchMapping("/citas/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    Cita reprogramar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                     @Valid @RequestBody Solicitudes.Reprogramacion datos) {
        return agenda.reprogramar(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @PatchMapping("/citas/{id}/estado")
    Cita estado(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                @Valid @RequestBody Solicitudes.CambioEstado datos) {
        return agenda.cambiarEstado(Long.parseLong(jwt.getSubject()), id, datos);
    }
}
