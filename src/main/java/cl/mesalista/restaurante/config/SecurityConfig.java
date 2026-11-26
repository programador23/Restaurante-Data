package cl.mesalista.restaurante.config;

import cl.mesalista.restaurante.model.Rol;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String DEFAULT_JWT_SECRET = "mesa-lista-local-dev-secret-change-before-production-2026";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
        if (DEFAULT_JWT_SECRET.equals(secret)) {
            log.warn("JWT_SECRET usa el valor por defecto público. Define un JWT_SECRET propio en producción.");
        }
        SecretKey key = signingKey(secret);
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret, UsuarioRepository usuarios) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> userValidator = jwt -> {
            Number userId = jwt.getClaim("userId");
            if (userId == null) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "El token no identifica un usuario.", null));
            }
            return usuarios.findWithRestauranteById(userId.longValue())
                    .filter(usuario -> usuario.getNombreUsuario().equals(jwt.getSubject()))
                    .filter(usuario -> usuario.getRol().name().equals(jwt.getClaimAsString("role")))
                    .filter(usuario -> {
                        Number restaurantId = jwt.getClaim("restaurantId");
                        Long currentRestaurantId = usuario.getRestaurante() == null
                                ? null : usuario.getRestaurante().getIdRestaurante();
                        return currentRestaurantId == null
                                ? restaurantId == null
                                : restaurantId != null && currentRestaurantId.equals(restaurantId.longValue());
                    })
                    .map(usuario -> OAuth2TokenValidatorResult.success())
                    .orElseGet(() -> OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "El usuario o sus permisos cambiaron.", null)));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("mesalista-api"), userValidator));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(authorities);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/usuarios", "/api/usuarios/**")
                        .hasRole(Rol.ADMINISTRADOR_MASTER.name())
                        .requestMatchers("/api/restaurantes/*/usuarios", "/api/restaurantes/*/usuarios/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name())
                        .requestMatchers(HttpMethod.GET, "/api/restaurantes/*/menus",
                                "/api/restaurantes/*/menus/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name(), Rol.GARZON.name())
                        .requestMatchers("/api/restaurantes/*/menus", "/api/restaurantes/*/menus/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name())
                        .requestMatchers(HttpMethod.GET, "/api/menus/*/items", "/api/menus/*/items/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name(), Rol.GARZON.name())
                        .requestMatchers("/api/menus/*/items", "/api/menus/*/items/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name())
                        .requestMatchers("/api/restaurantes/*/pedidos", "/api/restaurantes/*/pedidos/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name(), Rol.GARZON.name())
                        .requestMatchers("/api/restaurantes/*/caja", "/api/restaurantes/*/caja/**")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name())
                        .requestMatchers(HttpMethod.GET, "/api/restaurantes/*/plantilla-ticket")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name(), Rol.GARZON.name())
                        .requestMatchers("/api/restaurantes/*/plantilla-ticket")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name())
                        .requestMatchers(HttpMethod.GET, "/api/restaurantes", "/api/restaurantes/*")
                        .hasAnyRole(Rol.ADMINISTRADOR_MASTER.name(), Rol.ADMINISTRADOR.name(),
                                Rol.RESTAURANTE.name(), Rol.GARZON.name())
                        .requestMatchers("/api/restaurantes/**").hasRole(Rol.ADMINISTRADOR_MASTER.name())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource
                        .authenticationEntryPoint(SecurityConfig::sinSesion)
                        .accessDeniedHandler(SecurityConfig::sinPermiso)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authentication)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(SecurityConfig::sinSesion)
                        .accessDeniedHandler(SecurityConfig::sinPermiso));
        return http.build();
    }

    private static void sinSesion(HttpServletRequest request, HttpServletResponse response,
                                  AuthenticationException exception) throws IOException {
        escribirError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Tu sesión no es válida o expiró. Inicia sesión nuevamente.");
    }

    private static void sinPermiso(HttpServletRequest request, HttpServletResponse response,
                                   AccessDeniedException exception) throws IOException {
        escribirError(response, HttpServletResponse.SC_FORBIDDEN,
                "No tienes permiso para realizar esta acción.");
    }

    private static void escribirError(HttpServletResponse response, int status, String mensaje)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":" + status
                + ",\"error\":\"" + mensaje + "\"}");
    }

    private static SecretKey signingKey(String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes.");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }
}
