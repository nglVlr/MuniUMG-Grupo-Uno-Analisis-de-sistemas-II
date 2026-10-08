package gt.muni.quejas.dto;

// CU-12 FA01: cambio de contraseña
public record DtoCambioPassword(
        String passwordActual,
        String passwordNueva,
        String passwordConfirmacion
) {
}
