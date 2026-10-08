package gt.muni.quejas.repository;

import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface CasoRepository extends JpaRepository<Caso, Long> {
    Optional<Caso> findByCodigoSeguimiento(String codigoSeguimiento);

    List<Caso> findByEstadoInOrderByCreateAtAsc(List<EstadoCaso> estados);

    long countByTecnicoAndEstado(Usuario tecnico, EstadoCaso estado);

    List<Caso> findByTecnicoAndEstadoOrderByCreateAtAsc(Usuario tecnico, EstadoCaso estado);

    List<Caso> findByDepartamentoIdAndEstadoInOrderByCreateAtAsc(Long idDepartamento, List<EstadoCaso> estados);

    long countByCategoriaId(Long idCategoria);

    long countByDepartamentoId(Long idDepartamento);

    long countByMotivoRechazoId(Long idMotivoRechazo);

    List<Caso> findByCiudadanoDpiAndEstadoOrderByCreateAtAsc(String dpi, EstadoCaso estado);

    @Query("""
            select c from Caso c
            where c.createAt >= :inicio and c.createAt < :fin
              and (:idDepartamento is null or c.departamento.id = :idDepartamento)
              and (:tipoCaso is null or c.tipoCaso = :tipoCaso)
              and (:estado is null or c.estado.estado = :estado)
            order by c.createAt asc
            """)
    List<Caso> buscarParaReporte(@Param("inicio") LocalDateTime inicio,
                                 @Param("fin") LocalDateTime fin,
                                 @Param("idDepartamento") Long idDepartamento,
                                 @Param("tipoCaso") String tipoCaso,
                                 @Param("estado") String estado);
}
