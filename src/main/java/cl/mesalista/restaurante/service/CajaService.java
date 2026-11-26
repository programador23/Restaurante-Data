package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.CajaAperturaRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.CajaResponse;
import cl.mesalista.restaurante.model.Caja;
import cl.mesalista.restaurante.model.EstadoCaja;
import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.model.MetodoPago;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.repository.CajaRepository;
import cl.mesalista.restaurante.repository.PedidoRepository;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CajaService {
    private final CajaRepository cajas;
    private final PedidoRepository pedidos;
    private final RestauranteRepository restaurantes;
    private final TenantAccessService access;

    public CajaService(CajaRepository cajas, PedidoRepository pedidos,
                       RestauranteRepository restaurantes, TenantAccessService access) {
        this.cajas = cajas;
        this.pedidos = pedidos;
        this.restaurantes = restaurantes;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public CajaResponse actual(Long idRestaurante) {
        access.requireRestaurantAccess(idRestaurante);
        return cajas.findByRestaurante_IdRestauranteAndEstado(idRestaurante, EstadoCaja.ABIERTA)
                .map(this::response)
                .orElseGet(() -> new CajaResponse(null, null, null, null, null, null, null));
    }

    @Transactional
    public CajaResponse abrir(Long idRestaurante, CajaAperturaRequest request) {
        access.requireRestaurantAccess(idRestaurante);
        // El lock sobre el restaurante serializa aperturas simultáneas.
        Restaurante restaurante = restaurantes.findByIdRestaurante(idRestaurante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado."));
        if (cajas.findByRestaurante_IdRestauranteAndEstado(idRestaurante, EstadoCaja.ABIERTA).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una caja abierta.");
        }
        return response(cajas.save(new Caja(restaurante, request.montoInicial())));
    }

    @Transactional
    public CajaResponse cerrar(Long idRestaurante) {
        access.requireRestaurantAccess(idRestaurante);
        restaurantes.findByIdRestaurante(idRestaurante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado."));
        Caja caja = cajas.findByRestaurante_IdRestauranteAndEstado(idRestaurante, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay una caja abierta."));
        caja.cerrar(efectivoCobrado(caja, Instant.now()));
        return response(cajas.save(caja));
    }

    private CajaResponse response(Caja caja) {
        BigDecimal efectivoCobrado = caja.getEstado() == EstadoCaja.CERRADA
                ? caja.getEfectivoCobrado()
                : efectivoCobrado(caja, Instant.now());
        return new CajaResponse(
                caja.getIdCaja(),
                caja.getEstado(),
                caja.getMontoInicial(),
                efectivoCobrado,
                caja.getMontoInicial().add(efectivoCobrado),
                caja.getAbiertaEn(),
                caja.getCerradaEn());
    }

    private BigDecimal efectivoCobrado(Caja caja, Instant fin) {
        BigDecimal total = pedidos.sumarCobrado(caja.getRestaurante().getIdRestaurante(),
                EstadoPedido.PAGADO, MetodoPago.EFECTIVO, caja.getAbiertaEn(), fin);
        return total == null ? BigDecimal.ZERO : total;
    }
}
