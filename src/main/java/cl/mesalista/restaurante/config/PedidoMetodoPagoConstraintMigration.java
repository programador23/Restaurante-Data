package cl.mesalista.restaurante.config;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PedidoMetodoPagoConstraintMigration implements ApplicationRunner {
    private static final Pattern H2_ENUM_CHECK = Pattern.compile(
            "^\\(*\\s*\"?METODO_PAGO\"?\\s+IN\\s*\\((?:\\s*'(?:EFECTIVO|DEBITO|CREDITO|TARJETA)'\\s*,?)+\\)\\s*\\)*$");
    private final JdbcTemplate jdbcTemplate;

    public PedidoMetodoPagoConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var checks = jdbcTemplate.query("""
                SELECT tc.constraint_name, cc.check_clause
                FROM information_schema.table_constraints tc
                JOIN information_schema.check_constraints cc
                  ON tc.constraint_catalog = cc.constraint_catalog
                 AND tc.constraint_schema = cc.constraint_schema
                 AND tc.constraint_name = cc.constraint_name
                WHERE LOWER(tc.table_schema) = LOWER(CURRENT_SCHEMA())
                  AND LOWER(tc.table_name) = 'pedidos'
                  AND tc.constraint_type = 'CHECK'
                """, (rs, row) -> new Check(rs.getString(1), rs.getString(2)));
        for (Check check : checks) {
            String name = check.name().toLowerCase(Locale.ROOT);
            String clause = check.clause().toUpperCase(Locale.ROOT);
            if (name.equals("pedidos_metodo_pago_check")
                    || (name.startsWith("constraint_") && H2_ENUM_CHECK.matcher(clause).matches()
                    && clause.contains("'EFECTIVO'") && clause.contains("'DEBITO'") && clause.contains("'CREDITO'"))) {
                jdbcTemplate.execute("ALTER TABLE pedidos DROP CONSTRAINT \"" + check.name().replace("\"", "\"\"") + "\"");
            }
        }
        jdbcTemplate.execute("""
                ALTER TABLE pedidos ALTER COLUMN metodo_pago SET DATA TYPE VARCHAR(20)
                """);
        jdbcTemplate.execute("""
                ALTER TABLE pedidos ADD CONSTRAINT pedidos_metodo_pago_check
                CHECK (metodo_pago IN ('EFECTIVO', 'DEBITO', 'CREDITO', 'TARJETA'))
                """);
    }

    private record Check(String name, String clause) {
    }
}
