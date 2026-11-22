package db.migration;

import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__medios_y_efectivo extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws SQLException {
        var connection = context.getConnection();
        boolean oracle = connection.getMetaData().getDatabaseProductName().equals("Oracle");
        String consulta = oracle ? """
            SELECT constraint_name FROM user_constraints
            WHERE table_name='PAGO' AND constraint_type='C' AND search_condition_vc LIKE '%TRANSFERENCIA%'
            """ : """
            SELECT t.constraint_name FROM information_schema.table_constraints t
            JOIN information_schema.check_constraints c
            ON c.constraint_schema=t.constraint_schema AND c.constraint_name=t.constraint_name
            WHERE t.table_schema=CURRENT_SCHEMA AND t.table_name='PAGO' AND c.check_clause LIKE '%TRANSFERENCIA%'
            """;
        try (var sql = connection.createStatement()) {
            String nombre;
            try (var rows = sql.executeQuery(consulta)) {
                if (!rows.next()) throw new SQLException("No se encontró la restricción del medio de pago");
                nombre = rows.getString(1).replace("\"", "\"\"");
            }
            sql.execute("ALTER TABLE pago DROP CONSTRAINT \"" + nombre + "\"");
            sql.execute("ALTER TABLE pago ADD CONSTRAINT pago_medio_ck CHECK (medio IN ('YAPE','PLIN','TRANSFERENCIA','EFECTIVO'))");
            if (oracle) {
                sql.execute("ALTER TABLE pago MODIFY (contenido NULL, mime NULL)");
            } else {
                sql.execute("ALTER TABLE pago ALTER COLUMN contenido DROP NOT NULL");
                sql.execute("ALTER TABLE pago ALTER COLUMN mime DROP NOT NULL");
            }
            sql.execute("""
                CREATE TABLE configuracion_yape (
                    id NUMBER(1) PRIMARY KEY CHECK (id=1),
                    numero VARCHAR2(9 CHAR),
                    qr BLOB,
                    qr_mime VARCHAR2(40 CHAR)
                )
                """);
            sql.execute("INSERT INTO configuracion_yape (id) VALUES (1)");
        }
    }
}
