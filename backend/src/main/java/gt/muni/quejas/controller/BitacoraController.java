package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoPaginaBitacora;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.BitacoraConsultaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

// CU-15 Bitácora de actividades: solo consulta, exclusiva del Admin
@RestController
@RequestMapping("api/bitacora")
public class BitacoraController {

    private final BitacoraConsultaService bitacoraConsultaService;

    public BitacoraController(BitacoraConsultaService bitacoraConsultaService) {
        this.bitacoraConsultaService = bitacoraConsultaService;
    }

    @GetMapping
    public ResponseEntity<DtoPaginaBitacora> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "0") int pagina,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(bitacoraConsultaService.consultar(
                desde, hasta, usuario, accion, entidad, rol, pagina, admin));
    }

    @GetMapping("/acciones")
    public ResponseEntity<List<String>> listarAcciones(@AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(bitacoraConsultaService.listarAcciones(admin));
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String rol,
            @RequestParam(defaultValue = "0") int pagina,
            @AuthenticationPrincipal Usuario admin) {
        DtoPaginaBitacora resultado = bitacoraConsultaService.consultar(
                desde, hasta, usuario, accion, entidad, rol, pagina, admin);
        byte[] contenido = bitacoraConsultaService.exportarCsv(resultado).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bitacora.csv\"")
                .body(contenido);
    }
}
