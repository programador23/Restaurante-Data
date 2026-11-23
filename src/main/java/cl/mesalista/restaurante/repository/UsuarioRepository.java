package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.Usuario;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByIdUsuarioAndRestaurante_IdRestaurante(Long idUsuario, Long idRestaurante);
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);
    List<Usuario> findByRestaurante_IdRestaurante(Long idRestaurante);
    @Query("select u from Usuario u left join fetch u.restaurante where u.idUsuario = :idUsuario")
    Optional<Usuario> findWithRestauranteById(@Param("idUsuario") Long idUsuario);
    void deleteAllByRestaurante_IdRestaurante(Long idRestaurante);
}
