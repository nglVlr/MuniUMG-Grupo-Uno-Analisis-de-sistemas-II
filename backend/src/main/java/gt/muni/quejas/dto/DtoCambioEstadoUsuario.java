package gt.muni.quejas.dto;

// CU-10 FA04: cambio del estado de disponibilidad del usuario.
// FA08: cuiDestino llega solo cuando el usuario tiene casos pendientes y hay que reasignarlos.
public record DtoCambioEstadoUsuario(Long idEstado, String cuiDestino) {
}
