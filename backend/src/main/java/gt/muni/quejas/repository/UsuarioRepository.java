package gt.muni.quejas.repository;

import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String username);

    @Query("select u from Usuario u where u.idRol.rol = 'Tecnico' and u.idDepartamento.id = :idDepartamento and u.idEstado.estado = 'Activo'")
    List<Usuario> buscarTecnicosActivosPorDepartamento(@Param("idDepartamento") Long idDepartamento);
}
