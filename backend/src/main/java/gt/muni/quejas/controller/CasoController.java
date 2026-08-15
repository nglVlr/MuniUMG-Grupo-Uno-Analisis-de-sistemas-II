package gt.muni.quejas.controller;

import gt.muni.quejas.dto.ReasignacionRequest;
import gt.muni.quejas.dto.RegistroCasoRequest;
import gt.muni.quejas.model.*;
import gt.muni.quejas.repository.*;
import gt.muni.quejas.service.AsignacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CU-02 Registro de Caso, CU-03 Consulta de Estado, y el endpoint de
 * reasignacion manual entre actores (nuevo requerimiento).
 * Implementacion deliberadamente simple: sin capa de servicio extra para
 * el registro, la logica vive directo en el controlador.
 */
@RestController
@RequestMapping("/api/casos")
public class CasoController {

    private final CasoRepository casoRepository;
    private final CiudadanoRepository ciudadanoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionService asignacionService;

    public CasoController(CasoRepository casoRepository,
                           CiudadanoRepository ciudadanoRepository,
                           CategoriaRepository categoriaRepository,
                           UsuarioRepository usuarioRepository,
                           AsignacionService asignacionService) {
        this.casoRepository = casoRepository;
        this.ciudadanoRepository = ciudadanoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionService = asignacionService;
    }

    // CU-02, paso 4/FA02: buscar ciudadano por documento antes de completar el resto del formulario
    @GetMapping("/ciudadano")
    public ResponseEntity<Ciudadano> buscarCiudadano(@RequestParam TipoDocumento tipoDocumento,
                                                       @RequestParam String numeroDocumento) {
        return ciudadanoRepository.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()); // AN02 CU-02 No.8
    }

    @PostMapping
    public ResponseEntity<Caso> registrar(@Valid @RequestBody RegistroCasoRequest req) {
        Ciudadano ciudadano = ciudadanoRepository
                .findByTipoDocumentoAndNumeroDocumento(req.getTipoDocumento(), req.getNumeroDocumento())
                .orElseThrow(() -> new IllegalArgumentException("Ciudadano no encontrado")); // AN02 CU-02 No.8

        Categoria categoria = categoriaRepository.findById(req.getIdCategoria())
                .orElseThrow(() -> new IllegalArgumentException("Categoria invalida"));

        Caso caso = new Caso();
        caso.setCodigoSeguimiento(generarCodigoSeguimiento());
        caso.setTipoCaso(req.getTipoCaso());
        caso.setCiudadano(ciudadano);
        caso.setCategoria(categoria);
        caso.setDepartamento(categoria.getDepartamento()); // RN-CU02-03: el sistema fija el departamento
        caso.setDireccionProblema(req.getDireccionProblema());
        caso.setDescripcion(req.getDescripcion());
        caso.setEstado(EstadoCaso.REGISTRADO);
        caso.setFechaRegistro(LocalDateTime.now());

        return ResponseEntity.ok(casoRepository.save(caso));
    }

    // CU-03: consulta publica de estado con el codigo de seguimiento
    @GetMapping("/seguimiento/{codigo}")
    public ResponseEntity<Caso> consultarPorCodigo(@PathVariable String codigo) {
        return casoRepository.findByCodigoSeguimiento(codigo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()); // AN02 CU-03 No.2
    }

    /**
     * Reasignacion manual de un caso entre actores (nuevo requerimiento). Valida
     * disponibilidad del nuevo responsable (no VACACIONES/PERMISO/INACTIVO/BLOQUEADO)
     * antes de reasignar.
     */
    @PutMapping("/{id}/reasignar")
    public ResponseEntity<Caso> reasignar(@PathVariable Long id, @Valid @RequestBody ReasignacionRequest req) {
        Caso caso = casoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caso no encontrado"));
        Usuario nuevoResponsable = usuarioRepository.findById(req.getIdNuevoResponsable())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        asignacionService.reasignarCaso(caso, nuevoResponsable);
        return ResponseEntity.ok(caso);
    }

    private String generarCodigoSeguimiento() {
        return "SQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
