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
import pe.instruccionfacil.service.CatalogoService;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {
    private final CatalogoService catalogo;

    public CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/tipos-clase")
    List<Respuestas.TipoClase> clases() {
        return catalogo.clases(false);
    }

    @GetMapping("/instructores")
    List<Respuestas.Instructor> instructores() {
        return catalogo.instructores();
    }

    @GetMapping("/admin/tipos-clase")
    @PreAuthorize("hasRole('ADMIN')")
    List<Respuestas.TipoClase> todasLasClases() {
        return catalogo.clases(true);
    }

    @PostMapping("/admin/tipos-clase")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.TipoClase nuevaClase(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitudes.Tipo datos) {
        return catalogo.guardarClase(Long.parseLong(jwt.getSubject()), null, datos);
    }

    @PutMapping("/admin/tipos-clase/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    Respuestas.TipoClase editarClase(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                          @Valid @RequestBody Solicitudes.Tipo datos) {
        return catalogo.guardarClase(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @GetMapping("/admin/vehiculos")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Respuestas.Vehiculo> vehiculos() {
        return catalogo.flota();
    }

    @PostMapping("/admin/vehiculos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.Vehiculo nuevoVehiculo(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitudes.Flota datos) {
        return catalogo.guardarVehiculo(Long.parseLong(jwt.getSubject()), null, datos);
    }

    @PutMapping("/admin/vehiculos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    Respuestas.Vehiculo editarVehiculo(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                             @Valid @RequestBody Solicitudes.Flota datos) {
        return catalogo.guardarVehiculo(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @GetMapping("/admin/vehiculos/{id}/bloqueos")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Respuestas.Bloqueo> bloqueos(@PathVariable long id) {
        return catalogo.bloqueos(id);
    }

    @PostMapping("/admin/vehiculos/{id}/bloqueos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.Bloqueo bloquear(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                     @Valid @RequestBody Solicitudes.BloqueoVehiculo datos) {
        return catalogo.crearBloqueo(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @DeleteMapping("/admin/bloqueos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void borrarBloqueo(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        catalogo.borrarBloqueo(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/instructor/horarios")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    List<Respuestas.Horario> misHorarios(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogo.horarios(Long.parseLong(jwt.getSubject()), fecha);
    }

    @GetMapping("/admin/instructores/{id}/horarios")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Respuestas.Horario> horarios(@PathVariable long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogo.horarios(id, fecha);
    }

    @PostMapping("/instructor/horarios")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.Horario nuevoHorario(@AuthenticationPrincipal Jwt jwt,
                         @Valid @RequestBody Solicitudes.Ventana datos) {
        long instructor = Long.parseLong(jwt.getSubject());
        return catalogo.crearHorario(instructor, datos);
    }

    @DeleteMapping("/instructor/horarios/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void borrarHorario(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        catalogo.borrarHorario(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/admin/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    List<Respuestas.Evento> eventos() {
        return catalogo.eventosRecientes();
    }
}
