package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PedidoResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.EditarPedidoItemsRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.AvanzarPedidoRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.ReimpresionCocinaRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PagoMesaRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.PagoMesaResponse;
import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/aceptar-mas-antiguo")
    public ResponseEntity<PedidoResponse> aceptarMasAntiguo(@PathVariable Long idRestaurante) {
        PedidoResponse pedido = service.aceptarMasAntiguo(idRestaurante);
        return pedido == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pedido);
    }

    @PostMapping("/{idPedido}/aceptar")
    public PedidoResponse aceptar(@PathVariable Long idRestaurante, @PathVariable Long idPedido) {
        return service.aceptar(idRestaurante, idPedido);
    }

    @PutMapping("/{idPedido}/items")
    public PedidoResponse editarItems(@PathVariable Long idRestaurante, @PathVariable Long idPedido,
                                      @Valid @RequestBody EditarPedidoItemsRequest request) {
        return service.editarItems(idRestaurante, idPedido, request);
    }

    @PostMapping("/{idPedido}/avanzar")
    public PedidoResponse avanzar(@PathVariable Long idRestaurante, @PathVariable Long idPedido,
                                  @Valid @RequestBody AvanzarPedidoRequest request) {
        return service.avanzar(idRestaurante, idPedido, request.estadoActual());
    }

    @PostMapping("/{idPedido}/reimpresion-cocina")
    public PedidoResponse registrarReimpresionCocina(@PathVariable Long idRestaurante, @PathVariable Long idPedido,
                                                      @Valid @RequestBody ReimpresionCocinaRequest request) {
        return service.registrarReimpresionCocina(idRestaurante, idPedido, request.revision());
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
