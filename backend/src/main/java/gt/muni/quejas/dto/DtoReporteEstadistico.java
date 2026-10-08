package gt.muni.quejas.dto;

import java.time.LocalDate;
import java.util.List;

// CU-14 Reportería: resumen estadístico de los casos del rango consultado
public record DtoReporteEstadistico(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        long totalCasos,
        long casosResueltos,
        double diasPromedioAtencion,
        List<DtoConteo> porEstado,
        List<DtoConteo> porTipoCaso,
        List<DtoConteo> porDepartamento,
        List<DtoConteo> porCategoria
) {
}
