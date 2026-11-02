package gt.muni.quejas.dto;

// CU-19: resumen de los días de resolución de un Técnico, para el diagrama de caja.
public record DtoCajaTecnico(
        String tecnico,
        double minimo,
        double cuartilUno,
        double mediana,
        double cuartilTres,
        double maximo
) {
}
