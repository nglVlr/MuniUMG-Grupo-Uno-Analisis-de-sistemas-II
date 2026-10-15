package gt.muni.quejas.repository;

import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByCorreo(String username);

    Optional<Usuario> findByCui(String cui);

    boolean existsByCui(String cui);

    long countByIdDepartamentoId(Long idDepartamento);

    @Query("select u from Usuario u where u.idRol.rol = 'Tecnico' and u.idDepartamento.id = :idDepartamento and u.idEstado.estado = 'Activo'")
    List<Usuario> buscarTecnicosActivosPorDepartamento(@Param("idDepartamento") Long idDepartamento);

    @Query("select u from Usuario u where u.idRol.rol = 'Supervisor' and u.idDepartamento.id = :idDepartamento and u.idEstado.estado = 'Activo'")
    List<Usuario> buscarSupervisoresActivosPorDepartamento(@Param("idDepartamento") Long idDepartamento);

    // CU-09 FA12: usuarios que seguirían atendiendo la bandeja de un rol cuando otro
    // usuario de ese mismo rol deja de atenderla. El Auxiliar revisa los casos de todos
    // los departamentos (CU-04), por eso para ese rol idDepartamento llega nulo.
    @Query("""
            select count(u) from Usuario u
            where u.idRol.rol = :rol
              and u.idEstado.estado = 'Activo'
              and u.cui <> :cui
              and (:idDepartamento is null or u.idDepartamento.id = :idDepartamento)
            """)
    long contarActivosDelRol(@Param("rol") String rol,
                             @Param("idDepartamento") Long idDepartamento,
                             @Param("cui") String cui);
}
