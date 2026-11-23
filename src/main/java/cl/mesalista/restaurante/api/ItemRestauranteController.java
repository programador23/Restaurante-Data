package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.ItemRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.ItemResponse;
import cl.mesalista.restaurante.service.ItemRestauranteService;
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
@RequestMapping("/api/menus/{idMenu}/items")
public class ItemRestauranteController {
    private final ItemRestauranteService service;

    public ItemRestauranteController(ItemRestauranteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse crear(@PathVariable Long idMenu, @Valid @RequestBody ItemRequest request) {
        return service.crear(idMenu, request);
    }

    @GetMapping
    public List<ItemResponse> buscar(@PathVariable Long idMenu,
                                     @RequestParam(required = false) String q) {
        return service.buscar(idMenu, q);
    }

    @GetMapping("/{idItemRestaurante}")
    public ItemResponse obtener(@PathVariable Long idMenu, @PathVariable Long idItemRestaurante) {
        return service.obtener(idMenu, idItemRestaurante);
    }

    @PutMapping("/{idItemRestaurante}")
    public ItemResponse actualizar(@PathVariable Long idMenu, @PathVariable Long idItemRestaurante,
                                   @Valid @RequestBody ItemRequest request) {
        return service.actualizar(idMenu, idItemRestaurante, request);
    }

    @DeleteMapping("/{idItemRestaurante}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idMenu, @PathVariable Long idItemRestaurante) {
        service.eliminar(idMenu, idItemRestaurante);
    }
}
