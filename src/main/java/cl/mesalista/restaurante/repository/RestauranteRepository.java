package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.Restaurante;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    Optional<Restaurante> findByNombreIgnoreCase(String nombre);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Restaurante> findByIdRestaurante(Long idRestaurante);
    List<Restaurante> findByNombreContainingIgnoreCaseOrCategoriaContainingIgnoreCase(
            String nombre, String categoria);
}
