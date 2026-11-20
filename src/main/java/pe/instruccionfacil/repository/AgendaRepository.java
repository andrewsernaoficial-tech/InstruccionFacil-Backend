package pe.instruccionfacil.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.entity.Modelo.*;

@Repository
public class AgendaRepository {
    private static final String OCUPA = """
        (c.estado IN ('CONFIRMADA','EN_CURSO') OR (c.estado='PENDIENTE_PAGO' AND c.vence > ?))
        """;
    private static final RowMapper<Cita> FILA = (rs, n) -> new Cita(rs.getLong("id"), rs.getLong("alumno_id"),
        rs.getLong("instructor_id"), rs.getLong("tipo_clase_id"), rs.getObject("vehiculo_id", Long.class),
        rs.getString("placa_propia"), rs.getString("marca_propia"), rs.getString("modelo_propio"),
        Transmision.valueOf(rs.getString("transmision")), Canal.valueOf(rs.getString("canal")),
        rs.getObject("inicio", LocalDateTime.class), rs.getObject("fin", LocalDateTime.class),
        rs.getObject("vence", LocalDateTime.class), rs.getBigDecimal("monto"),
        EstadoCita.valueOf(rs.getString("estado")), rs.getString("motivo"));

    private final JdbcTemplate jdbc;
    private final Identificadores ids;

    public AgendaRepository(JdbcTemplate jdbc, Identificadores ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    public void bloquearCambios() {
        jdbc.queryForObject("SELECT id FROM agenda_control WHERE id=1 FOR UPDATE", Integer.class);
    }

    public Optional<Cita> buscar(long id) {
        return jdbc.query("SELECT * FROM cita WHERE id=?", FILA, id).stream().findFirst();
    }

    public List<Cita> listar(Usuario usuario, LocalDateTime inicio, LocalDateTime fin) {
        return switch (usuario.rol()) {
            case CLIENTE -> jdbc.query("""
                SELECT * FROM cita WHERE alumno_id=? AND inicio >= ? AND inicio < ? ORDER BY inicio, id
                """, FILA, usuario.id(), inicio, fin);
            case INSTRUCTOR -> jdbc.query("""
                SELECT * FROM cita WHERE instructor_id=? AND inicio >= ? AND inicio < ? ORDER BY inicio, id
                """, FILA, usuario.id(), inicio, fin);
            case ADMIN, RECEPCION -> jdbc.query("""
                SELECT * FROM cita WHERE inicio >= ? AND inicio < ? ORDER BY inicio, id
                """, FILA, inicio, fin);
        };
    }

    public boolean instructorOcupado(long instructor, LocalDateTime inicio, LocalDateTime fin,
                                     LocalDateTime ahora, long excluirCita) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM cita c WHERE c.instructor_id=? AND c.id <> ? AND c.inicio < ? AND c.fin > ? AND
            """ + OCUPA, Long.class, instructor, excluirCita, fin, inicio, ahora) > 0;
    }

    public boolean alumnoOcupado(long alumno, LocalDateTime inicio, LocalDateTime fin,
                                 LocalDateTime ahora, long excluirCita) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM cita c WHERE c.alumno_id=? AND c.id <> ? AND c.inicio < ? AND c.fin > ? AND
            """ + OCUPA, Long.class, alumno, excluirCita, fin, inicio, ahora) > 0;
    }

    public boolean placaPropiaOcupada(String placa, LocalDateTime inicio, LocalDateTime fin,
                                      LocalDateTime ahora, long excluirCita) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM cita c WHERE c.placa_propia=? AND c.id <> ? AND c.inicio < ? AND c.fin > ? AND
            """ + OCUPA, Long.class, placa, excluirCita, fin, inicio, ahora) > 0;
    }

    public boolean vehiculoOcupado(long vehiculo, LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM cita c WHERE c.vehiculo_id=? AND c.inicio < ? AND c.fin > ? AND
            """ + OCUPA, Long.class, vehiculo, fin, inicio, ahora) > 0;
    }

    public boolean vehiculoTieneCitas(long vehiculo, LocalDateTime ahora) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM cita c WHERE c.vehiculo_id=? AND c.fin > ? AND " + OCUPA,
            Long.class, vehiculo, ahora, ahora) > 0;
    }

    public Optional<Long> vehiculoLibre(Transmision transmision, LocalDateTime inicio,
                                        LocalDateTime fin, LocalDateTime ahora, long excluirCita) {
        return jdbc.query("""
            SELECT v.id FROM vehiculo v WHERE v.activo=1 AND v.transmision=?
              AND NOT EXISTS (
                SELECT 1 FROM bloqueo_vehiculo b WHERE b.vehiculo_id=v.id AND b.inicio < ? AND b.fin > ?
              )
              AND NOT EXISTS (
                SELECT 1 FROM cita c WHERE c.vehiculo_id=v.id AND c.id <> ? AND c.inicio < ? AND c.fin > ? AND
            """ + OCUPA + """
              )
            ORDER BY v.id FETCH FIRST 1 ROWS ONLY
            """, (rs, n) -> rs.getLong(1), transmision.name(), fin, inicio, excluirCita, fin, inicio, ahora)
            .stream().findFirst();
    }

    public Cita crear(long alumno, long instructor, TipoClase clase, Long vehiculo, String placaPropia,
                      String marca, String modelo, Transmision transmision, Canal canal, LocalDateTime inicio,
                      LocalDateTime fin, LocalDateTime vence) {
        long id = ids.siguiente();
        jdbc.update("""
            INSERT INTO cita (id,alumno_id,instructor_id,tipo_clase_id,vehiculo_id,placa_propia,
              marca_propia,modelo_propio,transmision,canal,inicio,fin,vence,monto,estado)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PENDIENTE_PAGO')
            """, id, alumno, instructor, clase.id(), vehiculo, placaPropia, marca, modelo, transmision.name(),
            canal.name(), inicio, fin, vence, clase.tarifa());
        return buscar(id).orElseThrow();
    }

    public void reprogramar(long id, long instructor, Long vehiculo, LocalDateTime inicio, LocalDateTime fin) {
        jdbc.update("UPDATE cita SET instructor_id=?, vehiculo_id=?, inicio=?, fin=? WHERE id=?",
            instructor, vehiculo, inicio, fin, id);
    }

    public List<Long> vencidas(LocalDateTime ahora) {
        return jdbc.query("SELECT id FROM cita WHERE estado='PENDIENTE_PAGO' AND vence <= ? ORDER BY id",
            (rs, n) -> rs.getLong(1), ahora);
    }

    public void cambiarEstado(long id, EstadoCita nuevo, String motivo) {
        jdbc.update("UPDATE cita SET estado=?, motivo=? WHERE id=?", nuevo.name(), motivo, id);
    }
}
