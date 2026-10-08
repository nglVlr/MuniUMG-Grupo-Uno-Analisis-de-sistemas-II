package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCasoAmpliacion;
import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.service.AmpliacionCasoService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// CU-18 Ampliación de casos. El ciudadano se identifica con el token de sesión
// que obtuvo al verificar su DPI, igual que para registrar un caso (CU-02).
@RestController
@RequestMapping("api/casos/ampliacion")
public class AmpliacionCasoController {

    private final AmpliacionCasoService ampliacionCasoService;

    public AmpliacionCasoController(AmpliacionCasoService ampliacionCasoService) {
        this.ampliacionCasoService = ampliacionCasoService;
    }

    @GetMapping
    public ResponseEntity<List<DtoCasoBandeja>> listarCasosEnAmpliacion(
            @RequestParam String tokenSesion) {
        return ResponseEntity.ok(ampliacionCasoService.listarCasosEnAmpliacion(tokenSesion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DtoCasoAmpliacion> obtenerDetalle(
            @PathVariable Long id,
            @RequestParam String tokenSesion) {
        return ResponseEntity.ok(ampliacionCasoService.obtenerDetalle(id, tokenSesion));
    }

    @PostMapping("/{id}")
    public ResponseEntity<DtoMensaje> reenviarCaso(
            @PathVariable Long id,
            @RequestParam String tokenSesion,
            @RequestParam String informacion,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia,
            HttpServletRequest request) {
        return ResponseEntity.ok(ampliacionCasoService.reenviarCaso(
                id, tokenSesion, informacion, evidencia, ClienteHttp.ipOrigen(request)));
    }
}
