package pe.instruccionfacil.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.entity.Rol;

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
        String sql = "SELECT * FROM usuario WHERE eliminado=0 AND (email = ? OR telefono = ?)";
        if (bloquear) {
            sql += " FOR UPDATE";
        }
        return jdbc.query(sql, FILA, acceso, acceso).stream().findFirst();
    }

    public boolean estaHabilitado(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE id=? AND eliminado=0", Long.class, id) > 0;
    }

    public List<Usuario> personal() {
        return jdbc.query("""
            SELECT * FROM usuario WHERE eliminado=0 AND rol IN ('INSTRUCTOR','RECEPCION') ORDER BY nombre,id
            """, FILA);
    }

    public Optional<Usuario> personalParaEditar(long id) {
        return jdbc.query("""
            SELECT * FROM usuario WHERE id=? AND eliminado=0 AND rol IN ('INSTRUCTOR','RECEPCION') FOR UPDATE
            """, FILA, id).stream().findFirst();
    }

    public void eliminarPersonal(long id) {
        jdbc.update("UPDATE usuario SET eliminado=1 WHERE id=?", id);
    }

    public Usuario crear(String nombre, String email, String telefono, String hash, Rol rol) {
        return crear(nombre, email, telefono, hash, rol, null, null, null);
    }

    public Usuario crear(String nombre, String email, String telefono, String hash, Rol rol,
                         String dni, LocalDate nacimiento, String direccion) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO usuario (id,nombre,email,telefono,password_hash,rol,dni,nacimiento,direccion) VALUES (?,?,?,?,?,?,?,?,?)",
            id, nombre, email, telefono, hash, rol.name(), dni, nacimiento, direccion);
        return buscar(id).orElseThrow();
    }

    public void acceso(long id, int intentos, LocalDateTime bloqueadoHasta) {
        jdbc.update("UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id = ?",
            intentos, bloqueadoHasta, id);
    }

    public Usuario crearCliente(String nombre, String email, String telefono, String dni,
                                LocalDate nacimiento, String direccion, String hash) {
        long id = ids.siguiente();
        jdbc.update("""
            INSERT INTO usuario (id, nombre, email, telefono, dni, nacimiento, direccion, password_hash, rol)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CLIENTE')
            """, id, nombre, email, telefono, dni, java.sql.Date.valueOf(nacimiento), direccion, hash);
        return buscar(id).orElseThrow();
    }

    public void restablecerClaveAdmin(long id, String hash) {
        jdbc.update("""
            UPDATE usuario SET password_hash = ?, intentos_fallidos = 0, bloqueado_hasta = NULL
            WHERE id = ? AND rol = 'ADMIN'
            """, hash, id);
    }

    public void actualizar(long id, String nombre, String email, String telefono, String dni,
                           LocalDate nacimiento, String direccion) {
        jdbc.update("""
            UPDATE usuario SET nombre=?, email=?, telefono=?, dni=?, nacimiento=?, direccion=? WHERE id=?
            """, nombre, email, telefono, dni, nacimiento == null ? null : java.sql.Date.valueOf(nacimiento), direccion, id);
    }

    public List<Instructor> instructores() {
        return jdbc.query("SELECT id, nombre FROM usuario WHERE rol = 'INSTRUCTOR' AND eliminado=0 ORDER BY nombre, id",
            (rs, fila) -> new Instructor(rs.getLong("id"), rs.getString("nombre")));
    }
}
