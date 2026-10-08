package gt.muni.quejas.dto;

import java.time.LocalDateTime;

// RN19 Campos de Formulario Actividades Técnico
public record DtoActividad(
        String codigoSeguimiento,
        Long codigoActividad,
        String detalle,
        LocalDateTime fecha
) {
}
