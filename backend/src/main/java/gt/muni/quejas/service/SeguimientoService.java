package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoActividad;
import gt.muni.quejas.dto.DtoExpediente;
import gt.muni.quejas.dto.DtoHistorialCaso;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.ActividadCaso;
import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.ActividadCasoRepository;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.EvidenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Seguimiento del caso: consulta de solo lectura del expediente completo.
// No es un caso de uso aparte, es una opción dentro de las bandejas (CU-04 a CU-07).
@Service
public class SeguimientoService {

    // Tipos de documento que el expediente puede entregar
    public static final String DOC_EVIDENCIA = "evidencia";
    public static final String DOC_SOPORTE_ATENCION = "soporte-atencion";
    public static final String DOC_INFORME_INICIAL = "informe-inicial";
    public static final String DOC_REPORTE_ATENCION = "reporte-atencion";
    public static final String DOC_REPORTE_SUPERVISION = "reporte-supervision";
    public static final String DOC_OFICIO = "oficio";

    private static final String ROL_ADMIN = "Admin";
    private static final String ROL_AUXILIAR = "Auxiliar";
    private static final String ROL_TECNICO = "Tecnico";
    private static final String ROL_SUPERVISOR = "Supervisor";
    private static final String ROL_JEFE = "Jefe";

    private final CasoRepository casoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final ActividadCasoRepository actividadCasoRepository;
    private final BitacoraCambioRepository bitacoraCambioRepository;

    public SeguimientoService(CasoRepository casoRepository,
                              EvidenciaRepository evidenciaRepository,
                              ActividadCasoRepository actividadCasoRepository,
                              BitacoraCambioRepository bitacoraCambioRepository) {
        this.casoRepository = casoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.actividadCasoRepository = actividadCasoRepository;
        this.bitacoraCambioRepository = bitacoraCambioRepository;
    }

    // ---------- Busca el caso por su código de seguimiento ----------
    @Transactional(readOnly = true)
    public DtoExpediente buscarPorCodigo(String codigoSeguimiento, Usuario usuario) {
        if (codigoSeguimiento == null || codigoSeguimiento.isBlank()) {
            throw new NegocioException(CodigoError.CODIGO_SEGUIMIENTO_REQUERIDO);
        }

        Caso caso = casoRepository.findByCodigoSeguimiento(codigoSeguimiento.trim().toUpperCase())
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        return armarExpediente(validarAcceso(caso, usuario));
    }

    @Transactional(readOnly = true)
    public DtoExpediente obtenerExpediente(Long idCaso, Usuario usuario) {
        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        return armarExpediente(validarAcceso(caso, usuario));
    }

