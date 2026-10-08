package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoExpediente;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.SeguimientoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;

// Seguimiento del caso: la misma consulta sirve a todas las bandejas
@RestController
@RequestMapping("api/seguimiento")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    public SeguimientoController(SeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    @GetMapping("/casos")
    public ResponseEntity<DtoExpediente> buscarPorCodigo(
            @RequestParam("codigo") String codigo,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(seguimientoService.buscarPorCodigo(codigo, usuario));
    }

    @GetMapping("/casos/{id}")
    public ResponseEntity<DtoExpediente> obtenerExpediente(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(seguimientoService.obtenerExpediente(id, usuario));
    }

    @GetMapping("/casos/{id}/documentos/{tipo}")
    public ResponseEntity<Resource> verDocumento(
            @PathVariable Long id,
            @PathVariable String tipo,
            @AuthenticationPrincipal Usuario usuario) throws MalformedURLException {

        // La evidencia del ciudadano y el soporte del Técnico son archivos subidos;
        // el resto son informes que genera el sistema.
        if (SeguimientoService.DOC_EVIDENCIA.equals(tipo)
                || SeguimientoService.DOC_SOPORTE_ATENCION.equals(tipo)) {
            Evidencia evidencia = seguimientoService.obtenerEvidencia(id, usuario, tipo);
            return archivo(evidencia.getRuta(), evidencia.getContentType(), evidencia.getNombreArchivo());
        }

        String ruta = seguimientoService.obtenerRutaInforme(id, usuario, tipo);
        return archivo(ruta, MediaType.APPLICATION_PDF_VALUE, tipo + ".pdf");
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
