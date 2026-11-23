package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.api.dto.ApiDtos.LoginRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.LoginResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioResponse;
import cl.mesalista.restaurante.model.Usuario;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final TenantAccessService access;
    private final long expirationSeconds;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, TenantAccessService access,
                       @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.access = access;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarios.findByNombreUsuarioIgnoreCase(request.nombreUsuario().trim())
                .filter(found -> passwordEncoder.matches(request.contrasena(), found.getContrasenaHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Nombre de usuario o contraseña incorrectos."));

        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("mesalista-api")
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expirationSeconds))
                .subject(usuario.getNombreUsuario())
                .claim("userId", usuario.getIdUsuario())
                .claim("role", usuario.getRol().name())
                .claim("roles", List.of(usuario.getRol().name()));
        if (usuario.getRestaurante() != null) {
            claims.claim("restaurantId", usuario.getRestaurante().getIdRestaurante());
        }
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new LoginResponse(token, "Bearer", expirationSeconds, response(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual() {
        Long userId = access.userId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no identifica un usuario.");
        }
        return usuarios.findById(userId)
                .map(AuthService::response)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "El usuario del token ya no existe."));
    }

    static UsuarioResponse response(Usuario usuario) {
        return new UsuarioResponse(usuario.getIdUsuario(),
                usuario.getRestaurante() == null ? null : usuario.getRestaurante().getIdRestaurante(),
                usuario.getNombre(), usuario.getApellido(), usuario.getNombreUsuario(), usuario.getRol());
    }
}
