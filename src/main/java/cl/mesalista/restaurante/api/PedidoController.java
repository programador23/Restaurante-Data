package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.PagoMesaRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PagoMesaResponse;
import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.service.PedidoService;
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
@RequestMapping("/api/restaurantes/{idRestaurante}/pedidos")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@PathVariable Long idRestaurante,
                                @Valid @RequestBody PedidoRequest request) {
        return service.crear(idRestaurante, request);
    }

    @GetMapping
    public List<PedidoResponse> buscar(@PathVariable Long idRestaurante,
                                       @RequestParam(required = false) EstadoPedido estado,
                                       @RequestParam(required = false) Integer numeroMesa) {
        return service.buscar(idRestaurante, estado, numeroMesa);
    }

    @GetMapping("/{idPedido}")
    public PedidoResponse obtener(@PathVariable Long idRestaurante, @PathVariable Long idPedido) {
        return service.obtener(idRestaurante, idPedido);
    }

    @PutMapping("/{idPedido}")
    public PedidoResponse actualizar(@PathVariable Long idRestaurante, @PathVariable Long idPedido,
                                     @Valid @RequestBody PedidoRequest request) {
        return service.actualizar(idRestaurante, idPedido, request);
    }

    @PostMapping("/mesas/{numeroMesa}/pago")
    public PagoMesaResponse pagarMesa(@PathVariable Long idRestaurante,
                                      @PathVariable int numeroMesa,
                                      @Valid @RequestBody PagoMesaRequest request) {
        return service.pagarMesa(idRestaurante, numeroMesa, request);
    }

    @DeleteMapping("/{idPedido}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idRestaurante, @PathVariable Long idPedido) {
        service.eliminar(idRestaurante, idPedido);
    }
}
