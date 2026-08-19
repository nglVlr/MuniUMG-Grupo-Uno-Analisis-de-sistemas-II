package gt.muni.quejas.exception;

import org.springframework.http.HttpStatus;

public enum CodigoError {
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "Sesión inválida o expirada"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"),
    USUARIO_NO_ENCONTRADO(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"),
    USUARIO_INACTIVO(HttpStatus.FORBIDDEN, "El usuario se encuentra inactivo"),
    USUARIO_BLOQUEADO(HttpStatus.FORBIDDEN, "El usuario se encuentra bloqueado, contacte al administrador"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso"),
    TOKEN_RECUPERACION_INVALIDO(HttpStatus.BAD_REQUEST, "El enlace de recuperación no es válido o ha expirado"),
    VALIDACION(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");

    private final HttpStatus httpStatus;
    private final String mensajePorDefecto;

    CodigoError(HttpStatus httpStatus, String mensajePorDefecto) {
        this.httpStatus = httpStatus;
        this.mensajePorDefecto = mensajePorDefecto;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMensajePorDefecto() {
        return mensajePorDefecto;
    }
}
