package pe.instruccionfacil.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.dto.Respuestas;
import pe.instruccionfacil.entity.Modelo.*;

@Repository
public class UsuarioRepository {
    private static final RowMapper<Usuario> FILA = (rs, n) -> new Usuario(
        rs.getLong("id"), rs.getString("nombre"), rs.getString("email"), rs.getString("telefono"),
        Rol.valueOf(rs.getString("rol")), rs.getString("password_hash"), rs.getInt("intentos_fallidos"),
        rs.getObject("bloqueado_hasta", LocalDateTime.class), rs.getString("dni"),
        rs.getDate("nacimiento") == null ? null : rs.getDate("nacimiento").toLocalDate(), rs.getString("direccion"));

    private final JdbcTemplate jdbc;
    private final Identificadores ids;

    public UsuarioRepository(JdbcTemplate jdbc, Identificadores ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    public Optional<Usuario> buscar(long id) {
        return jdbc.query("SELECT * FROM usuario WHERE id = ?", FILA, id).stream().findFirst();
    }

    public Optional<Usuario> buscarAcceso(String acceso, boolean bloquear) {
        String sql = "SELECT * FROM usuario WHERE email = ? OR telefono = ?";
        if (bloquear) {
            sql += " FOR UPDATE";
        }
        return jdbc.query(sql, FILA, acceso, acceso).stream().findFirst();
    }

    public Usuario crear(String nombre, String email, String telefono, String hash, Rol rol) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO usuario (id,nombre,email,telefono,password_hash,rol) VALUES (?,?,?,?,?,?)",
            id, nombre, email, telefono, hash, rol.name());
        return buscar(id).orElseThrow();
    }

    public void acceso(long id, int intentos, LocalDateTime bloqueadoHasta) {
        jdbc.update("UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id = ?",
            intentos, bloqueadoHasta, id);
    }

    public void actualizar(long id, String email, String telefono, Solicitudes.Perfil perfil) {
        jdbc.update("""
            UPDATE usuario SET nombre=?, email=?, telefono=?, dni=?, nacimiento=?, direccion=? WHERE id=?
            """, perfil.nombre().trim(), email, telefono, perfil.dni(),
            java.sql.Date.valueOf(perfil.nacimiento()), perfil.direccion().trim(), id);
    }

    public List<Respuestas.Instructor> instructores() {
        return jdbc.query("SELECT id, nombre FROM usuario WHERE rol = 'INSTRUCTOR' ORDER BY nombre, id",
            (rs, fila) -> new Respuestas.Instructor(rs.getLong("id"), rs.getString("nombre")));
    }
}
