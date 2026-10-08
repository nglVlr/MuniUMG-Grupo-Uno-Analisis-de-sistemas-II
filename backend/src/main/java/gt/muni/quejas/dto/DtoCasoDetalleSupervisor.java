package gt.muni.quejas.dto;

import java.time.LocalDateTime;
import java.util.List;

// CU-06: detalle del caso con el reporte de atención del Técnico y sus actividades
public record DtoCasoDetalleSupervisor(
        Long id,
        String codigoSeguimiento,
        LocalDateTime fechaIngreso,
        String tipoCaso,
        String categoria,
        String descripcion,
        String direccion,
        String estado,
        String nombreTecnico,
        String motivoRechazo,
        String comentarioRechazo,
        boolean tieneEvidencia,
        boolean tieneSoporteAtencion,
        boolean tieneReporteAtencion,
        boolean tieneReporteSupervision,
        List<DtoActividad> actividades
) {
}
