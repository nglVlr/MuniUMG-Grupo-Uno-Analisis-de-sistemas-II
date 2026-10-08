package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleSupervisor;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoObservaciones;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.JefaturaService;
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
@RequestMapping("api/jefatura")
public class JefaturaController {

    private final JefaturaService jefaturaService;

    public JefaturaController(JefaturaService jefaturaService) {
        this.jefaturaService = jefaturaService;
    }

    @GetMapping("/casos")
    public ResponseEntity<List<DtoCasoBandeja>> listarBandeja(
            @AuthenticationPrincipal Usuario jefatura) {
        return ResponseEntity.ok(jefaturaService.listarBandeja(jefatura));
    }

    @GetMapping("/casos/{id}")
    public ResponseEntity<DtoCasoDetalleSupervisor> obtenerDetalle(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura) {
        return ResponseEntity.ok(jefaturaService.obtenerDetalle(id, jefatura));
    }

    // CU-07 paso 9: Firmar Oficio
    @PostMapping("/casos/{id}/firmar")
    public ResponseEntity<DtoMensaje> firmarOficio(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(jefaturaService.firmarOficio(id, jefatura, ip));
    }

    // CU-07 FA02: Devolver a Supervisión
    @PostMapping("/casos/{id}/devolver")
    public ResponseEntity<DtoMensaje> devolverASupervision(
            @PathVariable Long id,
            @RequestBody DtoObservaciones dto,
            @AuthenticationPrincipal Usuario jefatura,
            HttpServletRequest request) {
        String ip = ClienteHttp.ipOrigen(request);
        return ResponseEntity.ok(jefaturaService.devolverASupervision(id, dto.observaciones(), jefatura, ip));
    }

    @GetMapping("/casos/{id}/evidencia")
    public ResponseEntity<Resource> verEvidencia(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura) throws MalformedURLException {
        Evidencia evidencia = jefaturaService.obtenerEvidencia(id, jefatura, Evidencia.TIPO_REGISTRO);
        return archivo(evidencia.getRuta(), evidencia.getContentType(), evidencia.getNombreArchivo());
    }

    @GetMapping("/casos/{id}/soporte-atencion")
    public ResponseEntity<Resource> verSoporteAtencion(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura) throws MalformedURLException {
        Evidencia evidencia = jefaturaService.obtenerEvidencia(id, jefatura, Evidencia.TIPO_ATENCION);
        return archivo(evidencia.getRuta(), evidencia.getContentType(), evidencia.getNombreArchivo());
    }

    @GetMapping("/casos/{id}/reporte-atencion")
    public ResponseEntity<Resource> verReporteAtencion(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura) throws MalformedURLException {
        String ruta = jefaturaService.obtenerRutaReporteAtencion(id, jefatura);
        return archivo(ruta, MediaType.APPLICATION_PDF_VALUE, "reporte-atencion.pdf");
    }

    @GetMapping("/casos/{id}/reporte-supervision")
    public ResponseEntity<Resource> verReporteSupervision(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura) throws MalformedURLException {
        String ruta = jefaturaService.obtenerRutaReporteSupervision(id, jefatura);
        return archivo(ruta, MediaType.APPLICATION_PDF_VALUE, "reporte-supervision.pdf");
    }

    @GetMapping("/casos/{id}/oficio")
    public ResponseEntity<Resource> verOficio(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario jefatura) throws MalformedURLException {
        String ruta = jefaturaService.obtenerRutaOficio(id, jefatura);
        return archivo(ruta, MediaType.APPLICATION_PDF_VALUE, "oficio-resolucion.pdf");
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
