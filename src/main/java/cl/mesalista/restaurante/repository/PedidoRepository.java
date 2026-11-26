package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.model.Pedido;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByRestaurante_IdRestaurante(Long idRestaurante);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pedido> findFirstByRestaurante_IdRestauranteAndEstadoOrderByCreadoEnAscIdPedidoAsc(
            Long idRestaurante, EstadoPedido estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pedido> findByIdPedidoAndRestaurante_IdRestaurante(Long idPedido, Long idRestaurante);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Pedido> findByRestaurante_IdRestauranteAndNumeroMesaAndEstado(
            Long idRestaurante, int numeroMesa, EstadoPedido estado);
    void deleteAllByRestaurante_IdRestaurante(Long idRestaurante);

    @Modifying
    @Query("update Pedido p set p.garzon = null where p.garzon.idUsuario = :idUsuario")
    void desvincularGarzonDePedidos(@Param("idUsuario") Long idUsuario);
}