    // ---------- Un documento del expediente, para abrirlo o descargarlo ----------
    @Transactional(readOnly = true)
    public Evidencia obtenerEvidencia(Long idCaso, Usuario usuario, String tipo) {
        Caso caso = validarAcceso(buscarCaso(idCaso), usuario);

        String tipoEvidencia = DOC_SOPORTE_ATENCION.equals(tipo)
                ? Evidencia.TIPO_ATENCION
                : Evidencia.TIPO_REGISTRO;

        return evidenciaRepository.findFirstByCasoIdAndTipo(caso.getId(), tipoEvidencia)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));
    }

    @Transactional(readOnly = true)
    public String obtenerRutaInforme(Long idCaso, Usuario usuario, String tipo) {
        Caso caso = validarAcceso(buscarCaso(idCaso), usuario);

        String ruta;
        if (DOC_INFORME_INICIAL.equals(tipo)) {
            ruta = caso.getInformeInicialRuta();
        } else if (DOC_REPORTE_ATENCION.equals(tipo)) {
            ruta = caso.getInformeAtencionRuta();
        } else if (DOC_REPORTE_SUPERVISION.equals(tipo)) {
            ruta = caso.getInformeSupervisionRuta();
        } else if (DOC_OFICIO.equals(tipo)) {
            ruta = caso.getOficioRuta();
        } else {
            throw new NegocioException(CodigoError.VALIDACION, "Tipo de documento no válido");
        }

        if (ruta == null) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        return ruta;
    }

    // ---------- Métodos de apoyo ----------
    private DtoExpediente armarExpediente(Caso caso) {
        List<ActividadCaso> actividades = actividadCasoRepository.findByCasoIdOrderByFechaAsc(caso.getId());
        List<DtoActividad> listaActividades = new ArrayList<>();
        for (int i = 0; i < actividades.size(); i++) {
            ActividadCaso actividad = actividades.get(i);
            listaActividades.add(new DtoActividad(
                    caso.getCodigoSeguimiento(),
                    actividad.getId(),
                    actividad.getDetalle(),
                    actividad.getFecha()
            ));
        }

        List<BitacoraCambio> registros = bitacoraCambioRepository.findByCasoIdOrderByIdAsc(caso.getId());
        List<DtoHistorialCaso> historial = new ArrayList<>();
        for (int i = 0; i < registros.size(); i++) {
            BitacoraCambio registro = registros.get(i);
            historial.add(new DtoHistorialCaso(
                    registro.getAccion(),
                    registro.getUsuario(),
                    registro.getFecha(),
                    registro.getHora(),
                    registro.getIpOrigen(),
                    registro.getDetalle() == null ? "" : registro.getDetalle()
            ));
        }

        return new DtoExpediente(
                caso.getId(),
                caso.getCodigoSeguimiento(),
                caso.getCreateAt(),
                caso.getTipoCaso(),
                caso.getCategoria().getNombre(),
                caso.getDepartamento().getNombre(),
                caso.getDescripcion(),
                caso.getDireccion(),
                caso.getEstado().getEstado(),
                caso.getCiudadano().getNombre(),
                caso.getTecnico() == null ? "" : caso.getTecnico().getNombre(),
                caso.getMotivoRechazo() == null ? "" : caso.getMotivoRechazo().getMotivo(),
                caso.getComentarioRechazo() == null ? "" : caso.getComentarioRechazo(),
                caso.getObservacionesSupervision() == null ? "" : caso.getObservacionesSupervision(),
                caso.getObservacionesJefatura() == null ? "" : caso.getObservacionesJefatura(),
                evidenciaRepository.existsByCasoIdAndTipo(caso.getId(), Evidencia.TIPO_REGISTRO),
                evidenciaRepository.existsByCasoIdAndTipo(caso.getId(), Evidencia.TIPO_ATENCION),
                caso.getInformeInicialRuta() != null,
                caso.getInformeAtencionRuta() != null,
                caso.getInformeSupervisionRuta() != null,
                caso.getOficioRuta() != null,
                listaActividades,
                historial
        );
    }

    private Caso buscarCaso(Long idCaso) {
        return casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));
    }

    // El alcance de la consulta depende del rol: el Técnico ve solo sus casos,
    // el Supervisor y la Jefatura los de su departamento, el Auxiliar y el Admin todos.
    private Caso validarAcceso(Caso caso, Usuario usuario) {
        if (usuario == null || usuario.getIdRol() == null) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }

        String rol = usuario.getIdRol().getRol();

        if (ROL_AUXILIAR.equals(rol) || ROL_ADMIN.equals(rol)) {
            return caso;
        }
        if (ROL_TECNICO.equals(rol)) {
            if (caso.getTecnico() == null || !caso.getTecnico().getCui().equals(usuario.getCui())) {
                throw new NegocioException(CodigoError.ACCESO_DENEGADO);
            }
            return caso;
        }
        if (ROL_SUPERVISOR.equals(rol) || ROL_JEFE.equals(rol)) {
            if (!caso.getDepartamento().getId().equals(usuario.getIdDepartamento().getId())) {
                throw new NegocioException(CodigoError.ACCESO_DENEGADO);
            }
            return caso;
        }
        throw new NegocioException(CodigoError.ACCESO_DENEGADO);
    }
}
