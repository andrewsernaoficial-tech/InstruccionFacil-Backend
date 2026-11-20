package pe.instruccionfacil.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.service.UsuarioService;

@RestController
@RequestMapping("/api/v1")
public class UsuarioController {
    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.Usuario registro(@Valid @RequestBody Solicitudes.Registro datos) {
        return usuarios.registrar(datos);
    }

    @PostMapping("/auth/login")
    Respuestas.Sesion login(@Valid @RequestBody Solicitudes.Login datos) {
        return usuarios.login(datos);
    }

    @GetMapping("/usuarios/me")
    @SecurityRequirement(name = "bearerAuth")
    Respuestas.Usuario perfil(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.perfil(Long.parseLong(jwt.getSubject()));
    }

    @PutMapping("/usuarios/me")
    @SecurityRequirement(name = "bearerAuth")
    Respuestas.Usuario perfil(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitudes.Perfil datos) {
        return usuarios.actualizar(Long.parseLong(jwt.getSubject()), datos);
    }

    @PostMapping("/admin/personal")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    Respuestas.Usuario personal(@Valid @RequestBody Solicitudes.Personal datos) {
        return usuarios.crearPersonal(datos);
    }

    @GetMapping("/admin/clientes/busqueda")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    @SecurityRequirement(name = "bearerAuth")
    Respuestas.Cliente buscarCliente(@RequestParam String contacto) {
        return usuarios.buscarCliente(contacto);
    }
}
