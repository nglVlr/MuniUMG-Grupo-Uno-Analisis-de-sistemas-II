package gt.muni.quejas.dto;

import java.time.LocalDateTime;
import java.util.List;

// CU-14: trazabilidad de un caso (antes CU-13), con su historial completo
public record DtoTrazabilidadCaso(
        Long id,
        String codigoSeguimiento,
        LocalDateTime fechaIngreso,
        String tipoCaso,
        String categoria,
        String departamento,
        String estado,
        String nombreCiudadano,
        String nombreTecnico,
        List<DtoHistorialCaso> historial
) {
}
