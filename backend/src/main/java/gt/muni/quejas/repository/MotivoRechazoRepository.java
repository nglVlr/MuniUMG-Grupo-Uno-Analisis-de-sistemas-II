package gt.muni.quejas.repository;

import gt.muni.quejas.model.MotivoRechazo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MotivoRechazoRepository extends JpaRepository<MotivoRechazo, Long> {

    List<MotivoRechazo> findByActivoTrue();

    boolean existsByMotivoIgnoreCase(String motivo);
}