package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoConfirmarRecuperacion;
import gt.muni.quejas.dto.DtoSolicitudRecuperacion;
import gt.muni.quejas.service.RecuperacionPasswordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/password")
public class RecuperacionPasswordController {
    private final RecuperacionPasswordService recuperacionPasswordService;

    public RecuperacionPasswordController(RecuperacionPasswordService recuperacionPasswordService) {
        this.recuperacionPasswordService = recuperacionPasswordService;
    }

    @PostMapping("/olvide")
    public ResponseEntity<String> solicitarRecuperacion(@Valid @RequestBody DtoSolicitudRecuperacion dto) {
        recuperacionPasswordService.solicitarRecuperacion(dto);
        return ResponseEntity.ok("Si el correo existe en el sistema, se ha enviado un enlace de recuperación.");
    }

    @PostMapping("/restablecer")
    public ResponseEntity<String> confirmarRecuperacion(@Valid @RequestBody DtoConfirmarRecuperacion dto) {
        recuperacionPasswordService.confirmarRecuperacion(dto);
        return ResponseEntity.ok("Tu contraseña ha sido actualizada correctamente.");
    }
}
