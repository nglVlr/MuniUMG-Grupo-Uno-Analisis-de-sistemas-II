package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCatalogosDashboard;
import gt.muni.quejas.dto.DtoDashboardRespuesta;
import gt.muni.quejas.dto.DtoFiltrosDashboard;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// CU-19 Dashboard de gráficas: pantalla aparte, exclusiva del Admin. Un solo endpoint
// genérico recibe la métrica, la agrupación y los 10 filtros, y devuelve las series
// listas para graficar.
@RestController
@RequestMapping("api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/catalogos")
    public ResponseEntity<DtoCatalogosDashboard> catalogos(@AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(dashboardService.catalogos(admin));
    }

    @GetMapping
    public ResponseEntity<DtoDashboardRespuesta> consultar(
            @RequestParam(defaultValue = "casos") String metrica,
            @RequestParam(defaultValue = "estado") String agrupacion,
            @RequestParam(defaultValue = "registro") String modoFecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String cuiTecnico,
            @RequestParam(required = false) Long idMotivoRechazo,
            @RequestParam(required = false) String cargo,
            @RequestParam(required = false) String cuiUsuario,
            @RequestParam(required = false) String dpiCiudadano,
            @RequestParam(required = false) String codigoSeguimiento,
            @AuthenticationPrincipal Usuario admin) {

        DtoFiltrosDashboard filtros = new DtoFiltrosDashboard(
                modoFecha, desde, hasta, idDepartamento, tipoCaso, idCategoria, estado,
                cuiTecnico, idMotivoRechazo, cargo, cuiUsuario, dpiCiudadano);

        return ResponseEntity.ok(dashboardService.consultar(
                filtros, metrica, agrupacion, codigoSeguimiento, admin));
    }
}
