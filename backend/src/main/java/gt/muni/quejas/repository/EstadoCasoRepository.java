package gt.muni.quejas.repository;

import gt.muni.quejas.model.EstadoCaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoCasoRepository extends JpaRepository<EstadoCaso, Long> {
    Optional<EstadoCaso> findByEstado(String estado);
}
