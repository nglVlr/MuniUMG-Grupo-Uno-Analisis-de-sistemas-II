package gt.muni.quejas.dto;

// CU-10: fila del listado de usuarios internos
public record DtoUsuarioAdmin(
        String cui,
        String nombre,
        String correo,
        Long idRol,
        String rol,
        Long idDepartamento,
        String departamento,
        Long idEstado,
        String estado,
        int casosActivos
) {
}
