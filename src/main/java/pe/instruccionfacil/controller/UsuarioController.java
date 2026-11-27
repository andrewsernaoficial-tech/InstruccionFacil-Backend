package pe.instruccionfacil.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.service.service.UsuarioService;

@RestController
@RequestMapping("/api/v1")
public class UsuarioController {
    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResponseDTO registro(@Valid @RequestBody RegistroRequestDTO datos) {
        return usuarios.registrar(datos);
    }

    @PostMapping("/auth/login")
    SesionResponseDTO login(@Valid @RequestBody LoginRequestDTO datos) {
        return usuarios.login(datos);
    }

    @GetMapping("/usuarios/me")
    @SecurityRequirement(name = "bearerAuth")
    UsuarioResponseDTO perfil(@AuthenticationPrincipal Jwt jwt) {
        return usuarios.perfil(Long.parseLong(jwt.getSubject()));
    }

    @PutMapping("/usuarios/me")
    @SecurityRequirement(name = "bearerAuth")
    UsuarioResponseDTO perfil(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PerfilRequestDTO datos) {
        return usuarios.actualizar(Long.parseLong(jwt.getSubject()), datos);
    }

    @PostMapping("/admin/personal")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResponseDTO personal(@Valid @RequestBody PersonalRequestDTO datos) {
        return usuarios.crearPersonal(datos);
    }

    @GetMapping("/admin/personal")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    List<UsuarioResponseDTO> listaPersonal() {
        return usuarios.personal();
    }

    @PutMapping("/admin/personal/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    UsuarioResponseDTO editarPersonal(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                      @Valid @RequestBody EdicionPersonalRequestDTO datos) {
        return usuarios.editarPersonal(Long.parseLong(jwt.getSubject()), id, datos);
    }

    @DeleteMapping("/admin/personal/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void borrarPersonal(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        usuarios.borrarPersonal(Long.parseLong(jwt.getSubject()), id);
    }

    @GetMapping("/admin/clientes/busqueda")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    @SecurityRequirement(name = "bearerAuth")
    ClienteResponseDTO buscarCliente(@RequestParam String contacto) {
        return usuarios.buscarCliente(contacto);
    }
}
