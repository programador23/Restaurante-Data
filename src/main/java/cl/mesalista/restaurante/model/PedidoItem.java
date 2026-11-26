package cl.mesalista.restaurante.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido_items")
public class PedidoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedidoItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_restaurante", nullable = false)
    private ItemRestaurante itemRestaurante;

    @Positive
    @Column(nullable = false)
    private int cantidad;

    @Column(name = "nombre_item", nullable = false, length = 120)
    private String nombreItem;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;

    protected PedidoItem() {
    }

    public PedidoItem(ItemRestaurante itemRestaurante, int cantidad) {
        this.itemRestaurante = itemRestaurante;
        this.cantidad = cantidad;
        this.nombreItem = itemRestaurante.getNombre();
        this.valorUnitario = itemRestaurante.getValor();
    }

    void setPedido(Pedido pedido) { this.pedido = pedido; }
    public Long getIdPedidoItem() { return idPedidoItem; }
    public ItemRestaurante getItemRestaurante() { return itemRestaurante; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getNombreItem() { return nombreItem; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
}
