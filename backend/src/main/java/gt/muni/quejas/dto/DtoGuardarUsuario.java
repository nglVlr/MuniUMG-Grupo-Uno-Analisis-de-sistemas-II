package gt.muni.quejas.dto;

// CU-10 paso 4: campos del formulario de usuario.
// En edición la contraseña puede venir vacía, para dejar la que ya tenía.
public record DtoGuardarUsuario(
        String cui,
        String nombre,
        String correo,
        String password,
        Long idRol,
        Long idDepartamento,
        Long idEstado
) {
}
