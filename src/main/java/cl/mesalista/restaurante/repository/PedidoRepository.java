package cl.mesalista.restaurante.repository;

import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.model.MetodoPago;
import cl.mesalista.restaurante.model.Pedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    List<Pedido> findByRestaurante_IdRestaurante(Long idRestaurante);

    @Override
    @EntityGraph(attributePaths = {"items", "garzon"})
    List<Pedido> findAll(Specification<Pedido> spec, Sort sort);

    @Query("select sum(i.valorUnitario * i.cantidad) from PedidoItem i join i.pedido p "
            + "where p.restaurante.idRestaurante = :idRestaurante and p.estado = :estado "
            + "and p.metodoPago = :metodoPago and p.pagadoEn >= :desde and p.pagadoEn <= :hasta")
    BigDecimal sumarCobrado(@Param("idRestaurante") Long idRestaurante,
                            @Param("estado") EstadoPedido estado,
                            @Param("metodoPago") MetodoPago metodoPago,
                            @Param("desde") Instant desde,
                            @Param("hasta") Instant hasta);

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
