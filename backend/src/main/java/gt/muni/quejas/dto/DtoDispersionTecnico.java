package gt.muni.quejas.dto;

// Dashboard 2: un punto de la grafica de dispersion.
// x es la cantidad de casos del Tecnico y y sus dias promedio de resolucion.
public record DtoDispersionTecnico(String tecnico, double cantidadCasos, double diasPromedio) {
}
