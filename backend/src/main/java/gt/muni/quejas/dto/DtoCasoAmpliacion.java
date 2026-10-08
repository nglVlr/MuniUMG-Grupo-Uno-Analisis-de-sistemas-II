package gt.muni.quejas.dto;

import java.time.LocalDateTime;

// CU-18 paso 7: el caso y las instrucciones de ampliación que dejó el Auxiliar
public record DtoCasoAmpliacion(
        Long id,
        String codigoSeguimiento,
        LocalDateTime fechaIngreso,
        String tipoCaso,
        String categoria,
        String descripcion,
        String direccion,
        String estado,
        String motivoRechazo,
        String instruccionesAmpliacion,
        String informacionAmpliacion
) {
}
