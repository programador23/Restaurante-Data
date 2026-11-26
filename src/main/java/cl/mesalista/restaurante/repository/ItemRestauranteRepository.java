package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.ItemRestaurante;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRestauranteRepository extends JpaRepository<ItemRestaurante, Long> {
    List<ItemRestaurante> findByMenu_IdMenu(Long idMenu);

    @Query("select i from ItemRestaurante i join fetch i.menu where i.idItemRestaurante in :ids")
    List<ItemRestaurante> findAllConMenu(@Param("ids") Collection<Long> ids);
}
