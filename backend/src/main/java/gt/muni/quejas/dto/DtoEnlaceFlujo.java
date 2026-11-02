package gt.muni.quejas.dto;

// CU-19: transición de un estado a otro, para el diagrama de Sankey.
public record DtoEnlaceFlujo(String origen, String destino, long valor) {
}
