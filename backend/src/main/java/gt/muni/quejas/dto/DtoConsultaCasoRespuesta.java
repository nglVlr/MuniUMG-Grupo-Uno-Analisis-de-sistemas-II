package gt.muni.quejas.dto;

import java.time.LocalDateTime;

public record DtoConsultaCasoRespuesta(
        String codigoSeguimiento,
        String estado,
        LocalDateTime fechaRegistro,
        String departamentoResponsable,
        String descripcion,
        String mensaje
) {
}