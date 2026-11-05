package gt.muni.quejas.dto;

import java.util.List;

// Dashboard 2: una categoria del selector. Lleva su departamento y los tipos de
// caso que admite, para que la pantalla muestre solo las que corresponden.
public record DtoCategoria2(Long id, String nombre, Long idDepartamento, List<String> tiposCaso) {
}
