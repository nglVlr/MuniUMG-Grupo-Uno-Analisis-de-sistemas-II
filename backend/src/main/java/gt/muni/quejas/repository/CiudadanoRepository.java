package gt.muni.quejas.repository;

import gt.muni.quejas.model.Ciudadano;
import gt.muni.quejas.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CiudadanoRepository extends JpaRepository<Ciudadano, Long> {

    // FA02 del CU-02: busqueda del ciudadano en la base precargada por su documento
    Optional<Ciudadano> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
