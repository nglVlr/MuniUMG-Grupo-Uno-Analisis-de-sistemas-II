package gt.muni.quejas.dto;

import java.util.List;

// CU-11: un elemento de cualquiera de los tres catálogos administrables.
// Los campos de departamento y tipos de caso solo aplican a las categorías.
public record DtoCatalogoElemento(
        Long id,
        String nombre,
        boolean activo,
        Long idDepartamento,
        String departamento,
        List<String> tiposCaso,
        boolean enUso
) {
}
