package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCiudadano;
import gt.muni.quejas.dto.DtoConfirmarVerificacion;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoRegistroCasoRespuesta;
import gt.muni.quejas.dto.DtoSolicitudVerificacion;
import gt.muni.quejas.service.CasoService;
import gt.muni.quejas.service.VerificacionCiudadanoService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/casos")
public class CasoController {

    private final VerificacionCiudadanoService verificacionCiudadanoService;
    private final CasoService casoService;

    public CasoController(VerificacionCiudadanoService verificacionCiudadanoService,
                          CasoService casoService) {
        this.verificacionCiudadanoService = verificacionCiudadanoService;
        this.casoService = casoService;
    }

    @PostMapping("/verificacion")
    public ResponseEntity<DtoMensaje> solicitarVerificacion(@RequestBody DtoSolicitudVerificacion dto) {
        verificacionCiudadanoService.solicitarCodigo(dto);
        return ResponseEntity.ok(new DtoMensaje(
                "Se envió un código de verificación al correo electrónico asociado al DPI."));
    }

    @PostMapping("/verificacion/confirmar")
    public ResponseEntity<DtoCiudadano> confirmarVerificacion(@RequestBody DtoConfirmarVerificacion dto) {
        return ResponseEntity.ok(verificacionCiudadanoService.confirmarCodigo(dto));
    }

    @PostMapping
    public ResponseEntity<DtoRegistroCasoRespuesta> registrar(
            @RequestParam String tokenSesion,
            @RequestParam String tipoCaso,
            @RequestParam Long idCategoria,
            @RequestParam String direccionProblema,
            @RequestParam String descripcion,
            @RequestPart(value = "evidencia", required = false) MultipartFile evidencia,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(casoService.registrar(
                tokenSesion,
                tipoCaso,
                idCategoria,
                direccionProblema,
                descripcion,
                evidencia,
                ClienteHttp.ipOrigen(request)
        ));
    }
}
