package gt.muni.quejas.dto;

import java.util.List;

// CU-19: nodo de treemap, sunburst o árbol. Las hojas llevan la lista de hijos vacía.
public record DtoNodoJerarquia(String nombre, double valor, List<DtoNodoJerarquia> hijos) {
}
