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
    DPI_INVALIDO(HttpStatus.BAD_REQUEST, "El número de identificación ingresado no tiene un formato válido."),
    CIUDADANO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "No se encontró un ciudadano registrado con el número de identificación ingresado."),
    CODIGO_VERIFICACION_INVALIDO(HttpStatus.BAD_REQUEST, "El código es inválido, intente de nuevo"),
    SESION_REGISTRO_INVALIDA(HttpStatus.BAD_REQUEST, "Debe completar la verificación de su DPI antes de registrar el caso."),
    CAMPOS_OBLIGATORIOS(HttpStatus.BAD_REQUEST, "Debe completar todos los campos obligatorios del formulario."),
    TIPO_CASO_REQUERIDO(HttpStatus.BAD_REQUEST, "Debe seleccionar el tipo de caso (queja, reclamo, denuncia o sugerencia)."),
    CATEGORIA_REQUERIDA(HttpStatus.BAD_REQUEST, "Debe seleccionar la categoría del problema reportado."),
    DIRECCION_REQUERIDA(HttpStatus.BAD_REQUEST, "Debe indicar la dirección o ubicación del problema reportado."),
    EVIDENCIA_TAMANO(HttpStatus.BAD_REQUEST, "El archivo adjunto supera el tamaño máximo permitido de 10MB."),
    EVIDENCIA_FORMATO(HttpStatus.BAD_REQUEST, "El archivo debe ser un PDF o una imagen (JPG, PNG o WEBP)."),
    SOPORTE_FORMATO(HttpStatus.BAD_REQUEST, "El soporte debe ser un archivo PDF."),
    EVIDENCIA_VACIA(HttpStatus.BAD_REQUEST, "El archivo no debe estar vacío."),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado"),
    CODIGO_SEGUIMIENTO_REQUERIDO(HttpStatus.BAD_REQUEST, "Debe completar todos los campos obligatorios del formulario."),
    CASO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El código es inválido, intente de nuevo."),
    CASO_NO_ENCONTRADO_AUXILIAR(HttpStatus.NOT_FOUND, "No se encontró un caso con el código o número ingresado."),
    TECNICO_NO_DISPONIBLE(HttpStatus.BAD_REQUEST, "No existe un Técnico activo en el departamento para asignar el caso."),
    REPORTE_ATENCION_INCOMPLETO(HttpStatus.BAD_REQUEST, "El reporte de atención no contiene toda la información obligatoria."),
    USUARIO_CON_CASOS_ACTIVOS(HttpStatus.BAD_REQUEST, "El usuario tiene casos activos asignados. Debe reasignarlos antes de continuar."),
    SIN_USUARIO_DESTINO(HttpStatus.BAD_REQUEST, "No existe otro Técnico activo en el departamento que pueda recibir los casos."),
    USUARIO_DUPLICADO(HttpStatus.BAD_REQUEST, "Ya existe un usuario registrado con ese CUI o correo electrónico."),
    DATOS_USUARIO_INVALIDOS(HttpStatus.BAD_REQUEST, "Los datos ingresados no son válidos, o las contraseñas no coinciden."),
    ELEMENTO_EN_USO(HttpStatus.BAD_REQUEST, "El elemento no puede eliminarse porque se encuentra en uso; puede inactivarlo."),
    ELEMENTO_DUPLICADO(HttpStatus.BAD_REQUEST, "Ya existe un elemento con ese nombre en el catálogo."),
    SIN_CASOS_AMPLIACION(HttpStatus.NOT_FOUND, "No existen casos que requieran ampliación para el número de identificación ingresado."),
    INFORMACION_AMPLIACION_REQUERIDA(HttpStatus.BAD_REQUEST, "Debe completar la información obligatoria solicitada."),
    RANGO_FECHAS_INVALIDO(HttpStatus.BAD_REQUEST, "El rango de fechas ingresado no es válido."),
    SIN_REGISTROS_FILTROS(HttpStatus.NOT_FOUND, "No existen registros para los filtros aplicados.");


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
