package cl.mesalista.restaurante.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class PedidoMetodoPagoConstraintMigrationTest {
    @Test
    void migraRestriccionH2ExistenteSinPerderPagosNiOtrasRestricciones() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        verificarMigracion(new JdbcTemplate(source), "",
                "ENUM('EFECTIVO', 'DEBITO', 'CREDITO')");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_PAYMENT_POSTGRES_URL", matches = ".+")
    void migraRestriccionPostgresExistenteSinPerderPagosNiOtrasRestricciones() {
        var source = new DriverManagerDataSource(System.getenv("TEST_PAYMENT_POSTGRES_URL"),
                System.getenv("TEST_PAYMENT_POSTGRES_USER"), System.getenv("TEST_PAYMENT_POSTGRES_PASSWORD"));
        verificarMigracion(new JdbcTemplate(source), "CONSTRAINT pedidos_metodo_pago_check ", "VARCHAR(20)");
    }

    private void verificarMigracion(JdbcTemplate jdbc, String nombreRestriccion, String tipoMetodoPago) {
        jdbc.execute("""
                CREATE TABLE pedidos (
                    id BIGINT CHECK (id > 0),
                    metodo_pago %s %sCHECK (metodo_pago IN ('EFECTIVO', 'DEBITO', 'CREDITO'))
                )
                """.formatted(tipoMetodoPago, nombreRestriccion));
        jdbc.update("INSERT INTO pedidos VALUES (1, 'DEBITO'), (2, 'CREDITO')");
        var migration = new PedidoMetodoPagoConstraintMigration(jdbc);

        migration.run(null);
        migration.run(null);

        jdbc.update("INSERT INTO pedidos VALUES (3, 'TARJETA'), (4, 'EFECTIVO')");
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO pedidos VALUES (-1, 'TARJETA')"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO pedidos VALUES (5, 'OTRO')"));
    }
}
