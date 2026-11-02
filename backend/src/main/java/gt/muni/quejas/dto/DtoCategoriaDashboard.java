package gt.muni.quejas.dto;

import java.util.List;

// CU-19: la categoría del filtro depende del departamento y del tipo de caso,
// así que la pantalla necesita saber a qué departamento y a qué tipos pertenece.
public record DtoCategoriaDashboard(Long id, String nombre, Long idDepartamento, List<String> tiposCaso) {
}
