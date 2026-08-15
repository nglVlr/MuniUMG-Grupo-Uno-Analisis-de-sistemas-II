package gt.muni.quejas.service;

import gt.muni.quejas.model.*;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Algoritmo de asignacion y reasignacion de casos (CU-05, RN-CU05-04/05, y el
 * requerimiento de reasignacion entre actores). Logica simple a proposito:
 * no se usan colas ni prediccion, solo comparar carga de trabajo actual.
 */
@Service
public class AsignacionService {

    private final UsuarioRepository usuarioRepository;
    private final CasoRepository casoRepository;

    public AsignacionService(UsuarioRepository usuarioRepository, CasoRepository casoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.casoRepository = casoRepository;
    }

    /**
     * RN-CU05-04/05: determina el Empleado Responsable con menor carga de trabajo
     * dentro de un departamento, considerando UNICAMENTE empleados en estado ACTIVO
     * (es decir, se excluyen automaticamente los que estan en VACACIONES, PERMISO,
     * INACTIVO o BLOQUEADO).
     */
    public Usuario obtenerEmpleadoConMenorCarga(Long idDepartamento) {
        List<Usuario> disponibles = usuarioRepository.findByDepartamentoIdAndRolAndEstado(
                idDepartamento, Rol.EMPLEADO_RESPONSABLE, EstadoUsuario.ACTIVO);

        if (disponibles.isEmpty()) {
            throw new IllegalStateException(
                "No existe un Empleado Responsable activo en el departamento para asignar el caso"); // AN02 CU-05 No.3
        }

        return disponibles.stream()
                .min(Comparator.comparingLong(this::cargaActual))
                .orElseThrow();
    }

    /**
     * Reasignacion manual de un caso a otro usuario interno (nuevo requerimiento:
     * el sistema permite reasignar casos entre actores). Valida que el nuevo
     * responsable este disponible (estado ACTIVO) antes de reasignar.
     */
    public void reasignarCaso(Caso caso, Usuario nuevoResponsable) {
        if (nuevoResponsable.getEstado() != EstadoUsuario.ACTIVO) {
            throw new IllegalStateException(
                "El usuario seleccionado no esta disponible (estado: " + nuevoResponsable.getEstado() + ")");
        }
        caso.setEmpleadoAsignado(nuevoResponsable);
        casoRepository.save(caso);
        // La bitacora de este cambio se registra en BitacoraService (ver RN-CU05-06 / RN-CU16).
    }

    private long cargaActual(Usuario empleado) {
        return casoRepository.countByEmpleadoAsignadoIdAndEstado(empleado.getId(), EstadoCaso.EN_ATENCION);
    }
}
