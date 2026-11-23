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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "items_restaurante")
public class ItemRestaurante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItemRestaurante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_menu", nullable = false)
    private Menu menu;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(length = 80)
    private String categoria;

    @Column(length = 1000)
    private String imagen;

    @Column(nullable = false)
    private boolean disponible = true;

    protected ItemRestaurante() {
    }

    public ItemRestaurante(Menu menu, String nombre, String descripcion, BigDecimal valor,
                           String categoria, String imagen, boolean disponible) {
        this.menu = menu;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valor = valor;
        this.categoria = categoria;
        this.imagen = imagen;
        this.disponible = disponible;
    }

    public Long getIdItemRestaurante() { return idItemRestaurante; }
    public Menu getMenu() { return menu; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getValor() { return valor; }
    public String getCategoria() { return categoria; }
    public String getImagen() { return imagen; }
    public boolean isDisponible() { return disponible; }

    public void actualizar(String nombre, String descripcion, BigDecimal valor,
                           String categoria, String imagen, boolean disponible) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valor = valor;
        this.categoria = categoria;
        this.imagen = imagen;
        this.disponible = disponible;
    }
}
