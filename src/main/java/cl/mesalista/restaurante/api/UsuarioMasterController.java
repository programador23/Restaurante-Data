package cl.mesalista.restaurante.api;

import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioMasterCreateRequest;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioResponse;
import cl.mesalista.restaurante.api.dto.ApiDtos.UsuarioUpdateRequest;
import cl.mesalista.restaurante.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioMasterController {
    private final UsuarioService service;

    public UsuarioMasterController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listarTodos() {
        return service.listarTodos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioMasterCreateRequest request) {
        return service.crearGlobal(request);
    }

    @PutMapping("/{idUsuario}")
    public UsuarioResponse actualizar(@PathVariable Long idUsuario,
                                      @Valid @RequestBody UsuarioUpdateRequest request) {
        return service.actualizarGlobal(idUsuario, request);
    }

    @DeleteMapping("/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idUsuario) {
        service.eliminarGlobal(idUsuario);
    }
}
