package gt.muni.quejas.dto;

import java.time.LocalDateTime;

public record DtoCasoDetalleAuxiliar(
        Long id,
        String codigoSeguimiento,
        LocalDateTime fechaIngreso,
        String tipoCaso,
        String categoria,
        String descripcion,
        String direccion,
        String estado,
        boolean tieneEvidencia
) {
}