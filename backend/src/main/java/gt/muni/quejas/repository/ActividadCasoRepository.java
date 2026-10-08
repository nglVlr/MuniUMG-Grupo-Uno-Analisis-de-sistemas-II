package gt.muni.quejas.repository;

import gt.muni.quejas.model.ActividadCaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActividadCasoRepository extends JpaRepository<ActividadCaso, Long> {

    List<ActividadCaso> findByCasoIdOrderByFechaAsc(Long idCaso);
}
