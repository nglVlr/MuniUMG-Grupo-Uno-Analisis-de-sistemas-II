package gt.muni.quejas.repository;

import gt.muni.quejas.model.EstadoUsuario;
import gt.muni.quejas.model.Rol;
import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    // Usuarios de un departamento con un rol y estado especifico (ej. empleados ACTIVOS
    // de un departamento), usado por el algoritmo de asignacion y por la reasignacion.
    List<Usuario> findByDepartamentoIdAndRolAndEstado(Long idDepartamento, Rol rol, EstadoUsuario estado);
}
