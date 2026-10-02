package gt.muni.quejas.repository;

import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CasoRepository extends JpaRepository<Caso, Long> {
    Optional<Caso> findByCodigoSeguimiento(String codigoSeguimiento);

    List<Caso> findByEstadoInOrderByCreateAtAsc(List<EstadoCaso> estados);

    long countByTecnicoAndEstado(Usuario tecnico, EstadoCaso estado);
}
