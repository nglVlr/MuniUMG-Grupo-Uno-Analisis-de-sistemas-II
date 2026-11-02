package gt.muni.quejas.dto;

import java.util.List;

// CU-19 Dashboard de gráficas: resultado único del endpoint. Los filtros se aplican
// una sola vez y de ese mismo resultado salen todas las gráficas de la pantalla.
// forma indica qué estructura tiene la serie principal, para que la pantalla
// deshabilite los tipos de gráfica que no la aceptan.
public record DtoDashboardRespuesta(
        String metrica,
        String agrupacion,
        String forma,
        String formato,
        String nombreDesglose,
        boolean sinResultados,
        long totalCasos,
        List<String> categorias,
        List<DtoSerieDashboard> series,
        List<DtoSerieDashboard> seriesDesglosadas,
        List<DtoIndicadorTarjeta> indicadores,
        List<DtoConteo> embudo,
        List<DtoEnlaceFlujo> sankey,
        List<DtoNodoJerarquia> treemap,
        List<DtoNodoJerarquia> sunburst,
        List<DtoNodoJerarquia> arbol,
        List<DtoConteo> histograma,
        List<DtoConteo> distribucionDias,
        double mediaDias,
        double desviacionDias,
        List<DtoCajaTecnico> boxplot,
        List<DtoPuntoDispersion> dispersion,
        DtoMapaCalor calorDiaHora,
        DtoMapaCalor calorCategoriaMes,
        List<DtoPuntoCalendario> calendario,
        List<DtoPuntoPareto> pareto,
        List<DtoConteo> cascada,
        List<DtoConteo> acumuladoAbiertos,
        List<String> mesesCombinada,
        List<Double> recibidosPorMes,
        List<Double> tiempoPorMes,
        List<DtoFilaTecnico> tablaTecnicos,
        List<DtoConteo> rankingCiudadanos,
        List<DtoConteo> rechazadosCiudadanos,
        List<DtoConteo> rechazoTramiteCiudadanos,
        String casoTrazabilidad,
        List<DtoHistorialCaso> trazabilidad
) {
}
