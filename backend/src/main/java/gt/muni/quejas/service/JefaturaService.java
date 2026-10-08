package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoActividad;
import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleSupervisor;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.ActividadCaso;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.ActividadCasoRepository;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.EstadoCasoRepository;
import gt.muni.quejas.repository.EvidenciaRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class JefaturaService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_OFICIO_FIRMADO = "El oficio fue firmado exitosamente. El caso ha sido resuelto.";
    public static final String MENSAJE_CASO_PROCESADO = "El caso fue procesado exitosamente.";

    private static final String ROL_JEFE = "Jefe";
    private static final int MAXIMO_OBSERVACIONES = 1000;

    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final ActividadCasoRepository actividadCasoRepository;
    private final OficioResolucionService oficioResolucionService;
    private final BitacoraCambioService bitacoraCambioService;
    private final NotificacionCasoService notificacionCasoService;

    public JefaturaService(CasoRepository casoRepository,
                           EstadoCasoRepository estadoCasoRepository,
                           UsuarioRepository usuarioRepository,
                           EvidenciaRepository evidenciaRepository,
                           ActividadCasoRepository actividadCasoRepository,
                           OficioResolucionService oficioResolucionService,
                           BitacoraCambioService bitacoraCambioService,
                           NotificacionCasoService notificacionCasoService) {
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.usuarioRepository = usuarioRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.actividadCasoRepository = actividadCasoRepository;
        this.oficioResolucionService = oficioResolucionService;
        this.bitacoraCambioService = bitacoraCambioService;
        this.notificacionCasoService = notificacionCasoService;
    }

    // ---------- CU-07 paso 4: casos Pendiente de Firma del departamento ----------
    @Transactional(readOnly = true)
    public List<DtoCasoBandeja> listarBandeja(Usuario jefatura) {
        validarRolJefe(jefatura);

        List<EstadoCaso> estadosVisibles = new ArrayList<>();
        estadosVisibles.add(buscarEstado(EstadoCaso.PENDIENTE_FIRMA));

        List<Caso> casos = casoRepository.findByDepartamentoIdAndEstadoInOrderByCreateAtAsc(
                jefatura.getIdDepartamento().getId(), estadosVisibles);

        List<DtoCasoBandeja> resultado = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            resultado.add(new DtoCasoBandeja(
                    caso.getId(),
                    caso.getCodigoSeguimiento(),
                    caso.getCreateAt(),
                    caso.getTipoCaso(),
                    caso.getCategoria().getNombre(),
                    caso.getEstado().getEstado()
            ));
        }
        return resultado;
    }

    // ---------- CU-07 paso 7: detalle con el reporte de atención y de supervisión ----------
    @Transactional(readOnly = true)
    public DtoCasoDetalleSupervisor obtenerDetalle(Long idCaso, Usuario jefatura) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, jefatura);

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

        return new DtoCasoDetalleSupervisor(
                caso.getId(),
                caso.getCodigoSeguimiento(),
                caso.getCreateAt(),
                caso.getTipoCaso(),
                caso.getCategoria().getNombre(),
                caso.getDescripcion(),
                caso.getDireccion(),
                caso.getEstado().getEstado(),
                caso.getTecnico() == null ? "" : caso.getTecnico().getNombre(),
                caso.getMotivoRechazo() == null ? "" : caso.getMotivoRechazo().getMotivo(),
                caso.getComentarioRechazo() == null ? "" : caso.getComentarioRechazo(),
                evidenciaRepository.existsByCasoIdAndTipo(caso.getId(), Evidencia.TIPO_REGISTRO),
                evidenciaRepository.existsByCasoIdAndTipo(caso.getId(), Evidencia.TIPO_ATENCION),
                caso.getInformeAtencionRuta() != null,
                caso.getInformeSupervisionRuta() != null,
                listaActividades
        );
    }

    // ---------- CU-07 pasos 9 al 13: la Jefatura firma el oficio ----------
    @Transactional
    public DtoMensaje firmarOficio(Long idCaso, Usuario jefatura, String ip) {
        Caso caso = buscarCasoEnEstado(idCaso, jefatura, EstadoCaso.PENDIENTE_FIRMA);

        List<ActividadCaso> actividades = actividadCasoRepository.findByCasoIdOrderByFechaAsc(caso.getId());
        caso.setOficioRuta(oficioResolucionService.generar(caso, jefatura, actividades));
        caso.setEstado(buscarEstado(EstadoCaso.RESUELTO));
        casoRepository.save(caso);

        notificacionCasoService.notificarResolucion(caso, jefatura, ip);

        bitacoraCambioService.registrarCambio(
                "Firma de oficio de resolución",
                jefatura.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "La Jefatura firmó el oficio y el caso quedó Resuelto",
                EstadoCaso.PENDIENTE_FIRMA,
                EstadoCaso.RESUELTO
        );

        return new DtoMensaje(MENSAJE_OFICIO_FIRMADO);
    }

    // ---------- CU-07 FA02: la Jefatura devuelve el caso al Supervisor ----------
    @Transactional
    public DtoMensaje devolverASupervision(Long idCaso, String observaciones, Usuario jefatura, String ip) {
        Caso caso = buscarCasoEnEstado(idCaso, jefatura, EstadoCaso.PENDIENTE_FIRMA);
        String observacionesLimpias = validarObservaciones(observaciones);

        caso.setObservacionesJefatura(observacionesLimpias);
        caso.setEstado(buscarEstado(EstadoCaso.EN_SUPERVISION));
        casoRepository.save(caso);

        notificarSupervisores(caso, observacionesLimpias, jefatura, ip);

        bitacoraCambioService.registrarCambio(
                "Devolución a Supervisión",
                jefatura.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "Observaciones: " + observacionesLimpias,
                EstadoCaso.PENDIENTE_FIRMA,
                EstadoCaso.EN_SUPERVISION
        );

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- Archivos del expediente que revisa la Jefatura ----------
    @Transactional(readOnly = true)
    public Evidencia obtenerEvidencia(Long idCaso, Usuario jefatura, String tipo) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, jefatura);
        return evidenciaRepository.findFirstByCasoIdAndTipo(caso.getId(), tipo)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));
    }

    @Transactional(readOnly = true)
    public String obtenerRutaReporteAtencion(Long idCaso, Usuario jefatura) {
        return rutaObligatoria(buscarCasoDeLaBandeja(idCaso, jefatura).getInformeAtencionRuta());
    }

    @Transactional(readOnly = true)
    public String obtenerRutaReporteSupervision(Long idCaso, Usuario jefatura) {
        return rutaObligatoria(buscarCasoDeLaBandeja(idCaso, jefatura).getInformeSupervisionRuta());
    }

    @Transactional(readOnly = true)
    public String obtenerRutaOficio(Long idCaso, Usuario jefatura) {
        return rutaObligatoria(buscarCasoDeLaBandeja(idCaso, jefatura).getOficioRuta());
    }

    // ---------- Métodos de apoyo ----------
    // El Supervisor del departamento recibe el aviso de la devolución
    private void notificarSupervisores(Caso caso, String observaciones, Usuario jefatura, String ip) {
        List<Usuario> supervisores = usuarioRepository.buscarSupervisoresActivosPorDepartamento(
                caso.getDepartamento().getId());

        notificacionCasoService.notificarDevolucionASupervision(caso, observaciones, supervisores, jefatura, ip);
    }

    private String rutaObligatoria(String ruta) {
        if (ruta == null) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        return ruta;
    }

    private String validarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        String limpias = observaciones.trim();
        if (limpias.length() > MAXIMO_OBSERVACIONES) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        return limpias;
    }

    private Caso buscarCasoEnEstado(Long idCaso, Usuario jefatura, String estadoEsperado) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, jefatura);

        if (!estadoEsperado.equals(caso.getEstado().getEstado())) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        return caso;
    }

    private Caso buscarCasoDeLaBandeja(Long idCaso, Usuario jefatura) {
        validarRolJefe(jefatura);

        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        // La Jefatura solo ve los casos de su propio departamento
        if (!caso.getDepartamento().getId().equals(jefatura.getIdDepartamento().getId())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        return caso;
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolJefe(Usuario jefatura) {
        if (jefatura == null || jefatura.getIdRol() == null
                || !ROL_JEFE.equals(jefatura.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }

    private EstadoCaso buscarEstado(String nombreEstado) {
        return estadoCasoRepository.findByEstado(nombreEstado)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado " + nombreEstado));
    }
}
