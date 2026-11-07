package gt.muni.quejas.dto;

import java.util.List;

// Dashboard 2: la campana de Gauss. tramos son las barras (dias de resolucion
// agrupados de dos en dos) y con la media y la desviacion se dibuja la curva.
public record DtoCampanaGauss(List<DtoValorEtiqueta> tramos, double media, double desviacion) {
}
