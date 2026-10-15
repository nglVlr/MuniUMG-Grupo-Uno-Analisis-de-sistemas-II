package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoOpcionCatalogo;
import gt.muni.quejas.dto.DtoReporteEstadistico;
import gt.muni.quejas.dto.DtoTrazabilidadCaso;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.ReporteExcelService;
import gt.muni.quejas.service.ReportePdfService;
import gt.muni.quejas.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// CU-14 Reportería: estadísticas y trazabilidad del caso
@RestController
@RequestMapping("api/reportes")
public class ReporteController {

    // El tipo de contenido de un archivo .xlsx de Excel
    private static final String TIPO_EXCEL =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteService reporteService;
    private final ReportePdfService reportePdfService;
    private final ReporteExcelService reporteExcelService;

    public ReporteController(ReporteService reporteService,
                             ReportePdfService reportePdfService,
                             ReporteExcelService reporteExcelService) {
        this.reporteService = reporteService;
        this.reportePdfService = reportePdfService;
        this.reporteExcelService = reporteExcelService;
    }

    // RN32: lista del filtro Departamento del formulario reportes estadísticos
    @GetMapping("/departamentos")
    public ResponseEntity<List<DtoOpcionCatalogo>> departamentos(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.listarDepartamentos(usuario));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<DtoReporteEstadistico> estadisticas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.generarEstadisticas(
                fechaInicio, fechaFin, idDepartamento, tipoCaso, estado, usuario));
    }

    @GetMapping("/estadisticas/pdf")
    public ResponseEntity<byte[]> estadisticasPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal Usuario usuario) {
        DtoReporteEstadistico reporte = reporteService.generarEstadisticas(
                fechaInicio, fechaFin, idDepartamento, tipoCaso, estado, usuario);
        return archivo(reportePdfService.generarEstadisticas(reporte),
                MediaType.APPLICATION_PDF_VALUE, "reporte-estadistico.pdf");
    }

    // Se exporta como libro de Excel (.xlsx) con la tabla ya formateada
    @GetMapping("/estadisticas/excel")
    public ResponseEntity<byte[]> estadisticasExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal Usuario usuario) {
        DtoReporteEstadistico reporte = reporteService.generarEstadisticas(
                fechaInicio, fechaFin, idDepartamento, tipoCaso, estado, usuario);
        return archivo(reporteExcelService.generarEstadisticas(reporte),
                TIPO_EXCEL, "reporte-estadistico.xlsx");
    }

    @GetMapping("/trazabilidad")
    public ResponseEntity<DtoTrazabilidadCaso> trazabilidad(
            @RequestParam(required = false) String codigoSeguimiento,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reporteService.generarTrazabilidad(codigoSeguimiento, usuario));
    }

    @GetMapping("/trazabilidad/pdf")
    public ResponseEntity<byte[]> trazabilidadPdf(
            @RequestParam(required = false) String codigoSeguimiento,
            @AuthenticationPrincipal Usuario usuario) {
        DtoTrazabilidadCaso traza = reporteService.generarTrazabilidad(codigoSeguimiento, usuario);
        return archivo(reportePdfService.generarTrazabilidad(traza),
                MediaType.APPLICATION_PDF_VALUE, "trazabilidad-" + traza.codigoSeguimiento() + ".pdf");
    }

    @GetMapping("/trazabilidad/excel")
    public ResponseEntity<byte[]> trazabilidadExcel(
            @RequestParam(required = false) String codigoSeguimiento,
            @AuthenticationPrincipal Usuario usuario) {
        DtoTrazabilidadCaso traza = reporteService.generarTrazabilidad(codigoSeguimiento, usuario);
        return archivo(reporteExcelService.generarTrazabilidad(traza),
                TIPO_EXCEL, "trazabilidad-" + traza.codigoSeguimiento() + ".xlsx");
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, String contentType, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(contenido);
    }
}
