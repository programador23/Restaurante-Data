package cl.mesalista.restaurante.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cajas")
public class Caja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCaja;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private Restaurante restaurante;

    @Column(name = "monto_inicial", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoInicial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "efectivo_cobrado", nullable = false, precision = 19, scale = 2)
    private BigDecimal efectivoCobrado = BigDecimal.ZERO;

    @Column(name = "abierta_en", nullable = false)
    private Instant abiertaEn = Instant.now();

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    protected Caja() {
    }

    public Caja(Restaurante restaurante, BigDecimal montoInicial) {
        this.restaurante = restaurante;
        this.montoInicial = montoInicial;
    }

    public Long getIdCaja() { return idCaja; }
    public Restaurante getRestaurante() { return restaurante; }
    public BigDecimal getMontoInicial() { return montoInicial; }
    public EstadoCaja getEstado() { return estado; }
    public BigDecimal getEfectivoCobrado() { return efectivoCobrado; }
    public Instant getAbiertaEn() { return abiertaEn; }
    public Instant getCerradaEn() { return cerradaEn; }

    public void cerrar(BigDecimal efectivoCobrado) {
        this.efectivoCobrado = efectivoCobrado;
        this.estado = EstadoCaja.CERRADA;
        this.cerradaEn = Instant.now();
    }
}
