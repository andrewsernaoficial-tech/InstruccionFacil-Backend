package pe.instruccionfacil.repository;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.entity.Modelo.*;

@Repository
public class PagoRepository {
    private static final String COLUMNAS = """
        p.id,p.cita_id,p.medio,p.operacion,p.monto,p.estado,p.creado,p.revisado,p.revisor_id,p.motivo
        """;
    private static final RowMapper<Pago> FILA = (rs, n) -> new Pago(rs.getLong("id"), rs.getLong("cita_id"),
        MedioPago.valueOf(rs.getString("medio")), rs.getString("operacion"), rs.getBigDecimal("monto"),
        EstadoPago.valueOf(rs.getString("estado")), rs.getObject("creado", LocalDateTime.class),
        rs.getObject("revisado", LocalDateTime.class), rs.getObject("revisor_id", Long.class), rs.getString("motivo"));

    private final JdbcTemplate jdbc;
    private final Identificadores ids;

    public PagoRepository(JdbcTemplate jdbc, Identificadores ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    public Optional<Pago> buscar(long id) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM pago p WHERE p.id=?", FILA, id).stream().findFirst();
    }

    public List<Pago> deCita(long cita) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM pago p WHERE p.cita_id=? ORDER BY p.creado, p.id", FILA, cita);
    }

    public List<Pago> pendientes(LocalDateTime ahora) {
        return jdbc.query("SELECT " + COLUMNAS + """
            FROM pago p JOIN cita c ON c.id=p.cita_id
            WHERE p.estado='EN_REVISION' AND c.estado='PENDIENTE_PAGO' AND c.vence > ?
            ORDER BY p.creado,p.id FETCH FIRST 200 ROWS ONLY
            """, FILA, ahora);
    }

    public Optional<Comprobante> comprobante(long id) {
        return jdbc.query("SELECT contenido,mime FROM pago WHERE id=?",
            (rs, n) -> new Comprobante(rs.getBytes("contenido"), rs.getString("mime")), id).stream().findFirst();
    }

    public boolean hayPagoEnRevision(long cita) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pago WHERE cita_id=? AND estado='EN_REVISION'",
            Long.class, cita) > 0;
    }

    public Pago crear(Cita cita, MedioPago medio, String operacion, Comprobante archivo, LocalDateTime ahora) {
        long id = ids.siguiente();
        jdbc.update(conn -> {
            var ps = conn.prepareStatement("""
                INSERT INTO pago (id,cita_id,medio,operacion,monto,contenido,mime,estado,creado)
                VALUES (?,?,?,?,?,?,?,'EN_REVISION',?)
                """);
            ps.setLong(1, id);
            ps.setLong(2, cita.id());
            ps.setString(3, medio.name());
            ps.setString(4, operacion);
            ps.setBigDecimal(5, cita.monto());
            ps.setBinaryStream(6, new ByteArrayInputStream(archivo.contenido()), archivo.contenido().length);
            ps.setString(7, archivo.mime());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(ahora));
            return ps;
        });
        return buscar(id).orElseThrow();
    }

    public void revisar(long id, EstadoPago estado, long revisor, String motivo, LocalDateTime ahora) {
        jdbc.update("UPDATE pago SET estado=?, revisor_id=?, motivo=?, revisado=? WHERE id=?",
            estado.name(), revisor, motivo, ahora, id);
    }

    public void cerrarPendientes(long cita, Long revisor, String motivo, LocalDateTime ahora) {
        jdbc.update("""
            UPDATE pago SET estado='RECHAZADO', revisor_id=?, motivo=?, revisado=?
            WHERE cita_id=? AND estado='EN_REVISION'
            """, revisor, motivo, ahora, cita);
    }
}
