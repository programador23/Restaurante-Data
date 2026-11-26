package cl.mesalista.restaurante.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "restaurantes")
public class Restaurante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRestaurante;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String nombre;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(length = 200)
    private String ticketEncabezado;

    @Column(length = 6000)
    private String ticketContenido;

    @Column(length = 1000)
    private String ticketPie;

    protected Restaurante() {
    }

    public Restaurante(String nombre, String categoria) {
        this.nombre = nombre;
        this.categoria = categoria;
    }

    public Long getIdRestaurante() { return idRestaurante; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getTicketEncabezado() { return ticketEncabezado; }
    public String getTicketContenido() { return ticketContenido; }
    public String getTicketPie() { return ticketPie; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setPlantillaTicket(String encabezado, String contenido, String pie) {
        this.ticketEncabezado = encabezado;
        this.ticketContenido = contenido;
        this.ticketPie = pie;
    }
}
