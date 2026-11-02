package gt.muni.quejas.dto;

import java.util.List;

// CU-19: tarjeta de indicador. chispa trae los últimos 12 meses para la sparkline
// y variacion el cambio porcentual contra el mes anterior.
public record DtoIndicadorTarjeta(
        String clave,
        String titulo,
        double valor,
        String formato,
        double variacion,
        List<Double> chispa
) {
}
