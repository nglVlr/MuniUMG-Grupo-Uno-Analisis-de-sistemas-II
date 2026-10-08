package gt.muni.quejas.dto;

import java.time.LocalDate;
import java.time.LocalTime;

// Una linea del historial de cambios del caso, para el seguimiento
public record DtoHistorialCaso(
        String accion,
        String usuario,
        LocalDate fecha,
        LocalTime hora,
        String ipOrigen,
        String detalle
) {
}
