package gt.muni.quejas.dto;

// CU-12 paso 2: datos propios del usuario. Solo el nombre es editable.
public record DtoMiCuenta(
        String cui,
        String nombre,
        String correo,
        String rol,
        String departamento
) {
}
