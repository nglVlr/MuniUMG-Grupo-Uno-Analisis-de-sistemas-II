package gt.muni.quejas.dto;

import java.time.LocalDateTime;
import java.util.List;

// Seguimiento del caso: expediente consolidado, de solo lectura
public record DtoExpediente(
        Long id,
        String codigoSeguimiento,
        LocalDateTime fechaIngreso,
        String tipoCaso,
        String categoria,
        String departamento,
        String descripcion,
        String direccion,
        String estado,
        String nombreCiudadano,
        String nombreTecnico,
        String motivoRechazo,
        String comentarioRechazo,
        String observacionesSupervision,
        String observacionesJefatura,
        boolean tieneEvidencia,
        boolean tieneSoporteAtencion,
        boolean tieneInformeInicial,
        boolean tieneReporteAtencion,
        boolean tieneReporteSupervision,
        boolean tieneOficio,
        List<DtoActividad> actividades,
        List<DtoHistorialCaso> historial
) {
}
