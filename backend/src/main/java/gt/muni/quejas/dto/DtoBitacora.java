package gt.muni.quejas.dto;

import java.time.LocalDate;
import java.time.LocalTime;

// CU-15: una fila de la bitácora de actividades
public record DtoBitacora(
        Long id,
        String accion,
        String usuario,
        String rol,
        String entidad,
        String codigoSeguimiento,
        LocalDate fecha,
        LocalTime hora,
        String ipOrigen,
        String detalle,
        String valorAnterior,
        String valorNuevo
) {
}
