package gt.muni.quejas.repository;

import gt.muni.quejas.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {

    Optional<Departamento> findByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
