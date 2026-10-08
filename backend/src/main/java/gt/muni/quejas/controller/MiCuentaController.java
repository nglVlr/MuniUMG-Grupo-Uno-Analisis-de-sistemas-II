package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoActualizarCuenta;
import gt.muni.quejas.dto.DtoCambioPassword;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoMiCuenta;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.MiCuentaService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CU-12 Administrar Cuenta: disponible para todos los usuarios internos
@RestController
@RequestMapping("api/cuenta")
public class MiCuentaController {

    private final MiCuentaService miCuentaService;

    public MiCuentaController(MiCuentaService miCuentaService) {
        this.miCuentaService = miCuentaService;
    }

    @GetMapping
    public ResponseEntity<DtoMiCuenta> obtenerMiCuenta(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(miCuentaService.obtenerMiCuenta(usuario));
    }

    @PutMapping
    public ResponseEntity<DtoMensaje> actualizarDatos(
            @RequestBody DtoActualizarCuenta dto,
            @AuthenticationPrincipal Usuario usuario,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(miCuentaService.actualizarDatos(dto, usuario, ip));
    }

    // CU-12 FA01: cambio de contraseña
    @PutMapping("/password")
    public ResponseEntity<DtoMensaje> cambiarPassword(
            @RequestBody DtoCambioPassword dto,
            @AuthenticationPrincipal Usuario usuario,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(miCuentaService.cambiarPassword(dto, usuario, ip));
    }
}
