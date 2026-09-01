package gt.muni.quejas.repository;

import gt.muni.quejas.model.Ciudadano;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadanoRepository extends JpaRepository<Ciudadano, String> {
}
