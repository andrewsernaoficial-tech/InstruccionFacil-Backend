package pe.instruccionfacil.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Identificadores {
    private final JdbcTemplate jdbc;

    public Identificadores(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long siguiente() {
        return jdbc.queryForObject("SELECT app_seq.NEXTVAL FROM dual", Long.class);
    }
}
