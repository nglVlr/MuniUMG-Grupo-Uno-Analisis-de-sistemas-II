package gt.muni.quejas.dto;

// CU-19: punto de la dispersión o burbuja. x es la carga del Técnico,
// y el tiempo promedio de resolución y tamano la cantidad de casos resueltos.
public record DtoPuntoDispersion(String etiqueta, double x, double y, double tamano) {
}
