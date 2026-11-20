package pe.instruccionfacil.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.instruccionfacil.config.SeguridadConfig;
import pe.instruccionfacil.dto.*;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.exception.ApiException;
import pe.instruccionfacil.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private static final Duration VIGENCIA = Duration.ofHours(1);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwt;
    private final Clock reloj;
    private final String hashAlternativo;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwords, JwtEncoder jwt, Clock reloj) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.jwt = jwt;
        this.reloj = reloj;
        this.hashAlternativo = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    public Respuestas.Usuario registrar(Solicitudes.Registro datos) {
        String correo = normalizar(datos.email());
        validarContacto(correo, datos.telefono());
        return publicar(usuarios.crear(datos.nombre().trim(), correo, datos.telefono(), hash(datos.password()), Rol.CLIENTE));
    }

    public Respuestas.Usuario crearPersonal(Solicitudes.Personal datos) {
        if (datos.rol() != Rol.INSTRUCTOR && datos.rol() != Rol.RECEPCION) {
            throw ApiException.invalido("El rol debe ser INSTRUCTOR o RECEPCION");
        }
        return publicar(usuarios.crear(datos.nombre().trim(), normalizar(datos.email()), null,
            hash(datos.password()), datos.rol()));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Respuestas.Sesion login(Solicitudes.Login datos) {
        var usuario = usuarios.buscarAcceso(normalizar(datos.usuario()), true).orElse(null);
        var ahora = LocalDateTime.now(reloj);
        if (usuario != null && usuario.bloqueadoHasta() != null && usuario.bloqueadoHasta().isAfter(ahora)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Acceso temporalmente bloqueado. Intenta en 10 minutos");
        }
        String hash = usuario == null ? hashAlternativo : usuario.passwordHash();
        boolean coincide = datos.password().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(datos.password(), hash);
        if (!coincide || usuario == null) {
            if (usuario != null) {
                int intentos = usuario.bloqueadoHasta() == null ? usuario.intentos() + 1 : 1;
                usuarios.acceso(usuario.id(), intentos, intentos >= 5 ? ahora.plusMinutes(10) : null);
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        usuarios.acceso(usuario.id(), 0, null);
        var instante = reloj.instant();
        var claims = JwtClaimsSet.builder().issuer(SeguridadConfig.EMISOR).subject(Long.toString(usuario.id()))
            .issuedAt(instante).expiresAt(instante.plus(VIGENCIA)).claim("role", usuario.rol().name()).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Respuestas.Sesion(jwt.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
            "Bearer", VIGENCIA.toSeconds(), publicar(usuario));
    }

    public Usuario obtener(long id) {
        return usuarios.buscar(id).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
    }

    public Respuestas.Usuario perfil(long id) {
        return publicar(obtener(id));
    }

    public Respuestas.Cliente buscarCliente(String contacto) {
        var cliente = usuarios.buscarAcceso(normalizar(contacto), false)
            .filter(usuario -> usuario.rol() == Rol.CLIENTE)
            .orElseThrow(() -> ApiException.noEncontrado("Cliente no encontrado"));
        return new Respuestas.Cliente(cliente.id(), cliente.nombre());
    }

    @Transactional
    public Respuestas.Usuario actualizar(long id, Solicitudes.Perfil datos) {
        String correo = normalizar(datos.email());
        validarContacto(correo, datos.telefono());
        usuarios.actualizar(id, correo, datos.telefono(), datos);
        return perfil(id);
    }

    private static Respuestas.Usuario publicar(Usuario usuario) {
        return new Respuestas.Usuario(usuario.id(), usuario.nombre(), usuario.email(), usuario.telefono(), usuario.rol(),
            usuario.dni(), usuario.nacimiento(), usuario.direccion());
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private static void validarContacto(String correo, String telefono) {
        if (correo == null && telefono == null) {
            throw ApiException.invalido("Indica un correo o un teléfono");
        }
    }

    private String hash(String password) {
        if (password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72
            || password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            throw ApiException.invalido("La contraseña debe tener 10 caracteres, una letra y un número como mínimo, y hasta 72 bytes");
        }
        return passwords.encode(password);
    }

    @Bean
    ApplicationRunner administradorInicial(@Value("${app.admin-email}") String email,
                                          @Value("${app.admin-password}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            String correo = normalizar(email);
            if (correo == null || !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") || correo.length() > 254) {
                throw new IllegalArgumentException("Configura un APP_ADMIN_EMAIL válido");
            }
            var existente = usuarios.buscarAcceso(correo, false);
            if (existente.isPresent() && existente.get().rol() != Rol.ADMIN) {
                throw new IllegalStateException("APP_ADMIN_EMAIL ya pertenece a una cuenta sin rol ADMIN");
            }
            if (existente.isEmpty()) {
                usuarios.crear("Administrador", correo, null, hash(password), Rol.ADMIN);
            }
        };
    }
}
