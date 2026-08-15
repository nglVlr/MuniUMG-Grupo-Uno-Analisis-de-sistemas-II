package gt.muni.quejas.dto;

import gt.muni.quejas.model.TipoCaso;
import gt.muni.quejas.model.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Datos que envia el portal ciudadano al registrar un caso (CU-02). */
@Getter
@Setter
public class RegistroCasoRequest {

    @NotNull
    private TipoDocumento tipoDocumento;

    @NotBlank
    private String numeroDocumento;

    @NotNull
    private TipoCaso tipoCaso;

    @NotNull
    private Long idCategoria;

    @NotBlank
    private String direccionProblema;

    @NotBlank
    private String descripcion;
}
