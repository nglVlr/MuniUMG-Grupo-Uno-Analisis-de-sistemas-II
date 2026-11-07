package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCampanaGauss;
import gt.muni.quejas.dto.DtoDispersionTecnico;
import gt.muni.quejas.dto.DtoFiltros2;
import gt.muni.quejas.dto.DtoValorEtiqueta;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.Dashboard2Service;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard 2: una direccion por cada grafica, para que se entienda de un vistazo.
 *
 *     GET /api/dashboard2/barras       casos por estado
 *     GET /api/dashboard2/pastel       casos por tipo de caso
 *     GET /api/dashboard2/dispersion   casos del Tecnico contra sus dias promedio
 *     GET /api/dashboard2/campana      dias de resolucion, con media y desviacion
 *
 * Las cuatro reciben los mismos cuatro filtros. La fecha es obligatoria.
 * Departamento, tipo y categoria son opcionales: si no se mandan, la consulta
 * los recibe en NULL y no filtra por ellos. El conteo usa la fecha de registro
 * del caso. Dispersion y campana miden los dias hasta la accion
 * "Firma de oficio de resolución" en la bitacora.
 */
@RestController
@RequestMapping("api/dashboard2")
public class Dashboard2Controller {

    private final Dashboard2Service dashboard2Service;

    public Dashboard2Controller(Dashboard2Service dashboard2Service) {
        this.dashboard2Service = dashboard2Service;
    }

    // Lo que llena los selectores de departamento, tipo de caso y categoria
    @GetMapping("/filtros")
    public ResponseEntity<DtoFiltros2> filtros(@AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(dashboard2Service.filtros(admin));
    }

    @GetMapping("/barras")
    public ResponseEntity<List<DtoValorEtiqueta>> barras(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) Long idCategoria,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(dashboard2Service.casosPorEstado(
                desde, hasta, idDepartamento, tipoCaso, idCategoria, admin));
    }

    @GetMapping("/pastel")
    public ResponseEntity<List<DtoValorEtiqueta>> pastel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) Long idCategoria,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(dashboard2Service.casosPorTipo(
                desde, hasta, idDepartamento, tipoCaso, idCategoria, admin));
    }

    @GetMapping("/dispersion")
    public ResponseEntity<List<DtoDispersionTecnico>> dispersion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) Long idCategoria,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(dashboard2Service.casosPorTecnico(
                desde, hasta, idDepartamento, tipoCaso, idCategoria, admin));
    }

    @GetMapping("/campana")
    public ResponseEntity<DtoCampanaGauss> campana(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long idDepartamento,
            @RequestParam(required = false) String tipoCaso,
            @RequestParam(required = false) Long idCategoria,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.ok(dashboard2Service.campanaDeGauss(
                desde, hasta, idDepartamento, tipoCaso, idCategoria, admin));
    }
}
