package gt.muni.quejas.repository;

import gt.muni.quejas.model.CorrelativoSeguimiento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CorrelativoSeguimientoRepository extends JpaRepository<CorrelativoSeguimiento, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CorrelativoSeguimiento c where c.anio = :anio")
    Optional<CorrelativoSeguimiento> lockByAnio(@Param("anio") Integer anio);
}
