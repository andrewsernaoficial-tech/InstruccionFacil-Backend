package pe.instruccionfacil.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutRepository {
    public record Checkout(long citaId, String referencia, String ordenExterna, String enlace,
                           String estado, LocalDateTime creado, LocalDateTime vence,
                           String incidencia, LocalDateTime actualizado) {
        public boolean abierto(LocalDateTime ahora) {
            return vence.isAfter(ahora) && (estado.equals("ABIERTO") || estado.equals("EN_PROCESO"));
        }
    }

    private static final RowMapper<Checkout> FILA = (rs, fila) -> new Checkout(rs.getLong("cita_id"),
        rs.getString("referencia"), rs.getString("orden_externa"), rs.getString("enlace"), rs.getString("estado"),
        rs.getObject("creado", LocalDateTime.class), rs.getObject("vence", LocalDateTime.class),
        rs.getString("incidencia"), rs.getObject("actualizado", LocalDateTime.class));

    private final JdbcTemplate jdbc;

    public CheckoutRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Checkout> porCita(long citaId, boolean bloquear) {
        String sql = "SELECT * FROM pago_checkout WHERE cita_id=?";
        return jdbc.query(sql + (bloquear ? " FOR UPDATE" : ""), FILA, citaId).stream().findFirst();
    }

    public Optional<Checkout> porReferencia(String referencia) {
        return jdbc.query("SELECT * FROM pago_checkout WHERE referencia=?", FILA, referencia).stream().findFirst();
    }

    public void crear(long citaId, String referencia, LocalDateTime creado, LocalDateTime vence) {
        jdbc.update("INSERT INTO pago_checkout (cita_id, referencia, estado, creado, vence) VALUES (?,?,'ABIERTO',?,?)",
            citaId, referencia, creado, vence);
    }

    public void guardarOrden(long citaId, String orden, String enlace) {
        jdbc.update("UPDATE pago_checkout SET orden_externa=?, enlace=? WHERE cita_id=?", orden, enlace, citaId);
    }

    public void actualizar(long citaId, String estado, String incidencia, LocalDateTime ahora) {
        jdbc.update("UPDATE pago_checkout SET estado=?, incidencia=?, actualizado=? WHERE cita_id=?", estado, incidencia, ahora, citaId);
    }

    public List<Checkout> incidencias() {
        return jdbc.query("""
            SELECT * FROM pago_checkout WHERE incidencia IS NOT NULL
            ORDER BY actualizado DESC, cita_id DESC FETCH FIRST 30 ROWS ONLY
            """, FILA);
    }
}
