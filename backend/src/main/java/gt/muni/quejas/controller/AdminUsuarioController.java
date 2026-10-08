package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCambioEstadoUsuario;
import gt.muni.quejas.dto.DtoCatalogosUsuario;
import gt.muni.quejas.dto.DtoGuardarUsuario;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoReasignacionCasos;
import gt.muni.quejas.dto.DtoUsuarioAdmin;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.AdminUsuarioService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// CU-10 Mantenimiento de Usuarios
@RestController
@RequestMapping("api/admin/usuarios")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
        this.adminUsuarioService = adminUsuarioService;
    }

    @GetMapping
    public ResponseEntity<List<DtoUsuarioAdmin>> listarUsuarios(
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminUsuarioService.listarUsuarios(admin));
    }

    @GetMapping("/catalogos")
    public ResponseEntity<DtoCatalogosUsuario> obtenerCatalogos(
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminUsuarioService.obtenerCatalogos(admin));
    }

    @GetMapping("/{cui}")
    public ResponseEntity<DtoUsuarioAdmin> obtenerUsuario(
            @PathVariable String cui,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminUsuarioService.obtenerUsuario(cui, admin));
    }

    @PostMapping
    public ResponseEntity<DtoMensaje> crearUsuario(
            @RequestBody DtoGuardarUsuario dto,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminUsuarioService.crearUsuario(dto, admin, ip));
    }

    @PutMapping("/{cui}")
    public ResponseEntity<DtoMensaje> editarUsuario(
            @PathVariable String cui,
            @RequestBody DtoGuardarUsuario dto,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminUsuarioService.editarUsuario(cui, dto, admin, ip));
    }

    // CU-10 FA01: cambio del estado de disponibilidad
    @PutMapping("/{cui}/estado")
    public ResponseEntity<DtoMensaje> cambiarEstado(
            @PathVariable String cui,
            @RequestBody DtoCambioEstadoUsuario dto,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminUsuarioService.cambiarEstado(cui, dto.idEstado(), admin, ip));
    }

    // CU-10 FA04: reasignar los casos activos antes de inactivar al usuario
    @PostMapping("/{cui}/reasignar-casos")
    public ResponseEntity<DtoMensaje> reasignarCasos(
            @PathVariable String cui,
            @RequestBody DtoReasignacionCasos dto,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminUsuarioService.reasignarCasos(cui, dto.cuiDestino(), admin, ip));
    }
}
