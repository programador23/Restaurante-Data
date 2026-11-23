package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.Caja;
import cl.mesalista.restaurante.model.EstadoCaja;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    Optional<Caja> findByRestaurante_IdRestauranteAndEstado(Long idRestaurante, EstadoCaja estado);
}
