package pe.instruccionfacil.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReciboCorreoRepository {
    public record Envio(long pagoId, String destinatario, String estado, int intentos) {}
    private final JdbcTemplate jdbc;

    public ReciboCorreoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Envio> buscar(long pagoId) {
        return jdbc.query("SELECT pago_id,destinatario,estado,intentos FROM recibo_correo WHERE pago_id=?",
            (rs, fila) -> new Envio(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4)),
            pagoId).stream().findFirst();
    }

    public void encolar(long pagoId, String email, LocalDateTime ahora) {
        jdbc.update("INSERT INTO recibo_correo (pago_id,destinatario,estado,proximo) VALUES (?,?,'PENDIENTE',?)",
            pagoId, email, ahora);
    }

    public List<Long> pendientes(LocalDateTime ahora) {
        return jdbc.queryForList("""
            SELECT pago_id FROM recibo_correo WHERE estado IN ('PENDIENTE','ENVIANDO')
            AND proximo<=? ORDER BY proximo,pago_id FETCH FIRST 10 ROWS ONLY
            """, Long.class, ahora);
    }

    public boolean reclamar(long pagoId, LocalDateTime ahora) {
        return jdbc.update("""
            UPDATE recibo_correo SET estado='ENVIANDO', intentos=intentos+1, proximo=?
            WHERE pago_id=? AND estado IN ('PENDIENTE','ENVIANDO') AND proximo<=?
            """, ahora.plusMinutes(2), pagoId, ahora) != 0;
    }

    public void enviado(Envio envio, String id) {
        jdbc.update("""
            UPDATE recibo_correo SET estado='ENVIADO', mensaje_externo=?
            WHERE pago_id=? AND intentos=? AND estado='ENVIANDO'
            """, id, envio.pagoId(), envio.intentos());
    }

    public void fallo(Envio envio, LocalDateTime ahora) {
        jdbc.update("""
            UPDATE recibo_correo SET estado=?, proximo=?
            WHERE pago_id=? AND intentos=? AND estado='ENVIANDO'
            """, envio.intentos() >= 5 ? "FALLIDO" : "PENDIENTE", ahora.plusMinutes(2L * envio.intentos()),
            envio.pagoId(), envio.intentos());
    }

    public void reintentar(long pagoId, String email, LocalDateTime ahora) {
        jdbc.update("""
            UPDATE recibo_correo SET estado='PENDIENTE', destinatario=?, intentos=0, proximo=?
            WHERE pago_id=? AND estado='FALLIDO'
            """, email, ahora, pagoId);
    }
}
