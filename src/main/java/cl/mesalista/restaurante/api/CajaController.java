package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.CajaAperturaRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.CajaResponse;
import cl.mesalista.restaurante.service.CajaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurantes/{idRestaurante}/caja")
public class CajaController {
    private final CajaService service;

    public CajaController(CajaService service) {
        this.service = service;
    }

    @GetMapping
    public CajaResponse actual(@PathVariable Long idRestaurante) {
        return service.actual(idRestaurante);
    }

    @PostMapping("/apertura")
    public CajaResponse abrir(@PathVariable Long idRestaurante,
                              @Valid @RequestBody CajaAperturaRequest request) {
        return service.abrir(idRestaurante, request);
    }

    @PostMapping("/cierre")
    public CajaResponse cerrar(@PathVariable Long idRestaurante) {
        return service.cerrar(idRestaurante);
    }
}
