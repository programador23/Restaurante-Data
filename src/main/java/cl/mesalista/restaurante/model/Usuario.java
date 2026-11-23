package cl.mesalista.restaurante.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "nombre_usuario"))
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_restaurante")
    private Restaurante restaurante;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String apellido;

    @NotBlank
    @Column(name = "nombre_usuario", nullable = false, length = 80, unique = true)
    private String nombreUsuario;

    @JsonIgnore
    @NotBlank
    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    protected Usuario() {
    }

    public Usuario(Restaurante restaurante, String nombre, String apellido, String nombreUsuario,
                   String contrasenaHash, Rol rol) {
        this.restaurante = restaurante;
        this.nombre = nombre;
        this.apellido = apellido;
        this.nombreUsuario = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }

    public Long getIdUsuario() { return idUsuario; }
    public Restaurante getRestaurante() { return restaurante; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getNombreUsuario() { return nombreUsuario; }
    public Rol getRol() { return rol; }
    public String getContrasenaHash() { return contrasenaHash; }

    public void actualizar(String nombre, String apellido, String contrasenaHash, Rol rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        if (contrasenaHash != null) {
            this.contrasenaHash = contrasenaHash;
        }
        this.rol = rol;
    }
}
