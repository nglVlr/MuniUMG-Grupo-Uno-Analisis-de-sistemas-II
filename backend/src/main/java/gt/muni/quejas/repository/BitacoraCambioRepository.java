package gt.muni.quejas.repository;

import gt.muni.quejas.model.BitacoraCambio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BitacoraCambioRepository extends JpaRepository<BitacoraCambio, Long> {

    List<BitacoraCambio> findByCasoIdOrderByIdAsc(Long idCaso);

    // CU-15: consulta del Admin con filtros y paginación
    @Query("""
            select b from BitacoraCambio b
            where b.fecha >= :desde and b.fecha <= :hasta
              and (:usuario is null or lower(b.usuario) like :usuario)
              and (:accion is null or b.accion = :accion)
              and (:entidad is null or b.entidad = :entidad)
              and (:rol is null or b.rol = :rol)
            order by b.id desc
            """)
    Page<BitacoraCambio> consultar(@Param("desde") LocalDate desde,
                                   @Param("hasta") LocalDate hasta,
                                   @Param("usuario") String usuario,
                                   @Param("accion") String accion,
                                   @Param("entidad") String entidad,
                                   @Param("rol") String rol,
                                   Pageable pageable);

    @Query("select distinct b.accion from BitacoraCambio b order by b.accion")
    List<String> listarAcciones();
}
