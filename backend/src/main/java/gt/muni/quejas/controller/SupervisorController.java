package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleSupervisor;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoObservaciones;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.SupervisorService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("api/supervisor")
public class SupervisorController {

    private final SupervisorService supervisorService;

    public SupervisorController(SupervisorService supervisorService) {
        this.supervisorService = supervisorService;
    }

    @GetMapping("/casos")
    public ResponseEntity<List<DtoCasoBandeja>> listarBandeja(
            @AuthenticationPrincipal Usuario supervisor) {
        return ResponseEntity.ok(supervisorService.listarBandeja(supervisor));
    }

    @GetMapping("/casos/{id}")
    public ResponseEntity<DtoCasoDetalleSupervisor> obtenerDetalle(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario supervisor) {
        return ResponseEntity.ok(supervisorService.obtenerDetalle(id, supervisor));
    }

    // CU-06 paso 9: Aprobar el reporte de atención
    @PostMapping("/casos/{id}/aprobar")
    public ResponseEntity<DtoMensaje> aprobarReporte(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario supervisor,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(supervisorService.aprobarReporte(id, supervisor, ip));
    }

    // CU-06 FA02: Devolver al Técnico
    @PostMapping("/casos/{id}/devolver")
    public ResponseEntity<DtoMensaje> devolverAlTecnico(
            @PathVariable Long id,
            @RequestBody DtoObservaciones dto,
            @AuthenticationPrincipal Usuario supervisor,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(supervisorService.devolverAlTecnico(id, dto.observaciones(), supervisor, ip));
    }

    // CU-06 FA03: Aprobar Rechazo
    @PostMapping("/casos/{id}/aprobar-rechazo")
    public ResponseEntity<DtoMensaje> aprobarRechazo(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario supervisor,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(supervisorService.aprobarRechazo(id, supervisor, ip));
    }

    // CU-06 FA04: Regresar a Revisión
    @PostMapping("/casos/{id}/regresar-revision")
    public ResponseEntity<DtoMensaje> regresarARevision(
            @PathVariable Long id,
            @RequestBody DtoObservaciones dto,
            @AuthenticationPrincipal Usuario supervisor,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(supervisorService.regresarARevision(id, dto.observaciones(), supervisor, ip));
    }

    @GetMapping("/casos/{id}/evidencia")
    public ResponseEntity<Resource> verEvidencia(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario supervisor) throws MalformedURLException {
        Evidencia evidencia = supervisorService.obtenerEvidencia(id, supervisor, Evidencia.TIPO_REGISTRO);
        return archivo(evidencia.getRuta(), evidencia.getContentType(), evidencia.getNombreArchivo());
    }

    @GetMapping("/casos/{id}/soporte-atencion")
    public ResponseEntity<Resource> verSoporteAtencion(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario supervisor) throws MalformedURLException {
        Evidencia evidencia = supervisorService.obtenerEvidencia(id, supervisor, Evidencia.TIPO_ATENCION);
        return archivo(evidencia.getRuta(), evidencia.getContentType(), evidencia.getNombreArchivo());
    }

    @GetMapping("/casos/{id}/reporte-atencion")
    public ResponseEntity<Resource> verReporteAtencion(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario supervisor) throws MalformedURLException {
        String ruta = supervisorService.obtenerRutaReporteAtencion(id, supervisor);
        return archivo(ruta, MediaType.APPLICATION_PDF_VALUE, "reporte-atencion.pdf");
    }

    private ResponseEntity<Resource> archivo(String ruta, String contentType, String nombre)
            throws MalformedURLException {
        Resource recurso = new UrlResource(Path.of(ruta).toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(recurso);
    }
}
