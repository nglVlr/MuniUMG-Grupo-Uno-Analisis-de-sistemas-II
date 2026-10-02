package gt.muni.quejas.dto;

import java.time.LocalDateTime;

public record DtoCasoBandeja(
        Long id,
        String codigoSeguimiento,
        LocalDateTime fechaIngreso,
        String tipoCaso,
        String categoria,
        String estado
) {
}
