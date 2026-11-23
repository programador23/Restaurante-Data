package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.RestauranteRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.RestauranteResponse;
import cl.mesalista.restaurante.model.Menu;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.repository.ItemRestauranteRepository;
import cl.mesalista.restaurante.repository.MenuRepository;
import cl.mesalista.restaurante.repository.PedidoRepository;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RestauranteService {
    private final RestauranteRepository restaurantes;
    private final MenuRepository menus;
    private final ItemRestauranteRepository items;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final TenantAccessService access;

    public RestauranteService(RestauranteRepository restaurantes, MenuRepository menus,
                              ItemRestauranteRepository items, PedidoRepository pedidos,
                              UsuarioRepository usuarios, TenantAccessService access) {
        this.restaurantes = restaurantes;
        this.menus = menus;
        this.items = items;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.access = access;
    }

    @Transactional
    public RestauranteResponse crear(RestauranteRequest request) {
        access.requireMaster();
        return response(restaurantes.save(new Restaurante(request.nombre().trim(), request.categoria().trim())));
    }

    @Transactional(readOnly = true)
    public List<RestauranteResponse> buscar(String q) {
        if (!access.isMaster()) {
            Long idRestaurante = access.restaurantId();
            if (idRestaurante == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "El usuario no está asociado a un restaurante.");
            }
            return restaurantes.findById(idRestaurante)
                    .filter(restaurante -> coincide(restaurante, q))
                    .map(RestauranteService::response)
                    .stream().toList();
        }
        List<Restaurante> encontrados = q == null || q.isBlank()
                ? restaurantes.findAll()
                : restaurantes.findByNombreContainingIgnoreCaseOrCategoriaContainingIgnoreCase(q.trim(), q.trim());
        return encontrados.stream().map(RestauranteService::response).toList();
    }

    @Transactional(readOnly = true)
    public RestauranteResponse obtener(Long id) {
        access.requireRestaurantAccess(id);
        return response(encontrar(id));
    }

    @Transactional
    public RestauranteResponse actualizar(Long id, RestauranteRequest request) {
        access.requireMaster();
        Restaurante restaurante = encontrar(id);
        restaurante.setNombre(request.nombre().trim());
        restaurante.setCategoria(request.categoria().trim());
        return response(restaurantes.save(restaurante));
    }

    @Transactional
    public void eliminar(Long id) {
        access.requireMaster();
        Restaurante restaurante = encontrar(id);
        pedidos.deleteAllByRestaurante_IdRestaurante(id);
        for (Menu menu : menus.findByRestaurante_IdRestaurante(id)) {
            items.deleteAll(items.findByMenu_IdMenu(menu.getIdMenu()));
            menus.delete(menu);
        }
        usuarios.deleteAllByRestaurante_IdRestaurante(id);
        restaurantes.delete(restaurante);
    }

    private Restaurante encontrar(Long id) {
        return restaurantes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private static RestauranteResponse response(Restaurante restaurante) {
        return new RestauranteResponse(restaurante.getIdRestaurante(), restaurante.getNombre(),
                restaurante.getCategoria());
    }

    private static boolean coincide(Restaurante restaurante, String q) {
        return q == null || q.isBlank()
                || restaurante.getNombre().toLowerCase().contains(q.trim().toLowerCase())
                || restaurante.getCategoria().toLowerCase().contains(q.trim().toLowerCase());
    }
}
