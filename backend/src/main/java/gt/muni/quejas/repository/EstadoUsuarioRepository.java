package gt.muni.quejas.repository;

import gt.muni.quejas.model.EstadoUsuarios;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoUsuarioRepository extends JpaRepository<EstadoUsuarios, Long> {
    Optional<EstadoUsuarios> findByEstado(String estado);
}
