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
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.repository.AuditoriaRepository;
import pe.instruccionfacil.service.CatalogoService;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {
    private final CatalogoService catalogo;
    private final AuditoriaRepository auditoria;

    public CatalogoController(CatalogoService catalogo, AuditoriaRepository auditoria) {
        this.catalogo = catalogo;
        this.auditoria = auditoria;
    }

    @GetMapping("/tipos-clase")
    List<TipoClase> clases() {
        return catalogo.clases(false);
    }

    @GetMapping("/instructores")
    List<Respuestas.Instructor> instructores() {
        return catalogo.instructores();
    }

    @GetMapping("/admin/tipos-clase")
    @PreAuthorize("hasRole('ADMIN')")
    List<TipoClase> todasLasClases() {
        return catalogo.clases(true);
    }

    @PostMapping("/admin/tipos-clase")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    TipoClase nuevaClase(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitudes.Tipo datos) {
        return catalogo.guardarClase(Long.parseLong(jwt.getSubject()), null, datos);
    }

    @PutMapping("/admin/tipos-clase/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    TipoClase editarClase(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                          @Valid @RequestBody Solicitudes.Tipo datos) {
        return catalogo.guardarClase(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @GetMapping("/admin/vehiculos")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Vehiculo> vehiculos() {
        return catalogo.flota();
    }

    @PostMapping("/admin/vehiculos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Vehiculo nuevoVehiculo(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitudes.Flota datos) {
        return catalogo.guardarVehiculo(Long.parseLong(jwt.getSubject()), null, datos);
    }

    @PutMapping("/admin/vehiculos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    Vehiculo editarVehiculo(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                             @Valid @RequestBody Solicitudes.Flota datos) {
        return catalogo.guardarVehiculo(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @GetMapping("/admin/vehiculos/{id}/bloqueos")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Bloqueo> bloqueos(@PathVariable long id) {
        return catalogo.bloqueos(id);
    }

    @PostMapping("/admin/vehiculos/{id}/bloqueos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Bloqueo bloquear(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                     @Valid @RequestBody Solicitudes.BloqueoVehiculo datos) {
        return catalogo.crearBloqueo(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @DeleteMapping("/admin/bloqueos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void borrarBloqueo(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        catalogo.borrarBloqueo(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/admin/instructores/{id}/horarios")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    List<Horario> horarios(@PathVariable long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return catalogo.horarios(id, fecha);
    }

    @PostMapping("/admin/instructores/{id}/horarios")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    Horario nuevoHorario(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                         @Valid @RequestBody Solicitudes.Ventana datos) {
        return catalogo.crearHorario(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @DeleteMapping("/admin/horarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void borrarHorario(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        catalogo.borrarHorario(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/admin/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    List<Evento> eventos() {
        return auditoria.recientes();
    }
}
