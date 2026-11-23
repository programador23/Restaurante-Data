package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.Restaurante;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    Optional<Restaurante> findByNombreIgnoreCase(String nombre);
    List<Restaurante> findByNombreContainingIgnoreCaseOrCategoriaContainingIgnoreCase(
            String nombre, String categoria);
}
