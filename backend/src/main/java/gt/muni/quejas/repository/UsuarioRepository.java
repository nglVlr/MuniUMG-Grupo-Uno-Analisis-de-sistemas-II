package gt.muni.quejas.repository;

import aj.org.objectweb.asm.commons.Remapper;
import gt.muni.quejas.model.EstadoUsuarios;
import gt.muni.quejas.model.Rol;
import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String username);


}
