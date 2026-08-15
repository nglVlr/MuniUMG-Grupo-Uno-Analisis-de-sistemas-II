package gt.muni.quejas.model;

/**
 * Estado del usuario interno. Se usa tanto para control de acceso (RN-CU01-01/03)
 * como para saber su DISPONIBILIDAD real a la hora de asignar o reasignar un caso:
 * un usuario en VACACIONES o PERMISO no debe recibir casos nuevos aunque este ACTIVO
 * para efectos de login.
 */
public enum EstadoUsuario {
    ACTIVO,
    INACTIVO,
    BLOQUEADO,
    VACACIONES,
    PERMISO
}
