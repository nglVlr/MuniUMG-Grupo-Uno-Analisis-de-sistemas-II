package gt.muni.quejas.repository;

import gt.muni.quejas.model.TokenRecuperacion;
import gt.muni.quejas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByToken(String token);

    List<TokenRecuperacion> findByUsuarioAndUsadoFalse(Usuario usuario);
}
