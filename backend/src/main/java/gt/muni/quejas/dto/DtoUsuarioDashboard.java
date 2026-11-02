package gt.muni.quejas.dto;

// CU-19: usuario para los filtros de Técnico y de usuario. El filtro de Técnico
// depende del departamento, por eso se envía el departamento de cada uno.
public record DtoUsuarioDashboard(String cui, String nombre, String rol, Long idDepartamento) {
}
