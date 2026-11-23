package pe.instruccionfacil.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.entity.Modelo.*;
import pe.instruccionfacil.entity.Transmision;

@Repository
public class CatalogoRepository {
    private static final RowMapper<TipoClase> TIPO = (rs, n) -> new TipoClase(rs.getLong("id"),
        rs.getString("nombre"), rs.getInt("minutos"), rs.getBigDecimal("tarifa"), rs.getInt("activo") == 1);
    private static final RowMapper<Vehiculo> VEHICULO = (rs, n) -> new Vehiculo(rs.getLong("id"),
        rs.getString("placa"), rs.getString("descripcion"), Transmision.valueOf(rs.getString("transmision")),
        rs.getInt("activo") == 1);
    private static final RowMapper<Horario> HORARIO = (rs, n) -> new Horario(rs.getLong("id"),
        rs.getLong("instructor_id"), rs.getObject("inicio", LocalDateTime.class), rs.getObject("fin", LocalDateTime.class));
    private static final RowMapper<Bloqueo> BLOQUEO = (rs, n) -> new Bloqueo(rs.getLong("id"),
        rs.getLong("vehiculo_id"), rs.getObject("inicio", LocalDateTime.class),
        rs.getObject("fin", LocalDateTime.class), rs.getString("motivo"));

    private final JdbcTemplate jdbc;
    private final Identificadores ids;

    public CatalogoRepository(JdbcTemplate jdbc, Identificadores ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    public List<TipoClase> clases(boolean incluirInactivas) {
        String filtro = incluirInactivas ? " WHERE eliminado=0" : " WHERE eliminado=0 AND activo=1";
        return jdbc.query("SELECT * FROM tipo_clase" + filtro + " ORDER BY nombre, id", TIPO);
    }

    public Optional<TipoClase> clase(long id) {
        return jdbc.query("SELECT * FROM tipo_clase WHERE id=? AND eliminado=0", TIPO, id).stream().findFirst();
    }

    public long crearClase(String nombre, int minutos, BigDecimal tarifa, boolean activo) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO tipo_clase (id,nombre,minutos,tarifa,activo) VALUES (?,?,?,?,?)",
            id, nombre, minutos, tarifa, activo ? 1 : 0);
        return id;
    }

    public void editarClase(long id, String nombre, int minutos, BigDecimal tarifa, boolean activo) {
        jdbc.update("UPDATE tipo_clase SET nombre=?, minutos=?, tarifa=?, activo=? WHERE id=?",
            nombre, minutos, tarifa, activo ? 1 : 0, id);
    }

    public boolean borrarClase(long id) {
        int borradas = jdbc.update("""
            DELETE FROM tipo_clase WHERE id=? AND eliminado=0
            AND NOT EXISTS (SELECT 1 FROM cita WHERE tipo_clase_id=?)
            """, id, id);
        if (borradas != 0) return false;
        jdbc.update("UPDATE tipo_clase SET activo=0, eliminado=1 WHERE id=?", id);
        return true;
    }

    public List<Vehiculo> flota() {
        return jdbc.query("SELECT * FROM vehiculo WHERE eliminado=0 ORDER BY placa, id", VEHICULO);
    }

    public Optional<Vehiculo> vehiculo(long id) {
        return jdbc.query("SELECT * FROM vehiculo WHERE id=? AND eliminado=0", VEHICULO, id).stream().findFirst();
    }

    public long crearVehiculo(String placa, String descripcion, Transmision transmision, boolean activo) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO vehiculo (id,placa,descripcion,transmision,activo) VALUES (?,?,?,?,?)",
            id, placa, descripcion, transmision.name(), activo ? 1 : 0);
        return id;
    }

    public void editarVehiculo(long id, String placa, String descripcion, Transmision transmision, boolean activo) {
        jdbc.update("UPDATE vehiculo SET placa=?, descripcion=?, transmision=?, activo=? WHERE id=?",
            placa, descripcion, transmision.name(), activo ? 1 : 0, id);
    }

    public void retirarVehiculo(long id) {
        jdbc.update("UPDATE vehiculo SET activo=0, eliminado=1 WHERE id=?", id);
    }

    public Optional<Horario> horario(long id) {
        return jdbc.query("SELECT * FROM horario_instructor WHERE id=?", HORARIO, id).stream().findFirst();
    }

    public List<Horario> horarios(long instructor, LocalDateTime inicio, LocalDateTime fin) {
        return jdbc.query("""
            SELECT * FROM horario_instructor WHERE instructor_id=? AND inicio < ? AND fin > ? ORDER BY inicio
            """, HORARIO, instructor, fin, inicio);
    }

    public long crearHorario(long instructor, LocalDateTime inicio, LocalDateTime fin) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO horario_instructor (id,instructor_id,inicio,fin) VALUES (?,?,?,?)",
            id, instructor, inicio, fin);
        return id;
    }

    public void borrarHorario(long id) {
        jdbc.update("DELETE FROM horario_instructor WHERE id=?", id);
    }

    public Optional<Bloqueo> bloqueo(long id) {
        return jdbc.query("SELECT * FROM bloqueo_vehiculo WHERE id=?", BLOQUEO, id).stream().findFirst();
    }

    public List<Bloqueo> bloqueos(long vehiculo, LocalDateTime desde) {
        return jdbc.query("SELECT * FROM bloqueo_vehiculo WHERE vehiculo_id=? AND fin > ? ORDER BY inicio",
            BLOQUEO, vehiculo, desde);
    }

    public long crearBloqueo(long vehiculo, LocalDateTime inicio, LocalDateTime fin, String motivo) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO bloqueo_vehiculo (id,vehiculo_id,inicio,fin,motivo) VALUES (?,?,?,?,?)",
            id, vehiculo, inicio, fin, motivo);
        return id;
    }

    public void borrarBloqueo(long id) {
        jdbc.update("DELETE FROM bloqueo_vehiculo WHERE id=?", id);
    }
}
