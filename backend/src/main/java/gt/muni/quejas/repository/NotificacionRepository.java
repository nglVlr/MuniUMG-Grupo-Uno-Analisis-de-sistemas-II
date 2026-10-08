package gt.muni.quejas.repository;

import gt.muni.quejas.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByCasoIdOrderByIdAsc(Long idCaso);
}
