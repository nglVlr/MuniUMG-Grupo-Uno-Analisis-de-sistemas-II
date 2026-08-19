package gt.muni.quejas.dto;

public record DtoJwtRespuesta(
        String jwtToken,
        String correo,
        String nombreUsuario,
        Integer rol,
        String cui) {
}
