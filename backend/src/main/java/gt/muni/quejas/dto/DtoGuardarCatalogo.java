package gt.muni.quejas.dto;

import java.util.List;

// CU-11 paso 5: datos del elemento que el Admin agrega o edita
public record DtoGuardarCatalogo(
        String nombre,
        Boolean activo,
        Long idDepartamento,
        List<String> tiposCaso
) {
}
