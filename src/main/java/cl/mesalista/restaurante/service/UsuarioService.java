package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioCreateRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioMasterCreateRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioUpdateRequest;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.model.Rol;
import cl.mesalista.restaurante.model.Usuario;
import cl.mesalista.restaurante.repository.PedidoRepository;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PedidoRepository pedidos;
    private final RestauranteRepository restaurantes;
    private final PasswordEncoder passwordEncoder;
    private final TenantAccessService access;

    public UsuarioService(UsuarioRepository usuarios, PedidoRepository pedidos, RestauranteRepository restaurantes,
                          PasswordEncoder passwordEncoder, TenantAccessService access) {
        this.usuarios = usuarios;
        this.pedidos = pedidos;
        this.restaurantes = restaurantes;
        this.passwordEncoder = passwordEncoder;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarRestaurante(Long idRestaurante) {
        access.requireRestaurantAccess(idRestaurante);
        return usuarios.findByRestaurante_IdRestaurante(idRestaurante).stream()
                .map(AuthService::response).toList();
    }

    @Transactional
    public UsuarioResponse crearRestaurante(Long idRestaurante, UsuarioCreateRequest request) {
        access.requireRestaurantAccess(idRestaurante);
        if (!access.isMaster() && request.rol() != Rol.RESTAURANTE && request.rol() != Rol.GARZON) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un Administrador solo puede crear usuarios Restaurante o Garzón.");
        }
        if (request.rol() == Rol.ADMINISTRADOR_MASTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Crea AdministradorMaster desde /api/usuarios, sin restaurante.");
        }
        Restaurante restaurante = restaurante(idRestaurante);
        return response(crear(restaurante, request.nombre(), request.apellido(),
                request.nombreUsuario(), request.contrasena(), request.rol()));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        access.requireMaster();
        return usuarios.findAll().stream().map(AuthService::response).toList();
    }

    @Transactional
    public UsuarioResponse crearGlobal(UsuarioMasterCreateRequest request) {
        access.requireMaster();
        Restaurante restaurante = request.idRestaurante() == null
                ? null : restaurante(request.idRestaurante());
        if (request.rol() == Rol.ADMINISTRADOR_MASTER && restaurante != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AdministradorMaster no puede estar asociado a un restaurante.");
        }
        if (request.rol() != Rol.ADMINISTRADOR_MASTER && restaurante == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debes asignar un restaurante a este usuario.");
        }
        return response(crear(restaurante, request.nombre(), request.apellido(),
                request.nombreUsuario(), request.contrasena(), request.rol()));
    }

    @Transactional
    public UsuarioResponse actualizarRestaurante(Long idRestaurante, Long idUsuario,
                                                  UsuarioUpdateRequest request) {
        access.requireRestaurantAccess(idRestaurante);
        Usuario usuario = usuarioRestaurante(idRestaurante, idUsuario);
        validarCambioRol(usuario, request.rol());
        if (!access.isMaster() && ((usuario.getRol() != Rol.RESTAURANTE && usuario.getRol() != Rol.GARZON)
                || (request.rol() != Rol.RESTAURANTE && request.rol() != Rol.GARZON))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un Administrador solo puede modificar usuarios Restaurante o Garzón.");
        }
        return actualizar(usuario, request);
    }

    @Transactional
    public UsuarioResponse actualizarGlobal(Long idUsuario, UsuarioUpdateRequest request) {
        access.requireMaster();
        Usuario usuario = usuario(idUsuario);
        validarCambioRol(usuario, request.rol());
        return actualizar(usuario, request);
    }

    @Transactional
    public void eliminarRestaurante(Long idRestaurante, Long idUsuario) {
        access.requireRestaurantAccess(idRestaurante);
        Usuario usuario = usuarioRestaurante(idRestaurante, idUsuario);
        if (usuario.getIdUsuario().equals(access.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes eliminar tu propio usuario.");
        }
        if (!access.isMaster() && usuario.getRol() != Rol.RESTAURANTE && usuario.getRol() != Rol.GARZON) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un Administrador solo puede eliminar usuarios Restaurante o Garzón.");
        }
        pedidos.desvincularGarzonDePedidos(usuario.getIdUsuario());
        usuarios.delete(usuario);
    }

    @Transactional
    public void eliminarGlobal(Long idUsuario) {
        access.requireMaster();
        Usuario usuario = usuario(idUsuario);
        if (usuario.getIdUsuario().equals(access.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes eliminar tu propio usuario.");
        }
        pedidos.desvincularGarzonDePedidos(usuario.getIdUsuario());
        usuarios.delete(usuario);
    }

    private Usuario crear(Restaurante restaurante, String nombre, String apellido,
                          String nombreUsuario, String contrasena, Rol rol) {
        String username = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByNombreUsuarioIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso.");
        }
        return usuarios.save(new Usuario(restaurante, nombre.trim(), apellido.trim(), username,
                passwordEncoder.encode(contrasena), rol));
    }

    private UsuarioResponse actualizar(Usuario usuario, UsuarioUpdateRequest request) {
        String hash = request.contrasena() == null ? null : passwordEncoder.encode(request.contrasena());
        usuario.actualizar(request.nombre().trim(), request.apellido().trim(), hash, request.rol());
        return response(usuarios.save(usuario));
    }

    private void validarCambioRol(Usuario usuario, Rol nuevoRol) {
        if (usuario.getRestaurante() == null && nuevoRol != Rol.ADMINISTRADOR_MASTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cambiar el alcance de un AdministradorMaster.");
        }
        if (usuario.getRestaurante() != null && nuevoRol == Rol.ADMINISTRADOR_MASTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AdministradorMaster no puede estar asociado a un restaurante.");
        }
    }

    private Usuario usuarioRestaurante(Long idRestaurante, Long idUsuario) {
        return usuarios.findByIdUsuarioAndRestaurante_IdRestaurante(idUsuario, idRestaurante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Usuario usuario(Long idUsuario) {
        return usuarios.findWithRestauranteById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Restaurante restaurante(Long idRestaurante) {
        return restaurantes.findById(idRestaurante).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante no encontrado"));
    }

    private static UsuarioResponse response(Usuario usuario) {
        return AuthService.response(usuario);
    }
}
