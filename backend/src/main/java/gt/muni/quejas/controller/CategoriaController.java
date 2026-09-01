package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoCategoria;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.service.CatalogoCacheService;
import gt.muni.quejas.util.TipoCaso;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/categorias")
public class CategoriaController {

    private final CatalogoCacheService catalogoCacheService;

    public CategoriaController(CatalogoCacheService catalogoCacheService) {
        this.catalogoCacheService = catalogoCacheService;
    }

    @GetMapping
    public ResponseEntity<List<DtoCategoria>> listar(@RequestParam String tipoCaso) {
        if (!TipoCaso.esValido(tipoCaso)) {
            throw new NegocioException(CodigoError.TIPO_CASO_REQUERIDO);
        }
        return ResponseEntity.ok(catalogoCacheService.listarCategorias(tipoCaso));
    }
}
