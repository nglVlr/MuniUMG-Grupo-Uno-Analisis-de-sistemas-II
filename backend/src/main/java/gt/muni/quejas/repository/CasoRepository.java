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

    // CU-10 FA08: casos que el Técnico tiene a su cargo, para saber si puede inactivarse
    long countByTecnicoAndEstadoIn(Usuario tecnico, List<EstadoCaso> estados);

    List<Caso> findByTecnicoAndEstadoInOrderByCreateAtAsc(Usuario tecnico, List<EstadoCaso> estados);

    // RN26: carga pendiente de cada rol. Se cuenta por el nombre del estado para no
    // tener que buscar primero cada EstadoCaso de la lista.
    @Query("select count(c) from Caso c where c.tecnico = :tecnico and c.estado.estado in :estados")
    long contarPorTecnicoYEstados(@Param("tecnico") Usuario tecnico,
                                  @Param("estados") List<String> estados);

    @Query("select count(c) from Caso c where c.estado.estado in :estados")
    long contarPorEstados(@Param("estados") List<String> estados);

    @Query("select count(c) from Caso c where c.departamento.id = :idDepartamento and c.estado.estado in :estados")
    long contarPorDepartamentoYEstados(@Param("idDepartamento") Long idDepartamento,
                                       @Param("estados") List<String> estados);

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

    // CU-19 Dashboard: los filtros de caso se aplican una sola vez, en esta consulta.
    // El rango de fechas no va aquí porque puede ser por fecha de registro o por fecha
    // de resolución, y la fecha de resolución se deduce de la bitácora.
    @Query("""
            select distinct c from Caso c
            join fetch c.ciudadano
            join fetch c.categoria
            join fetch c.departamento
            join fetch c.estado
            left join fetch c.tecnico
            left join fetch c.motivoRechazo
            where (:idDepartamento is null or c.departamento.id = :idDepartamento)
              and (:tipoCaso is null or c.tipoCaso = :tipoCaso)
              and (:idCategoria is null or c.categoria.id = :idCategoria)
              and (:estado is null or c.estado.estado = :estado)
              and (:cuiTecnico is null or c.tecnico.cui = :cuiTecnico)
              and (:idMotivoRechazo is null or c.motivoRechazo.id = :idMotivoRechazo)
              and (:dpiCiudadano is null or c.ciudadano.dpi = :dpiCiudadano)
            order by c.createAt asc
            """)
    List<Caso> buscarParaDashboard(@Param("idDepartamento") Long idDepartamento,
                                   @Param("tipoCaso") String tipoCaso,
                                   @Param("idCategoria") Long idCategoria,
                                   @Param("estado") String estado,
                                   @Param("cuiTecnico") String cuiTecnico,
                                   @Param("idMotivoRechazo") Long idMotivoRechazo,
                                   @Param("dpiCiudadano") String dpiCiudadano);
}
