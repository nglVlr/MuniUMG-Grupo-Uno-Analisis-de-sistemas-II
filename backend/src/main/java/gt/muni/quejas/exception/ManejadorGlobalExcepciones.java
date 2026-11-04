package gt.muni.quejas.exception;

import gt.muni.quejas.dto.DtoError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {
    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

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

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<DtoError> manejarArchivoGrande(Exception ex, HttpServletRequest request) {
        DtoError error = DtoError.of(
                CodigoError.EVIDENCIA_TAMANO.getHttpStatus().value(),
                CodigoError.EVIDENCIA_TAMANO.name(),
                CodigoError.EVIDENCIA_TAMANO.getMensajePorDefecto(),
                request.getRequestURI()
        );
        return ResponseEntity.status(CodigoError.EVIDENCIA_TAMANO.getHttpStatus()).body(error);
    }

    // Un metodo HTTP que el endpoint no soporta es 405, no un error interno
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DtoError> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        DtoError error = DtoError.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METODO_NO_PERMITIDO",
                "La operación solicitada no está permitida sobre este recurso",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    // Un id de categoría que no es número, o un parámetro obligatorio que no viaja,
    // es un formulario incompleto: se responde 400 y no un error interno.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DtoError> manejarParametroFaltante(MissingServletRequestParameterException ex,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(CodigoError.CAMPOS_OBLIGATORIOS.getHttpStatus()).body(DtoError.of(
                CodigoError.CAMPOS_OBLIGATORIOS.getHttpStatus().value(),
                CodigoError.CAMPOS_OBLIGATORIOS.name(),
                CodigoError.CAMPOS_OBLIGATORIOS.getMensajePorDefecto(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<DtoError> manejarParteFaltante(MissingServletRequestPartException ex,
                                                         HttpServletRequest request) {
        boolean reporte = "detalle".equals(ex.getRequestPartName()) || "soporte".equals(ex.getRequestPartName());
        CodigoError codigo = reporte ? CodigoError.REPORTE_ATENCION_INCOMPLETO : CodigoError.CAMPOS_OBLIGATORIOS;
        return ResponseEntity.status(codigo.getHttpStatus()).body(DtoError.of(
                codigo.getHttpStatus().value(),
                codigo.name(),
                codigo.getMensajePorDefecto(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DtoError> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex,
                                                          HttpServletRequest request) {
        CodigoError codigo = "idCategoria".equals(ex.getName())
                ? CodigoError.CATEGORIA_REQUERIDA
                : CodigoError.CAMPOS_OBLIGATORIOS;
        return ResponseEntity.status(codigo.getHttpStatus()).body(DtoError.of(
                codigo.getHttpStatus().value(),
                codigo.name(),
                codigo.getMensajePorDefecto(),
                request.getRequestURI()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DtoError> manejarExcepcionGenerica(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
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
