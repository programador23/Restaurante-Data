package cl.mesalista.restaurante.api.dto;

import cl.mesalista.restaurante.model.EstadoPedido;
import cl.mesalista.restaurante.model.EstadoCaja;
import cl.mesalista.restaurante.model.MetodoPago;
import cl.mesalista.restaurante.model.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record RestauranteRequest(
            @NotBlank String nombre,
            @NotBlank String categoria) {
    }

    public record RestauranteResponse(Long idRestaurante, String nombre, String categoria) {
    }

    public record MenuRequest(@NotBlank String nombre) {
    }

    public record MenuResponse(
            Long idMenu, Long idRestaurante, String nombreRestaurante, String nombre) {
    }

    public record ItemRequest(
            @NotBlank String nombre,
            @NotBlank String descripcion,
            @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
            String categoria,
            String imagen,
            Boolean disponible) {
    }

    public record ItemResponse(
            Long idItemRestaurante,
            Long idMenu,
            String nombre,
            String descripcion,
            BigDecimal valor,
            String categoria,
            String imagen,
            boolean disponible) {
    }

    public record PedidoItemRequest(
            @NotNull Long idItemRestaurante,
            @Positive int cantidad) {
    }

    public record PedidoRequest(
            @Positive int numeroMesa,
            Long idGarzon,
            String descripcion,
            EstadoPedido estado,
            @NotEmpty List<@Valid PedidoItemRequest> items) {
    }

    public record PedidoItemResponse(
            Long idItemRestaurante,
            String nombre,
            int cantidad,
            BigDecimal valorUnitario,
            BigDecimal subtotal) {
    }

    public record PedidoResponse(
            Long idPedido,
            Long idRestaurante,
            int numeroMesa,
            Long idGarzon,
            String nombreGarzon,
            String descripcion,
            EstadoPedido estado,
            MetodoPago metodoPago,
            Instant creadoEn,
            List<PedidoItemResponse> items,
            BigDecimal total) {
    }

    public record PagoMesaRequest(
            @NotEmpty List<@NotNull @Positive Long> idPedidos,
            @NotNull @DecimalMin("0.01") BigDecimal totalConfirmado,
            @NotNull MetodoPago metodoPago) {
    }

    public record PagoMesaResponse(
            int numeroMesa,
            List<PedidoResponse> pedidos,
            BigDecimal total) {
    }

    public record CajaAperturaRequest(
            @NotNull @DecimalMin("0.00") BigDecimal montoInicial) {
    }

    public record CajaResponse(
            Long idCaja,
            EstadoCaja estado,
            BigDecimal montoInicial,
            BigDecimal efectivoCobrado,
            BigDecimal totalCaja,
            Instant abiertaEn,
            Instant cerradaEn) {
    }

    public record LoginRequest(
            @NotBlank String nombreUsuario,
            @NotBlank String contrasena) {
    }

    public record UsuarioCreateRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Size(max = 80) String nombreUsuario,
            @NotBlank @Size(min = 8, max = 72) String contrasena,
            @NotNull Rol rol) {
    }

    public record UsuarioMasterCreateRequest(
            Long idRestaurante,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @NotBlank @Size(max = 80) String nombreUsuario,
            @NotBlank @Size(min = 8, max = 72) String contrasena,
            @NotNull Rol rol) {
    }

    public record UsuarioUpdateRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @Size(min = 8, max = 72) String contrasena,
            @NotNull Rol rol) {
    }

    public record UsuarioResponse(
            Long idUsuario,
            Long idRestaurante,
            String nombre,
            String apellido,
            String nombreUsuario,
            Rol rol) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn,
                                UsuarioResponse usuario) {
    }
}
