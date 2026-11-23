package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.MenuRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.MenuResponse;
import cl.mesalista.restaurante.model.Menu;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.repository.MenuRepository;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MenuService {
    private final MenuRepository menus;
    private final RestauranteRepository restaurantes;
    private final TenantAccessService access;

    public MenuService(MenuRepository menus, RestauranteRepository restaurantes, TenantAccessService access) {
        this.menus = menus;
        this.restaurantes = restaurantes;
        this.access = access;
    }

    @Transactional
    public MenuResponse crear(Long idRestaurante, MenuRequest request) {
        access.requireRestaurantAccess(idRestaurante);
        Restaurante restaurante = restaurante(idRestaurante);
        if (menus.existsByRestaurante_IdRestaurante(idRestaurante)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cada restaurante puede tener un único menú.");
        }
        return response(menus.save(new Menu(restaurante, request.nombre().trim())));
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> buscar(Long idRestaurante, String q) {
        access.requireRestaurantAccess(idRestaurante);
        restaurante(idRestaurante);
        return menus.findByRestaurante_IdRestaurante(idRestaurante).stream()
                .filter(menu -> q == null || q.isBlank()
                        || menu.getNombre().toLowerCase().contains(q.trim().toLowerCase()))
                .map(MenuService::response).toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse obtener(Long idRestaurante, Long idMenu) {
        return response(menu(idRestaurante, idMenu));
    }

    @Transactional
    public MenuResponse actualizar(Long idRestaurante, Long idMenu, MenuRequest request) {
        Menu menu = menu(idRestaurante, idMenu);
        menu.setNombre(request.nombre().trim());
        return response(menus.save(menu));
    }

    @Transactional
    public void eliminar(Long idRestaurante, Long idMenu) {
        menus.delete(menu(idRestaurante, idMenu));
    }

    private Menu menu(Long idRestaurante, Long idMenu) {
        Menu menu = menus.findById(idMenu)
                .filter(found -> found.getRestaurante().getIdRestaurante().equals(idRestaurante))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menú no encontrado"));
        access.requireRestaurantAccess(idRestaurante);
        return menu;
    }

    private Restaurante restaurante(Long id) {
        return restaurantes.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private static MenuResponse response(Menu menu) {
        return new MenuResponse(menu.getIdMenu(), menu.getRestaurante().getIdRestaurante(),
                menu.getRestaurante().getNombre(), menu.getNombre());
    }
}
