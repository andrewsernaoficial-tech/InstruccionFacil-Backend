package pe.instruccionfacil.repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.instruccionfacil.entity.Modelo.Evento;

@Repository
public class AuditoriaRepository {
    private final JdbcTemplate jdbc;
    private final Identificadores ids;
    private final Clock reloj;

    public AuditoriaRepository(JdbcTemplate jdbc, Identificadores ids, Clock reloj) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.reloj = reloj;
    }

    public void registrar(Long actor, String entidad, long id, String accion, String detalle) {
        jdbc.update("""
            INSERT INTO auditoria (id,actor_id,entidad,entidad_id,accion,detalle,fecha) VALUES (?,?,?,?,?,?,?)
            """, ids.siguiente(), actor, entidad, id, accion, detalle, LocalDateTime.now(reloj));
    }

    public List<Evento> recientes() {
        return jdbc.query("SELECT * FROM auditoria ORDER BY fecha DESC, id DESC FETCH FIRST 200 ROWS ONLY",
            (rs, n) -> new Evento(rs.getLong("id"), rs.getObject("actor_id", Long.class),
                rs.getString("entidad"), rs.getLong("entidad_id"), rs.getString("accion"), rs.getString("detalle"),
                rs.getObject("fecha", LocalDateTime.class)));
    }
}
