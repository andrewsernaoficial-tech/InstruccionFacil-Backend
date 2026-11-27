package pe.instruccionfacil.service.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.entity.*;
import pe.instruccionfacil.enums.*;
import pe.instruccionfacil.enums.Rol;

public interface UsuarioService {
    UsuarioResponseDTO registrar(RegistroRequestDTO datos);

    UsuarioResponseDTO crearPersonal(PersonalRequestDTO datos);

    List<UsuarioResponseDTO> personal();

    UsuarioResponseDTO editarPersonal(long actor, long id, EdicionPersonalRequestDTO datos);

    void borrarPersonal(long actor, long id);

    SesionResponseDTO login(LoginRequestDTO datos);

    Usuario obtener(long id);

    UsuarioResponseDTO perfil(long id);

    ClienteResponseDTO buscarCliente(String contacto);

    UsuarioResponseDTO actualizar(long id, PerfilRequestDTO datos);
}
