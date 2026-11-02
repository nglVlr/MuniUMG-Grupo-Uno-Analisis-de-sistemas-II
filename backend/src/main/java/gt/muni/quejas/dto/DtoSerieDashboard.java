package gt.muni.quejas.dto;

import java.util.List;

// CU-19: una serie de la gráfica. Los valores van en el mismo orden que las categorías.
public record DtoSerieDashboard(String nombre, List<Double> valores) {
}
