package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoActividad;
import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleAuxiliar;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoNuevaActividad;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.TecnicoService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("api/tecnico")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @GetMapping("/casos")
    public ResponseEntity<List<DtoCasoBandeja>> listarCasosAsignados(
            @AuthenticationPrincipal Usuario tecnico) {
        return ResponseEntity.ok(tecnicoService.listarCasosAsignados(tecnico));
    }

    @GetMapping("/casos/{id}")
    public ResponseEntity<DtoCasoDetalleAuxiliar> obtenerDetalle(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario tecnico) {
        return ResponseEntity.ok(tecnicoService.obtenerDetalle(id, tecnico));
    }

    @GetMapping("/casos/{id}/actividades")
    public ResponseEntity<List<DtoActividad>> listarActividades(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario tecnico) {
        return ResponseEntity.ok(tecnicoService.listarActividades(id, tecnico));
    }

    @PostMapping("/casos/{id}/actividades")
    public ResponseEntity<DtoMensaje> registrarActividad(
            @PathVariable Long id,
            @RequestBody DtoNuevaActividad dto,
            @AuthenticationPrincipal Usuario tecnico,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(tecnicoService.registrarActividad(id, dto.detalle(), tecnico, ip));
    }

    // CU-05 FA02: Finalizar caso, con el soporte obligatorio en PDF
    @PostMapping("/casos/{id}/finalizar")
    public ResponseEntity<DtoMensaje> finalizarAtencion(
            @PathVariable Long id,
            @RequestPart("detalle") String detalle,
            @RequestPart(value = "soporte", required = false) MultipartFile soporte,
            @AuthenticationPrincipal Usuario tecnico,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(tecnicoService.finalizarAtencion(id, detalle, soporte, tecnico, ip));
    }

    @GetMapping("/casos/{id}/evidencia")
    public ResponseEntity<Resource> verEvidencia(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario tecnico) throws MalformedURLException {
        Evidencia evidencia = tecnicoService.obtenerEvidenciaRegistro(id, tecnico);
        Resource recurso = new UrlResource(Path.of(evidencia.getRuta()).toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidencia.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + evidencia.getNombreArchivo() + "\"")
                .body(recurso);
    }
}
