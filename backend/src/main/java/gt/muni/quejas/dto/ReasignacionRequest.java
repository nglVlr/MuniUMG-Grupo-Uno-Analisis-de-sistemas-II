package gt.muni.quejas.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Reasignacion manual de un caso a otro usuario interno. */
@Getter
@Setter
public class ReasignacionRequest {

    @NotNull
    private Long idNuevoResponsable;

    private String motivo;
}
