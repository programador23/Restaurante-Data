package cl.mesalista.restaurante.config;

import cl.mesalista.restaurante.model.ItemRestaurante;
import cl.mesalista.restaurante.model.Menu;
import cl.mesalista.restaurante.model.Restaurante;
import cl.mesalista.restaurante.model.Rol;
import cl.mesalista.restaurante.model.Usuario;
import cl.mesalista.restaurante.repository.ItemRestauranteRepository;
import cl.mesalista.restaurante.repository.MenuRepository;
import cl.mesalista.restaurante.repository.RestauranteRepository;
import cl.mesalista.restaurante.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"docker", "local"})
public class DemoDataInitializer {
    @Bean
    CommandLineRunner cargarDatosDemo(RestauranteRepository restaurantes,
                                      MenuRepository menus,
                                      ItemRestauranteRepository items,
                                      UsuarioRepository usuarios,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${app.demo.admin-username:admin}") String adminUsername,
                                      @Value("${app.demo.admin-password:AdminDemo2026!}") String adminPassword) {
        return args -> {
            Restaurante restaurante = restaurantes.findByNombreIgnoreCase("La Buena Mesa Demo")
                    .orElseGet(() -> restaurantes.save(
                            new Restaurante("La Buena Mesa Demo", "Restaurante")));
            if (menus.findByRestaurante_IdRestaurante(restaurante.getIdRestaurante()).isEmpty()) {
                Menu menu = menus.save(new Menu(restaurante, "Carta principal"));
                items.save(new ItemRestaurante(menu, "Hamburguesa de la casa",
                        "Vacuno, queso mantecoso y cebolla caramelizada",
                        new BigDecimal("10990"), "Platos principales", null, true));
                items.save(new ItemRestaurante(menu, "Limonada menta jengibre",
                        "Limón, menta fresca y jengibre",
                        new BigDecimal("3490"), "Bebestibles", null, true));
            }
            if (!usuarios.existsByNombreUsuarioIgnoreCase(adminUsername)) {
                usuarios.save(new Usuario(null, "Administradora", "Demo", adminUsername,
                        passwordEncoder.encode(adminPassword), Rol.ADMINISTRADOR_MASTER));
            }
        };
    }
}
