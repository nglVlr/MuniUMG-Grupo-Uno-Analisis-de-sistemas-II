package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCatalogoElemento;
import gt.muni.quejas.dto.DtoGuardarCatalogo;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.AdminCatalogoService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// CU-11 Administración de Catálogos
@RestController
@RequestMapping("api/admin/catalogos")
public class AdminCatalogoController {

    private final AdminCatalogoService adminCatalogoService;

    public AdminCatalogoController(AdminCatalogoService adminCatalogoService) {
        this.adminCatalogoService = adminCatalogoService;
    }

    @GetMapping("/tipos-caso")
    public ResponseEntity<List<String>> listarTiposCaso(
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminCatalogoService.listarTiposCaso(admin));
    }

    @GetMapping("/{catalogo}")
    public ResponseEntity<List<DtoCatalogoElemento>> listar(
            @PathVariable String catalogo,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(adminCatalogoService.listar(catalogo, admin));
    }

    @PostMapping("/{catalogo}")
    public ResponseEntity<DtoMensaje> crear(
            @PathVariable String catalogo,
            @RequestBody DtoGuardarCatalogo dto,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminCatalogoService.crear(catalogo, dto, admin, ip));
    }

    @PutMapping("/{catalogo}/{id}")
    public ResponseEntity<DtoMensaje> editar(
            @PathVariable String catalogo,
            @PathVariable Long id,
            @RequestBody DtoGuardarCatalogo dto,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminCatalogoService.editar(catalogo, id, dto, admin, ip));
    }

    // CU-11 FA01: solo se elimina si el elemento no está en uso
    @DeleteMapping("/{catalogo}/{id}")
    public ResponseEntity<DtoMensaje> eliminar(
            @PathVariable String catalogo,
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario admin,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(adminCatalogoService.eliminar(catalogo, id, admin, ip));
    }
}
