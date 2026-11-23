package cl.mesalista.restaurante.service;

import cl.mesalista.restaurante.model.Rol;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantAccessService {
    public void requireRestaurantAccess(Long idRestaurante) {
        if (isMaster()) {
            return;
        }
        if (idRestaurante == null || !idRestaurante.equals(restaurantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para acceder a este restaurante.");
        }
    }

    public void requireMaster() {
        if (!isMaster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta operación requiere el rol AdministradorMaster.");
        }
    }

    public boolean isMaster() {
        return role() == Rol.ADMINISTRADOR_MASTER;
    }

    public Rol role() {
        Object value = jwt().getToken().getClaims().get("role");
        if (value instanceof String role) {
            try {
                return Rol.valueOf(role);
            } catch (IllegalArgumentException ignored) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Rol no válido en el token.");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no contiene un rol.");
    }

    public Long restaurantId() {
        Object value = jwt().getToken().getClaims().get("restaurantId");
        return value instanceof Number number ? number.longValue() : null;
    }

    public Long userId() {
        Object value = jwt().getToken().getClaims().get("userId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private JwtAuthenticationToken jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere iniciar sesión.");
    }
}
