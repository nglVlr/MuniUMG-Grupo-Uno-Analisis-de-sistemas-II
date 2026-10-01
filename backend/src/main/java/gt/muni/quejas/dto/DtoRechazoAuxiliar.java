package gt.muni.quejas.dto;

public record DtoRechazoAuxiliar(
        Long idMotivoRechazo,
        String comentario,
        Boolean aplicaAmpliacion
) {
}