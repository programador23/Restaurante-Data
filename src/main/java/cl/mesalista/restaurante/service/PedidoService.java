package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoItemRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoItemResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.PagoMesaRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PagoMesaResponse;
import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.model.ItemRestaurante;
import cl.mesalista.restaurante.model.Pedido;
import cl.mesalista.restaurante.model.PedidoItem;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.model.Rol;
import cl.mesalista.restaurante.model.Usuario;
import cl.mesalista.restaurante.repository.ItemRestauranteRepository;
import cl.mesalista.restaurante.repository.PedidoRepository;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final RestauranteRepository restaurantes;
    private final UsuarioRepository usuarios;
    private final ItemRestauranteRepository items;
    private final TenantAccessService access;

    public PedidoService(PedidoRepository pedidos, RestauranteRepository restaurantes,
                         UsuarioRepository usuarios, ItemRestauranteRepository items,
                         TenantAccessService access) {
        this.pedidos = pedidos;
        this.restaurantes = restaurantes;
        this.usuarios = usuarios;
        this.items = items;
        this.access = access;
    }

    @Transactional
    public PedidoResponse crear(Long idRestaurante, PedidoRequest request) {
        access.requireRestaurantAccess(idRestaurante);
        Restaurante restaurante = restaurante(idRestaurante);
        Pedido pedido = new Pedido(restaurante, request.numeroMesa(),
                garzon(idRestaurante, request.idGarzon()), request.descripcion(), request.estado());
        pedido.reemplazarItems(crearItems(idRestaurante, request.items()));
        return response(pedidos.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> buscar(Long idRestaurante, EstadoPedido estado, Integer numeroMesa) {
        access.requireRestaurantAccess(idRestaurante);
        restaurante(idRestaurante);
        return pedidos.findByRestaurante_IdRestaurante(idRestaurante).stream()
                .filter(pedido -> estado == null || pedido.getEstado() == estado)
                .filter(pedido -> numeroMesa == null || pedido.getNumeroMesa() == numeroMesa)
                .map(PedidoService::response).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long idRestaurante, Long idPedido) {
        return response(pedido(idRestaurante, idPedido));
    }

    @Transactional
    public PedidoResponse actualizar(Long idRestaurante, Long idPedido, PedidoRequest request) {
        Pedido pedido = pedido(idRestaurante, idPedido);
        pedido.actualizar(request.numeroMesa(), garzon(idRestaurante, request.idGarzon()),
                request.descripcion(), request.estado());
        pedido.reemplazarItems(crearItems(idRestaurante, request.items()));
        return response(pedidos.save(pedido));
    }

    @Transactional
    public PagoMesaResponse pagarMesa(Long idRestaurante, int numeroMesa, PagoMesaRequest request) {
        access.requireRestaurantAccess(idRestaurante);
        restaurante(idRestaurante);
        if (numeroMesa <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de mesa debe ser positivo.");
        }

        Set<Long> idPedidosSolicitados = new HashSet<>(request.idPedidos());
        if (idPedidosSolicitados.size() != request.idPedidos().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La lista de pedidos de la mesa contiene IDs duplicados.");
        }

        List<Pedido> pendientes = pedidos.findByRestaurante_IdRestauranteAndNumeroMesaAndEstado(
                idRestaurante, numeroMesa, EstadoPedido.ENTREGADO);
        Set<Long> idPedidosPendientes = pendientes.stream()
                .map(Pedido::getIdPedido)
                .collect(Collectors.toSet());
        if (pendientes.isEmpty() || !idPedidosPendientes.equals(idPedidosSolicitados)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los pedidos pendientes de pago de la mesa cambiaron. Actualiza la lista antes de cobrar.");
        }

        BigDecimal total = pendientes.stream()
                .map(PedidoService::response)
                .map(PedidoResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(request.totalConfirmado()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El total de la mesa cambió. Actualiza la lista antes de cobrar.");
        }

        pendientes.forEach(pedido -> pedido.marcarPagado(request.metodoPago()));
        List<PedidoResponse> respuestas = pedidos.saveAll(pendientes).stream()
                .map(PedidoService::response)
                .toList();
        return new PagoMesaResponse(numeroMesa, respuestas, total);
    }

    @Transactional
    public void eliminar(Long idRestaurante, Long idPedido) {
        pedidos.delete(pedido(idRestaurante, idPedido));
    }

    private List<PedidoItem> crearItems(Long idRestaurante, List<PedidoItemRequest> requestItems) {
        Set<Long> usados = new HashSet<>();
        return requestItems.stream().map(linea -> {
            if (!usados.add(linea.idItemRestaurante())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un producto no puede repetirse; aumenta su cantidad.");
            }
            ItemRestaurante item = items.findById(linea.idItemRestaurante()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
            if (!item.getMenu().getRestaurante().getIdRestaurante().equals(idRestaurante)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Todos los productos del pedido deben pertenecer al restaurante.");
            }
            if (!item.isDisponible()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto " + item.getNombre() + " no está disponible.");
            }
            return new PedidoItem(item, linea.cantidad());
        }).toList();
    }

    private Pedido pedido(Long idRestaurante, Long idPedido) {
        Pedido pedido = pedidos.findById(idPedido)
                .filter(found -> found.getRestaurante().getIdRestaurante().equals(idRestaurante))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        access.requireRestaurantAccess(idRestaurante);
        return pedido;
    }

    private Restaurante restaurante(Long id) {
        return restaurantes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private Usuario garzon(Long idRestaurante, Long idGarzon) {
        if (access.role() == Rol.GARZON) {
            if (idGarzon == null) {
                idGarzon = access.userId();
            } else if (!idGarzon.equals(access.userId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Un Garzón solo puede asignarse a sus propios pedidos.");
            }
        }
        if (idGarzon == null) {
            return null;
        }
        Usuario usuario = usuarios.findByIdUsuarioAndRestaurante_IdRestaurante(idGarzon, idRestaurante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El garzón no pertenece al restaurante."));
        if (usuario.getRol() != Rol.GARZON) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario indicado no tiene el rol Garzón.");
        }
        return usuario;
    }

    private static PedidoResponse response(Pedido pedido) {
        List<PedidoItemResponse> items = pedido.getItems().stream().map(linea -> {
            BigDecimal subtotal = linea.getValorUnitario().multiply(BigDecimal.valueOf(linea.getCantidad()));
            return new PedidoItemResponse(linea.getItemRestaurante().getIdItemRestaurante(),
                    linea.getNombreItem(), linea.getCantidad(), linea.getValorUnitario(), subtotal);
        }).toList();
        BigDecimal total = items.stream().map(PedidoItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Usuario garzon = pedido.getGarzon();
        String nombreGarzon = garzon == null ? null : garzon.getNombre() + " " + garzon.getApellido();
        return new PedidoResponse(pedido.getIdPedido(), pedido.getRestaurante().getIdRestaurante(),
                pedido.getNumeroMesa(), garzon == null ? null : garzon.getIdUsuario(), nombreGarzon,
                pedido.getDescripcion(), pedido.getEstado(), pedido.getMetodoPago(), pedido.getCreadoEn(), items, total);
    }
}
