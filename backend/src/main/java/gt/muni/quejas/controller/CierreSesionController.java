package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.BitacoraCambioService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CU-15: el cierre de sesión del empleado queda en bitácora. El JWT no guarda
// estado en el servidor, así que aquí solo se registra el evento.
@RestController
@RequestMapping("api/logout")
public class CierreSesionController {

    private final BitacoraCambioService bitacoraCambioService;

    public CierreSesionController(BitacoraCambioService bitacoraCambioService) {
        this.bitacoraCambioService = bitacoraCambioService;
    }

    @PostMapping
    public ResponseEntity<DtoMensaje> cerrarSesion(@AuthenticationPrincipal Usuario usuario,
                                                   HttpServletRequest request) {
        if (usuario != null) {
            bitacoraCambioService.registrarIngreso(
                    BitacoraCambioService.CIERRE_SESION,
                    usuario.getCui(),
                    ClienteHttp.ipOrigen(request),
                    "Empleado");
        }
        return ResponseEntity.ok(new DtoMensaje("Sesión cerrada"));
    }
}
