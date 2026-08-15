package gt.muni.quejas.repository;

import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.EstadoCaso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CasoRepository extends JpaRepository<Caso, Long> {

    // CU-03: consulta de estado por el ciudadano con su codigo de seguimiento
    Optional<Caso> findByCodigoSeguimiento(String codigoSeguimiento);

    // usado por el algoritmo de asignacion para contar la carga de trabajo de un empleado
    long countByEmpleadoAsignadoIdAndEstado(Long idEmpleado, EstadoCaso estado);

    List<Caso> findByDepartamentoIdAndEstado(Long idDepartamento, EstadoCaso estado);

    List<Caso> findByEmpleadoAsignadoId(Long idEmpleado);
}
