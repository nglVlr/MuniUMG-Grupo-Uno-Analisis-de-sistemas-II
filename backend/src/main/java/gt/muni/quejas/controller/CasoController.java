package gt.muni.quejas.controller;

import gt.muni.quejas.dto.*;
import gt.muni.quejas.service.CasoService;
import gt.muni.quejas.service.VerificacionCiudadanoService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/consulta")
    public ResponseEntity<DtoConsultaCasoRespuesta> consultarEstado(
            @RequestParam(required = false) String codigoSeguimiento) {
        return ResponseEntity.ok(casoService.consultarEstado(codigoSeguimiento));
    }
}
