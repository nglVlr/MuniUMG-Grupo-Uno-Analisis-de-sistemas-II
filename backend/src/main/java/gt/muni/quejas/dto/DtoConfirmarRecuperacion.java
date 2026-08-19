package gt.muni.quejas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DtoConfirmarRecuperacion(
        @NotBlank(message = "El token es obligatorio")
        String token,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,
        @NotBlank(message = "La confirmación es obligatoria")
        String confirmarPassword
) {
}
