package gt.muni.quejas.controller;

import gt.muni.quejas.model.Categoria;
import gt.muni.quejas.repository.CategoriaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CU-02 paso 2 / CU-11: catalogo de categorias (cacheable en el front). */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public List<Categoria> listar(@RequestParam(required = false) String tipoCaso) {
        List<Categoria> todas = categoriaRepository.findAll();
        if (tipoCaso == null || tipoCaso.isBlank()) {
            return todas;
        }
        // Filtra por tipo de caso (queja/denuncia/sugerencia) segun tiposPermitidos;
        // si una categoria no restringe tipos, aplica para cualquiera.
        return todas.stream()
                .filter(c -> c.getTiposPermitidos() == null
                        || c.getTiposPermitidos().isBlank()
                        || c.getTiposPermitidos().contains(tipoCaso.toUpperCase()))
                .toList();
    }
}
