package gt.muni.quejas.exception;

import gt.muni.quejas.dto.DtoError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<DtoError> manejarNegocioException(NegocioException ex, HttpServletRequest request) {
        CodigoError codigo = ex.getCodigoError();
        DtoError error = DtoError.of(
                codigo.getHttpStatus().value(),
                codigo.name(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(codigo.getHttpStatus()).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<DtoError> manejarBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                CodigoError.CREDENCIALES_INVALIDAS.getHttpStatus().value(),
                CodigoError.CREDENCIALES_INVALIDAS.name(),
                CodigoError.CREDENCIALES_INVALIDAS.getMensajePorDefecto(),
                request.getRequestURI()
        );
        return ResponseEntity.status(CodigoError.CREDENCIALES_INVALIDAS.getHttpStatus()).body(error);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<DtoError> manejarDisabled(DisabledException ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                CodigoError.USUARIO_INACTIVO.getHttpStatus().value(),
                CodigoError.USUARIO_INACTIVO.name(),
                CodigoError.USUARIO_INACTIVO.getMensajePorDefecto(),
                request.getRequestURI()
        );
        return ResponseEntity.status(CodigoError.USUARIO_INACTIVO.getHttpStatus()).body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<DtoError> manejarLocked(LockedException ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                CodigoError.USUARIO_BLOQUEADO.getHttpStatus().value(),
                CodigoError.USUARIO_BLOQUEADO.name(),
                CodigoError.USUARIO_BLOQUEADO.getMensajePorDefecto(),
                request.getRequestURI()
        );
        return ResponseEntity.status(CodigoError.USUARIO_BLOQUEADO.getHttpStatus()).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<DtoError> manejarAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                CodigoError.CREDENCIALES_INVALIDAS.getHttpStatus().value(),
                CodigoError.CREDENCIALES_INVALIDAS.name(),
                CodigoError.CREDENCIALES_INVALIDAS.getMensajePorDefecto(),
                request.getRequestURI()
        );
        return ResponseEntity.status(CodigoError.CREDENCIALES_INVALIDAS.getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DtoError> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<DetalleError> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new DetalleError(fe.getField(), mensajeDeFieldError(fe)))
                .toList();

        DtoError error = DtoError.of(
                CodigoError.VALIDACION.getHttpStatus().value(),
                CodigoError.VALIDACION.name(),
                CodigoError.VALIDACION.getMensajePorDefecto(),
                request.getRequestURI(),
                detalles
        );
        return ResponseEntity.status(CodigoError.VALIDACION.getHttpStatus()).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DtoError> manejarJsonMalFormado(HttpMessageNotReadableException ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                HttpStatus.BAD_REQUEST.value(),
                CodigoError.VALIDACION.name(),
                "El cuerpo de la petición no tiene un formato válido",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DtoError> manejarExcepcionGenerica(Exception ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                CodigoError.ERROR_INTERNO.getHttpStatus().value(),
                CodigoError.ERROR_INTERNO.name(),
                CodigoError.ERROR_INTERNO.getMensajePorDefecto(),
                request.getRequestURI()
        );
        return ResponseEntity.status(CodigoError.ERROR_INTERNO.getHttpStatus()).body(error);
    }

    private String mensajeDeFieldError(FieldError fieldError) {
        String mensaje = fieldError.getDefaultMessage();
        return mensaje != null ? mensaje : "Valor inválido";
    }
}
