package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.LoginRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.LoginResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioResponse;
import cl.mesalista.restaurante.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    public UsuarioResponse usuarioActual() {
        return service.usuarioActual();
    }
}
