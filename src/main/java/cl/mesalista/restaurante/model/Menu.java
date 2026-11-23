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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "menus", uniqueConstraints = @UniqueConstraint(
        name = "uk_menus_id_restaurante", columnNames = "id_restaurante"))
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMenu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_restaurante", nullable = false)
    private Restaurante restaurante;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nombre;

    protected Menu() {
    }

    public Menu(Restaurante restaurante, String nombre) {
        this.restaurante = restaurante;
        this.nombre = nombre;
    }

    public Long getIdMenu() { return idMenu; }
    public Restaurante getRestaurante() { return restaurante; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
