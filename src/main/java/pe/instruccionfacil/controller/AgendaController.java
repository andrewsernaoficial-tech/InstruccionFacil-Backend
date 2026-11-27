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
import pe.instruccionfacil.dto.*;

import pe.instruccionfacil.enums.Transmision;
import pe.instruccionfacil.service.service.AgendaService;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class AgendaController {
    private final AgendaService agenda;

    public AgendaController(AgendaService agenda) {
        this.agenda = agenda;
    }

    @GetMapping("/disponibilidad")
    List<DisponibilidadResponseDTO> disponibilidad(@RequestParam long instructorId, @RequestParam long tipoClaseId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam Transmision transmision, @RequestParam(defaultValue = "false") boolean vehiculoPropio) {
        return agenda.disponibilidad(instructorId, tipoClaseId, fecha, transmision, vehiculoPropio);
    }

    @GetMapping("/citas")
    List<CitaResponseDTO> citas(@AuthenticationPrincipal Jwt jwt,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return agenda.listar(Long.parseLong(jwt.getSubject()), desde, hasta);
    }

    @GetMapping("/citas/{id}/instructor-contacto")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','RECEPCION')")
    ContactoInstructorResponseDTO contacto(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return agenda.contactoInstructor(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/citas/{id}")
    CitaResponseDTO cita(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return agenda.consultar(Long.parseLong(jwt.getSubject()), id);
    }

    @PostMapping("/citas")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    CitaResponseDTO crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReservaRequestDTO datos) {
        return agenda.reservar(Long.parseLong(jwt.getSubject()), datos);
    }

    @PatchMapping("/citas/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('CLIENTE','RECEPCION','ADMIN')")
    CitaResponseDTO reprogramar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                     @Valid @RequestBody ReprogramacionRequestDTO datos) {
        return agenda.reprogramar(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @PatchMapping("/citas/{id}/estado")
    CitaResponseDTO estado(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                @Valid @RequestBody CambioEstadoRequestDTO datos) {
        return agenda.cambiarEstado(Long.parseLong(jwt.getSubject()), id, datos);
    }
}
