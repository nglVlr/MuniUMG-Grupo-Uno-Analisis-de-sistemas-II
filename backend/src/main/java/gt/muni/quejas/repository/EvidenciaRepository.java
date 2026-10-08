package gt.muni.quejas.repository;

import gt.muni.quejas.model.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {

    boolean existsByCasoId(Long idCaso);

    Optional<Evidencia> findFirstByCasoId(Long idCaso);

    boolean existsByCasoIdAndTipo(Long idCaso, String tipo);

    Optional<Evidencia> findFirstByCasoIdAndTipo(Long idCaso, String tipo);

    List<Evidencia> findByCasoIdOrderByIdAsc(Long idCaso);
}
