package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.ItemRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.ItemResponse;
import cl.mesalista.restaurante.model.ItemRestaurante;
import cl.mesalista.restaurante.model.Menu;
import cl.mesalista.restaurante.repository.ItemRestauranteRepository;
import cl.mesalista.restaurante.repository.MenuRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemRestauranteService {
    private final ItemRestauranteRepository items;
    private final MenuRepository menus;
    private final TenantAccessService access;

    public ItemRestauranteService(ItemRestauranteRepository items, MenuRepository menus,
                                  TenantAccessService access) {
        this.items = items;
        this.menus = menus;
        this.access = access;
    }

    @Transactional
    public ItemResponse crear(Long idMenu, ItemRequest request) {
        Menu menu = menu(idMenu);
        return response(items.save(new ItemRestaurante(menu, request.nombre().trim(),
                request.descripcion().trim(), request.valor(), request.categoria(), request.imagen(),
                request.disponible() == null || request.disponible())));
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> buscar(Long idMenu, String q) {
        menu(idMenu);
        return items.findByMenu_IdMenu(idMenu).stream()
                .filter(item -> q == null || q.isBlank()
                        || item.getNombre().toLowerCase().contains(q.trim().toLowerCase())
                        || item.getDescripcion().toLowerCase().contains(q.trim().toLowerCase()))
                .map(ItemRestauranteService::response).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse obtener(Long idMenu, Long idItem) {
        return response(item(idMenu, idItem));
    }

    @Transactional
    public ItemResponse actualizar(Long idMenu, Long idItem, ItemRequest request) {
        ItemRestaurante item = item(idMenu, idItem);
        item.actualizar(request.nombre().trim(), request.descripcion().trim(), request.valor(),
                request.categoria(), request.imagen(), request.disponible() == null || request.disponible());
        return response(items.save(item));
    }

    @Transactional
    public void eliminar(Long idMenu, Long idItem) {
        items.delete(item(idMenu, idItem));
    }

    private ItemRestaurante item(Long idMenu, Long idItem) {
        ItemRestaurante item = items.findById(idItem)
                .filter(found -> found.getMenu().getIdMenu().equals(idMenu))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        access.requireRestaurantAccess(item.getMenu().getRestaurante().getIdRestaurante());
        return item;
    }

    private Menu menu(Long id) {
        Menu menu = menus.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Menú no encontrado"));
        access.requireRestaurantAccess(menu.getRestaurante().getIdRestaurante());
        return menu;
    }

    private static ItemResponse response(ItemRestaurante item) {
        return new ItemResponse(item.getIdItemRestaurante(), item.getMenu().getIdMenu(),
                item.getNombre(), item.getDescripcion(), item.getValor(), item.getCategoria(),
                item.getImagen(), item.isDisponible());
    }
}
