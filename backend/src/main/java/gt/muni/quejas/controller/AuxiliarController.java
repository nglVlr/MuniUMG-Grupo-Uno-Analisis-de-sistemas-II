package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleAuxiliar;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoMotivoRechazo;
import gt.muni.quejas.dto.DtoRechazoAuxiliar;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.AuxiliarService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/auxiliar")
public class AuxiliarController {

    private final AuxiliarService auxiliarService;

    public AuxiliarController(AuxiliarService auxiliarService) {
        this.auxiliarService = auxiliarService;
    }

    @GetMapping("/casos")
    public ResponseEntity<List<DtoCasoBandeja>> listarBandeja() {
        return ResponseEntity.ok(auxiliarService.listarBandeja());
    }

    @GetMapping("/casos/{id}")
    public ResponseEntity<DtoCasoDetalleAuxiliar> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(auxiliarService.obtenerDetalle(id));
    }

    @GetMapping("/motivos-rechazo")
    public ResponseEntity<List<DtoMotivoRechazo>> listarMotivosRechazo() {
        return ResponseEntity.ok(auxiliarService.listarMotivosRechazo());
    }

    @PostMapping("/casos/{id}/aceptar")
    public ResponseEntity<DtoCasoDetalleAuxiliar> aceptarCaso(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario auxiliar,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(auxiliarService.aceptarCaso(id, auxiliar, ip));
    }

    @PostMapping("/casos/{id}/asignar-tecnico")
    public ResponseEntity<DtoMensaje> asignarTecnico(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario auxiliar,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(auxiliarService.asignarTecnico(id, auxiliar, ip));
    }

    @PostMapping("/casos/{id}/rechazar")
    public ResponseEntity<DtoMensaje> rechazarCaso(
            @PathVariable Long id,
            @RequestBody DtoRechazoAuxiliar dto,
            @AuthenticationPrincipal Usuario auxiliar,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(auxiliarService.rechazarCaso(id, dto, auxiliar, ip));
    }
}