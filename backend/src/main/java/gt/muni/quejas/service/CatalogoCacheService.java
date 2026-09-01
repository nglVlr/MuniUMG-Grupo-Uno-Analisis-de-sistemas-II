package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCategoria;
import gt.muni.quejas.model.Categoria;
import gt.muni.quejas.model.Ciudadano;
import gt.muni.quejas.repository.CategoriaRepository;
import gt.muni.quejas.repository.CiudadanoRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogoCacheService {

    private final CategoriaRepository categoriaRepository;
    private final CiudadanoRepository ciudadanoRepository;

    public CatalogoCacheService(CategoriaRepository categoriaRepository,
                                CiudadanoRepository ciudadanoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.ciudadanoRepository = ciudadanoRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "catalogos", key = "#tipoCaso")
    public List<DtoCategoria> listarCategorias(String tipoCaso) {
        return categoriaRepository.findActivasConDepartamento().stream()
                .filter(categoria -> categoria.admiteTipo(tipoCaso))
                .map(categoria -> new DtoCategoria(
                        categoria.getId(),
                        categoria.getNombre(),
                        categoria.getDepartamento().getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ciudadanos", key = "#dpi")
    public Optional<Ciudadano> buscarCiudadano(String dpi) {
        return ciudadanoRepository.findById(dpi);
    }

    @Transactional(readOnly = true)
    public Optional<Categoria> obtenerCategoriaActiva(Long id) {
        return categoriaRepository.findByIdConCatalogo(id).filter(Categoria::isActivo);
    }
}
