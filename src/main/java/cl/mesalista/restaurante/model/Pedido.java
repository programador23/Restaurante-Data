package cl.mesalista.restaurante.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedidos_restaurante_estado", columnList = "id_restaurante,estado"),
        @Index(name = "idx_pedidos_restaurante_mesa_estado", columnList = "id_restaurante,numero_mesa,estado")
})
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private Restaurante restaurante;

    @Positive
    @Column(name = "numero_mesa", nullable = false)
    private int numeroMesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_garzon")
    private Usuario garzon;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.NUEVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    @Column(nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "pagado_en")
    private Instant pagadoEn;

    @Column(name = "revision_cocina")
    private Long revisionCocina;

    @Column(name = "revision_cocina_impresa")
    private Long revisionCocinaImpresa;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(Restaurante restaurante, int numeroMesa, Usuario garzon,
                  String descripcion, EstadoPedido estado) {
        this.restaurante = restaurante;
        this.numeroMesa = numeroMesa;
        this.garzon = garzon;
        this.descripcion = descripcion;
        this.estado = estado == null ? EstadoPedido.NUEVO : estado;
    }

    public void reemplazarItems(List<PedidoItem> nuevosItems) {
        items.clear();
        nuevosItems.forEach(item -> {
            item.setPedido(this);
            items.add(item);
        });
    }

    public void actualizarItems(List<PedidoItem> nuevosItems) {
        items.removeIf(item -> !nuevosItems.contains(item));
        nuevosItems.stream().filter(item -> !items.contains(item)).forEach(item -> {
            item.setPedido(this);
            items.add(item);
        });
    }

    public Long getIdPedido() { return idPedido; }
    public Restaurante getRestaurante() { return restaurante; }
    public int getNumeroMesa() { return numeroMesa; }
    public Usuario getGarzon() { return garzon; }
    public String getDescripcion() { return descripcion; }
    public EstadoPedido getEstado() { return estado; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getPagadoEn() { return pagadoEn; }
    public long getRevisionCocina() { return revisionCocina == null ? 0 : revisionCocina; }
    public boolean isReimpresionCocinaPendiente() {
        return getRevisionCocina() > (revisionCocinaImpresa == null ? 0 : revisionCocinaImpresa);
    }
    public List<PedidoItem> getItems() { return items; }

    public void actualizar(int numeroMesa, Usuario garzon, String descripcion, EstadoPedido estado) {
        this.numeroMesa = numeroMesa;
        this.garzon = garzon;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public void marcarPagado(MetodoPago metodoPago) {
        this.estado = EstadoPedido.PAGADO;
        this.metodoPago = metodoPago;
        this.pagadoEn = Instant.now();
    }

    public void aceptar() {
        this.estado = EstadoPedido.EN_PREPARACION;
    }

    public void avanzar(EstadoPedido estado) {
        this.estado = estado;
    }

    public void marcarModificacionCocina() {
        this.revisionCocina = getRevisionCocina() + 1;
    }

    public void registrarReimpresionCocina() {
        this.revisionCocinaImpresa = getRevisionCocina();
    }
}
