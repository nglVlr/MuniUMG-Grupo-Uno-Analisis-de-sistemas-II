package gt.muni.quejas.dto;

// CU-19: barra del Pareto de motivos de rechazo, con su porcentaje acumulado.
public record DtoPuntoPareto(String etiqueta, long cantidad, double acumulado) {
}
