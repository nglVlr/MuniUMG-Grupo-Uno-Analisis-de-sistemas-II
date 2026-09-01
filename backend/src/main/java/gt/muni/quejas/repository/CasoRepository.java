package gt.muni.quejas.repository;

import gt.muni.quejas.model.Caso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CasoRepository extends JpaRepository<Caso, Long> {
    Optional<Caso> findByCodigoSeguimiento(String codigoSeguimiento);
}
