package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoItemRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoItemResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.EditarPedidoItemsRequest;
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
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        Specification<Pedido> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("restaurante").get("idRestaurante"), idRestaurante));
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (numeroMesa != null) {
                condiciones.add(cb.equal(root.get("numeroMesa"), numeroMesa));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return pedidos.findAll(filtro, Sort.by("idPedido")).stream()
                .map(PedidoService::response).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long idRestaurante, Long idPedido) {
        return response(pedido(idRestaurante, idPedido));
    }

    @Transactional
    public PedidoResponse actualizar(Long idRestaurante, Long idPedido, PedidoRequest request) {
        Pedido pedido = pedidoBloqueado(idRestaurante, idPedido);
        pedido.actualizar(request.numeroMesa(), garzon(idRestaurante, request.idGarzon()),
                request.descripcion(), request.estado());
        pedido.reemplazarItems(crearItems(idRestaurante, request.items()));
        return response(pedidos.save(pedido));
    }

    @Transactional
    public PedidoResponse aceptarMasAntiguo(Long idRestaurante) {
        access.requireRestaurantAccess(idRestaurante);
        restauranteBloqueado(idRestaurante);
        return pedidos.findFirstByRestaurante_IdRestauranteAndEstadoOrderByCreadoEnAscIdPedidoAsc(
                        idRestaurante, EstadoPedido.NUEVO)
                .map(pedido -> {
                    pedido.aceptar();
                    return response(pedido);
                }).orElse(null);
    }

    @Transactional
    public PedidoResponse aceptar(Long idRestaurante, Long idPedido) {
        access.requireRestaurantAccess(idRestaurante);
        restauranteBloqueado(idRestaurante);
        Pedido pedido = pedidoBloqueado(idRestaurante, idPedido);
        if (pedido.getEstado() != EstadoPedido.NUEVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya no está en estado nuevo.");
        }
        pedido.aceptar();
        return response(pedido);
    }

    @Transactional
    public PedidoResponse editarItems(Long idRestaurante, Long idPedido, EditarPedidoItemsRequest request) {
        Pedido pedido = pedidoBloqueado(idRestaurante, idPedido);
        requireEnPreparacion(pedido);
        Map<Long, Integer> originales = cantidades(request.itemsOriginales());
        Map<Long, Integer> actuales = pedido.getItems().stream().collect(Collectors.toMap(
                item -> item.getItemRestaurante().getIdItemRestaurante(), PedidoItem::getCantidad));
        if (!actuales.equals(originales)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido cambió. Actualiza los pedidos antes de editarlo nuevamente.");
        }
        Map<Long, Integer> solicitados = cantidades(request.items());
        Map<Long, PedidoItem> existentes = pedido.getItems().stream().collect(Collectors.toMap(
                item -> item.getItemRestaurante().getIdItemRestaurante(), item -> item));
        List<PedidoItem> nuevos = request.items().stream().map(linea -> {
            PedidoItem existente = existentes.get(linea.idItemRestaurante());
            if (existente != null) {
                if (linea.cantidad() > existente.getCantidad() && !existente.getItemRestaurante().isDisponible()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No se puede aumentar la cantidad de un producto no disponible.");
                }
                existente.setCantidad(linea.cantidad());
                return existente;
            }
            return crearItems(idRestaurante, List.of(linea)).get(0);
        }).toList();
        pedido.actualizarItems(nuevos);
        if (!actuales.equals(solicitados)) pedido.marcarModificacionCocina();
        return response(pedido);
    }

    @Transactional
    public PedidoResponse registrarReimpresionCocina(Long idRestaurante, Long idPedido, long revision) {
        Pedido pedido = pedidoBloqueado(idRestaurante, idPedido);
        if (!pedido.isReimpresionCocinaPendiente() || pedido.getRevisionCocina() != revision) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido o su impresión cambió. Actualiza los pedidos antes de volver a imprimir.");
        }
        pedido.registrarReimpresionCocina();
        return response(pedido);
    }

    @Transactional
    public PedidoResponse avanzar(Long idRestaurante, Long idPedido, EstadoPedido estadoActual) {
        Pedido pedido = pedidoBloqueado(idRestaurante, idPedido);
        if (pedido.getEstado() != estadoActual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estado del pedido cambió. Actualiza los pedidos.");
        }
        EstadoPedido siguiente = switch (estadoActual) {
            case EN_PREPARACION -> EstadoPedido.LISTO_PARA_SERVIR;
            case LISTO_PARA_SERVIR -> EstadoPedido.ENTREGADO;
            default -> throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido no puede avanzar desde este estado.");
        };
        pedido.avanzar(siguiente);
        return response(pedido);
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
        Pedido pedido = pedidoBloqueado(idRestaurante, idPedido);
        requireEnPreparacion(pedido);
        pedidos.delete(pedido);
    }

    private static void requireEnPreparacion(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya no está en preparación.");
        }
    }

    private static Map<Long, Integer> cantidades(List<PedidoItemRequest> lineas) {
        Map<Long, Integer> cantidades = new java.util.HashMap<>();
        for (PedidoItemRequest linea : lineas) {
            if (cantidades.putIfAbsent(linea.idItemRestaurante(), linea.cantidad()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un producto no puede repetirse; aumenta su cantidad.");
            }
        }
        return cantidades;
    }

    private List<PedidoItem> crearItems(Long idRestaurante, List<PedidoItemRequest> requestItems) {
        Set<Long> usados = new HashSet<>();
        for (PedidoItemRequest linea : requestItems) {
            if (!usados.add(linea.idItemRestaurante())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Un producto no puede repetirse; aumenta su cantidad.");
            }
        }
        Map<Long, ItemRestaurante> encontrados = items.findAllConMenu(usados).stream()
                .collect(Collectors.toMap(ItemRestaurante::getIdItemRestaurante, item -> item));
        return requestItems.stream().map(linea -> {
            ItemRestaurante item = encontrados.get(linea.idItemRestaurante());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
            }
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

    private Pedido pedidoBloqueado(Long idRestaurante, Long idPedido) {
        access.requireRestaurantAccess(idRestaurante);
        return pedidos.findByIdPedidoAndRestaurante_IdRestaurante(idPedido, idRestaurante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    private Restaurante restaurante(Long id) {
        return restaurantes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private Restaurante restauranteBloqueado(Long id) {
        return restaurantes.findByIdRestaurante(id).orElseThrow(() ->
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
                pedido.getDescripcion(), pedido.getEstado(), pedido.getMetodoPago(), pedido.getCreadoEn(), items, total,
                pedido.getRevisionCocina(), pedido.isReimpresionCocinaPendiente());
    }
}
