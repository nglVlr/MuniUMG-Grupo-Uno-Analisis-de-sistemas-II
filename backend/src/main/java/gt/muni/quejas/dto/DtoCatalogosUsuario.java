package gt.muni.quejas.dto;

import java.util.List;

// CU-10 paso 4: listas que alimentan el formulario de usuario
public record DtoCatalogosUsuario(
        List<DtoOpcionCatalogo> roles,
        List<DtoOpcionCatalogo> departamentos,
        List<DtoOpcionCatalogo> estados
) {
}
