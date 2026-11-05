package gt.muni.quejas.dto;

import java.util.List;

// Dashboard 2: lo que llena los selectores de la pantalla.
public record DtoFiltros2(
        List<DtoOpcionCatalogo> departamentos,
        List<String> tiposCaso,
        List<DtoCategoria2> categorias
) {
}
