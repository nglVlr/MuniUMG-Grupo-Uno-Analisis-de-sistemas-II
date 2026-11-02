package gt.muni.quejas.dto;

import java.time.LocalDate;

// CU-19 Dashboard de gráficas: los 10 filtros que llegan de la pantalla del Admin.
// modoFecha indica si el rango se aplica por fecha de registro o por fecha de resolución.
public record DtoFiltrosDashboard(
        String modoFecha,
        LocalDate desde,
        LocalDate hasta,
        Long idDepartamento,
        String tipoCaso,
        Long idCategoria,
        String estado,
        String cuiTecnico,
        Long idMotivoRechazo,
        String cargo,
        String cuiUsuario,
        String dpiCiudadano
) {
    public static final String MODO_REGISTRO = "registro";
    public static final String MODO_RESOLUCION = "resolucion";
}
