package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByRestaurante_IdRestaurante(Long idRestaurante);
    boolean existsByRestaurante_IdRestaurante(Long idRestaurante);
}
