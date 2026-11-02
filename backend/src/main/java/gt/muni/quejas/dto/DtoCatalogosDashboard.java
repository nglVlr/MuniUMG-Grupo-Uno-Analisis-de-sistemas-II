package gt.muni.quejas.dto;

import java.util.List;

// CU-19: todo lo que alimenta el panel de filtros del dashboard.
public record DtoCatalogosDashboard(
        List<DtoOpcionCatalogo> departamentos,
        List<DtoCategoriaDashboard> categorias,
        List<String> tiposCaso,
        List<String> estados,
        List<DtoUsuarioDashboard> tecnicos,
        List<DtoUsuarioDashboard> usuarios,
        List<DtoOpcionCatalogo> motivosRechazo,
        List<String> cargos
) {
}
