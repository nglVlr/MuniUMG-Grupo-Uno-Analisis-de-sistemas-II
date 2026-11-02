package gt.muni.quejas.dto;

import java.util.List;

// CU-19: mapa de calor completo, con sus etiquetas y sus celdas.
public record DtoMapaCalor(List<String> filas, List<String> columnas, List<DtoCeldaCalor> celdas) {
}
