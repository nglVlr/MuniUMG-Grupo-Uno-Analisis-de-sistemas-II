package gt.muni.quejas.dto;

// CU-19: celda del mapa de calor. fila y columna son posiciones dentro de las
// listas de etiquetas que trae el mapa.
public record DtoCeldaCalor(int fila, int columna, double valor) {
}
