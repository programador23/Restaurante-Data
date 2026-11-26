package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.RestauranteRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.RestauranteResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.PlantillaTicketRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PlantillaTicketResponse;
import cl.mesalista.restaurante.service.RestauranteService;
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
@RequestMapping("/api/restaurantes")
public class RestauranteController {
    private final RestauranteService service;

    public RestauranteController(RestauranteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestauranteResponse crear(@Valid @RequestBody RestauranteRequest request) {
        return service.crear(request);
    }

    @GetMapping
    public List<RestauranteResponse> buscar(@RequestParam(required = false) String q) {
        return service.buscar(q);
    }

    @GetMapping("/{idRestaurante}")
    public RestauranteResponse obtener(@PathVariable Long idRestaurante) {
        return service.obtener(idRestaurante);
    }

    @GetMapping("/{idRestaurante}/plantilla-ticket")
    public PlantillaTicketResponse obtenerPlantillaTicket(@PathVariable Long idRestaurante) {
        return service.obtenerPlantillaTicket(idRestaurante);
    }

    @PutMapping("/{idRestaurante}/plantilla-ticket")
    public PlantillaTicketResponse actualizarPlantillaTicket(@PathVariable Long idRestaurante,
                                                              @Valid @RequestBody PlantillaTicketRequest request) {
        return service.actualizarPlantillaTicket(idRestaurante, request);
    }

    @PutMapping("/{idRestaurante}")
    public RestauranteResponse actualizar(@PathVariable Long idRestaurante,
                                          @Valid @RequestBody RestauranteRequest request) {
        return service.actualizar(idRestaurante, request);
    }

    @DeleteMapping("/{idRestaurante}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idRestaurante) {
        service.eliminar(idRestaurante);
    }
}
