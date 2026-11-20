package pe.instruccionfacil.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.dto.Solicitudes;
import pe.instruccionfacil.entity.Modelo.*;

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
        String filtro = incluirInactivas ? "" : " WHERE activo=1";
        return jdbc.query("SELECT * FROM tipo_clase" + filtro + " ORDER BY nombre, id", TIPO);
    }

    public Optional<TipoClase> clase(long id) {
        return jdbc.query("SELECT * FROM tipo_clase WHERE id=?", TIPO, id).stream().findFirst();
    }

    public long crearClase(Solicitudes.Tipo datos) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO tipo_clase (id,nombre,minutos,tarifa,activo) VALUES (?,?,?,?,?)",
            id, datos.nombre().trim(), datos.minutos(), datos.tarifa(), datos.activo() ? 1 : 0);
        return id;
    }

    public void editarClase(long id, Solicitudes.Tipo datos) {
        jdbc.update("UPDATE tipo_clase SET nombre=?, minutos=?, tarifa=?, activo=? WHERE id=?",
            datos.nombre().trim(), datos.minutos(), datos.tarifa(), datos.activo() ? 1 : 0, id);
    }

    public List<Vehiculo> flota() {
        return jdbc.query("SELECT * FROM vehiculo ORDER BY placa, id", VEHICULO);
    }

    public Optional<Vehiculo> vehiculo(long id) {
        return jdbc.query("SELECT * FROM vehiculo WHERE id=?", VEHICULO, id).stream().findFirst();
    }

    public long crearVehiculo(String placa, Solicitudes.Flota datos) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO vehiculo (id,placa,descripcion,transmision,activo) VALUES (?,?,?,?,?)",
            id, placa, datos.descripcion().trim(), datos.transmision().name(), datos.activo() ? 1 : 0);
        return id;
    }

    public void editarVehiculo(long id, String placa, Solicitudes.Flota datos) {
        jdbc.update("UPDATE vehiculo SET placa=?, descripcion=?, transmision=?, activo=? WHERE id=?",
            placa, datos.descripcion().trim(), datos.transmision().name(), datos.activo() ? 1 : 0, id);
    }

    public Optional<Horario> horario(long id) {
        return jdbc.query("SELECT * FROM horario_instructor WHERE id=?", HORARIO, id).stream().findFirst();
    }

    public List<Horario> horarios(long instructor, LocalDateTime inicio, LocalDateTime fin) {
        return jdbc.query("""
            SELECT * FROM horario_instructor WHERE instructor_id=? AND inicio < ? AND fin > ? ORDER BY inicio
            """, HORARIO, instructor, fin, inicio);
    }

    public boolean cubreHorario(long instructor, LocalDateTime inicio, LocalDateTime fin) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM horario_instructor WHERE instructor_id=? AND inicio <= ? AND fin >= ?
            """, Long.class, instructor, inicio, fin) > 0;
    }

    public long crearHorario(long instructor, Solicitudes.Ventana datos) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO horario_instructor (id,instructor_id,inicio,fin) VALUES (?,?,?,?)",
            id, instructor, datos.inicio(), datos.fin());
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

    public long crearBloqueo(long vehiculo, Solicitudes.BloqueoVehiculo datos) {
        long id = ids.siguiente();
        jdbc.update("INSERT INTO bloqueo_vehiculo (id,vehiculo_id,inicio,fin,motivo) VALUES (?,?,?,?,?)",
            id, vehiculo, datos.inicio(), datos.fin(), datos.motivo().trim());
        return id;
    }

    public void borrarBloqueo(long id) {
        jdbc.update("DELETE FROM bloqueo_vehiculo WHERE id=?", id);
    }
}
