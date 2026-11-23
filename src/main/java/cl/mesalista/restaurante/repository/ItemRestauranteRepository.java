package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.ItemRestaurante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRestauranteRepository extends JpaRepository<ItemRestaurante, Long> {
    List<ItemRestaurante> findByMenu_IdMenu(Long idMenu);
}
