package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.MenuRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.MenuResponse;
import cl.mesalista.restaurante.service.MenuService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurantes/{idRestaurante}/menus")
public class MenuController {
    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuResponse crear(@PathVariable Long idRestaurante,
                              @Valid @RequestBody MenuRequest request) {
        return service.crear(idRestaurante, request);
    }

    @GetMapping
    public List<MenuResponse> buscar(@PathVariable Long idRestaurante,
                                     @RequestParam(required = false) String q) {
        return service.buscar(idRestaurante, q);
    }

    @GetMapping("/{idMenu}")
    public MenuResponse obtener(@PathVariable Long idRestaurante, @PathVariable Long idMenu) {
        return service.obtener(idRestaurante, idMenu);
    }

    @PutMapping("/{idMenu}")
    public MenuResponse actualizar(@PathVariable Long idRestaurante, @PathVariable Long idMenu,
                                   @Valid @RequestBody MenuRequest request) {
        return service.actualizar(idRestaurante, idMenu, request);
    }

    @DeleteMapping("/{idMenu}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idRestaurante, @PathVariable Long idMenu) {
        service.eliminar(idRestaurante, idMenu);
    }
}
