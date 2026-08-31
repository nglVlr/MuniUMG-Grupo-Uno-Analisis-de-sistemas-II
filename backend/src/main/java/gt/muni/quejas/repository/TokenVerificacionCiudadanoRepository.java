package gt.muni.quejas.repository;

import gt.muni.quejas.model.Ciudadano;
import gt.muni.quejas.model.TokenVerificacionCiudadano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenVerificacionCiudadanoRepository extends JpaRepository<TokenVerificacionCiudadano, Long> {
    List<TokenVerificacionCiudadano> findByCiudadanoAndUsadoFalse(Ciudadano ciudadano);

    Optional<TokenVerificacionCiudadano> findByTokenSesion(String tokenSesion);
}
