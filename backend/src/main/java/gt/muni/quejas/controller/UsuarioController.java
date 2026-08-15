package gt.muni.quejas.controller;

import gt.muni.quejas.model.EstadoUsuario;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CU-10 Mantenimiento de Usuarios. Incluye el endpoint para cambiar el estado
 * del usuario (ACTIVO, VACACIONES, PERMISO, etc.), que es lo que el Administrador
 * usa para marcar la disponibilidad real de un empleado/jefe/supervisor antes
 * de que el sistema le asigne o le reasigne un caso.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @PostMapping
    public Usuario crear(@RequestBody Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    // Cambio de estado (disponibilidad): ACTIVO | INACTIVO | BLOQUEADO | VACACIONES | PERMISO
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Usuario> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setEstado(EstadoUsuario.valueOf(body.get("estado")));
        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }
}
