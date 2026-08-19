package gt.muni.quejas.dto;

import gt.muni.quejas.exception.DetalleError;

import java.time.LocalDateTime;
import java.util.List;

public record DtoError(LocalDateTime timestamp,
                       int status,
                       String codigoError,
                       String mensaje,
                       String path,
                       List<DetalleError> detalles
) {

    public static DtoError of(int status, String codigoError, String mensaje, String path) {
        return new DtoError(LocalDateTime.now(), status, codigoError, mensaje, path, List.of());
    }

    public static DtoError of(int status, String codigoError, String mensaje, String path, List<DetalleError> detalles) {
        return new DtoError(LocalDateTime.now(), status, codigoError, mensaje, path, detalles);
    }
}