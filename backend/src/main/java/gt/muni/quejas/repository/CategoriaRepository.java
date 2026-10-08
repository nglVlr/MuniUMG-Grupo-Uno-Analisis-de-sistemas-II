package gt.muni.quejas.repository;

import gt.muni.quejas.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    long countByDepartamentoId(Long idDepartamento);

    boolean existsByNombreIgnoreCase(String nombre);

    @Query("""
            select distinct c from Categoria c
            left join fetch c.departamento
            left join fetch c.tiposCaso
            where c.activo = true
            order by c.nombre
            """)
    List<Categoria> findActivasConDepartamento();

    @Query("""
            select c from Categoria c
            join fetch c.departamento
            left join fetch c.tiposCaso
            where c.id = :id
            """)
    Optional<Categoria> findByIdConCatalogo(@Param("id") Long id);
}
