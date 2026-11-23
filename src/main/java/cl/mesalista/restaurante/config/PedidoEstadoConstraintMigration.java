package cl.mesalista.restaurante.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PedidoEstadoConstraintMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public PedidoEstadoConstraintMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE pedidos DROP CONSTRAINT IF EXISTS pedidos_estado_check");
        jdbcTemplate.execute("""
                ALTER TABLE pedidos ADD CONSTRAINT pedidos_estado_check
                CHECK (estado IN ('NUEVO', 'EN_PREPARACION', 'LISTO_PARA_SERVIR', 'ENTREGADO', 'PAGADO'))
                """);
    }
}
