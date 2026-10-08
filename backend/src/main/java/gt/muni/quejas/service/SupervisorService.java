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
public class SupervisorService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_CASO_PROCESADO = "El caso fue procesado exitosamente.";

    private static final String ROL_SUPERVISOR = "Supervisor";
    private static final String MOTIVO_INFORMACION_INCOMPLETA = "Información incompleta";
    private static final int MAXIMO_OBSERVACIONES = 1000;

    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final ActividadCasoRepository actividadCasoRepository;
    private final InformeSupervisionService informeSupervisionService;
    private final BitacoraCambioService bitacoraCambioService;
    private final NotificacionCasoService notificacionCasoService;

    public SupervisorService(CasoRepository casoRepository,
                             EstadoCasoRepository estadoCasoRepository,
                             UsuarioRepository usuarioRepository,
                             EvidenciaRepository evidenciaRepository,
                             ActividadCasoRepository actividadCasoRepository,
                             InformeSupervisionService informeSupervisionService,
                             BitacoraCambioService bitacoraCambioService,
                             NotificacionCasoService notificacionCasoService) {
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.usuarioRepository = usuarioRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.actividadCasoRepository = actividadCasoRepository;
        this.informeSupervisionService = informeSupervisionService;
        this.bitacoraCambioService = bitacoraCambioService;
        this.notificacionCasoService = notificacionCasoService;
    }

    // ---------- CU-06 paso 4: bandeja de supervisión del departamento ----------
    @Transactional(readOnly = true)
    public List<DtoCasoBandeja> listarBandeja(Usuario supervisor) {
        validarRolSupervisor(supervisor);

        List<EstadoCaso> estadosVisibles = new ArrayList<>();
        estadosVisibles.add(buscarEstado(EstadoCaso.EN_SUPERVISION));
        estadosVisibles.add(buscarEstado(EstadoCaso.PENDIENTE_APROBACION_RECHAZO));

        List<Caso> casos = casoRepository.findByDepartamentoIdAndEstadoInOrderByCreateAtAsc(
                supervisor.getIdDepartamento().getId(), estadosVisibles);

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

    // ---------- CU-06 paso 7: detalle del caso y reporte de atención ----------
    @Transactional(readOnly = true)
    public DtoCasoDetalleSupervisor obtenerDetalle(Long idCaso, Usuario supervisor) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, supervisor);

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

    // ---------- CU-06 pasos 9 al 13: el Supervisor aprueba el reporte ----------
    @Transactional
    public DtoMensaje aprobarReporte(Long idCaso, Usuario supervisor, String ip) {
        Caso caso = buscarCasoEnEstado(idCaso, supervisor, EstadoCaso.EN_SUPERVISION);

        String estadoAnterior = caso.getEstado().getEstado();
        caso.setInformeSupervisionRuta(informeSupervisionService.generar(caso, supervisor));
        caso.setEstado(buscarEstado(EstadoCaso.PENDIENTE_FIRMA));
        casoRepository.save(caso);

        bitacoraCambioService.registrarCambio(
                "Aprobación de reporte de atención",
                supervisor.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "El Supervisor aprobó el reporte y el caso pasó a Pendiente de Firma",
                estadoAnterior,
                EstadoCaso.PENDIENTE_FIRMA
        );

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- CU-06 FA02: el Supervisor devuelve el caso al Técnico ----------
    @Transactional
    public DtoMensaje devolverAlTecnico(Long idCaso, String observaciones, Usuario supervisor, String ip) {
        Caso caso = buscarCasoEnEstado(idCaso, supervisor, EstadoCaso.EN_SUPERVISION);
        String observacionesLimpias = validarObservaciones(observaciones);

        caso.setObservacionesSupervision(observacionesLimpias);
        caso.setEstado(buscarEstado(EstadoCaso.EN_ATENCION));
        casoRepository.save(caso);

        // El caso vuelve a estar En Atención, así que le sube la carga al Técnico
        if (caso.getTecnico() != null) {
            sumarCasoAlTecnico(caso.getTecnico().getCorreo());
            notificacionCasoService.notificarDevolucionAlTecnico(caso, observacionesLimpias, supervisor, ip);
        }

        bitacoraCambioService.registrarCambio(
                "Devolución al Técnico",
                supervisor.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "Observaciones: " + observacionesLimpias,
                EstadoCaso.EN_SUPERVISION,
                EstadoCaso.EN_ATENCION
        );

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- CU-06 FA03: el Supervisor aprueba un rechazo del Auxiliar ----------
    @Transactional
    public DtoMensaje aprobarRechazo(Long idCaso, Usuario supervisor, String ip) {
        Caso caso = buscarCasoEnEstado(idCaso, supervisor, EstadoCaso.PENDIENTE_APROBACION_RECHAZO);

        // RN04: si el motivo fue falta de información el caso se puede ampliar,
        // en cualquier otro caso queda Rechazado.
        boolean faltaInformacion = caso.getMotivoRechazo() != null
                && MOTIVO_INFORMACION_INCOMPLETA.equals(caso.getMotivoRechazo().getMotivo());

        String estadoFinal = faltaInformacion ? EstadoCaso.REQUIERE_AMPLIACION : EstadoCaso.RECHAZADO;
        caso.setEstado(buscarEstado(estadoFinal));
        casoRepository.save(caso);

        notificacionCasoService.notificarRechazo(caso, estadoFinal, supervisor, ip);

        bitacoraCambioService.registrarCambio(
                "Aprobación de rechazo",
                supervisor.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "El Supervisor aprobó el rechazo y el caso pasó a " + estadoFinal,
                EstadoCaso.PENDIENTE_APROBACION_RECHAZO,
                estadoFinal
        );

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- CU-06 FA04: el Supervisor no aprueba el rechazo ----------
    @Transactional
    public DtoMensaje regresarARevision(Long idCaso, String observaciones, Usuario supervisor, String ip) {
        Caso caso = buscarCasoEnEstado(idCaso, supervisor, EstadoCaso.PENDIENTE_APROBACION_RECHAZO);
        String observacionesLimpias = validarObservaciones(observaciones);

        caso.setObservacionesSupervision(observacionesLimpias);
        caso.setEstado(buscarEstado(EstadoCaso.EN_REVISION));
        casoRepository.save(caso);

        bitacoraCambioService.registrarCambio(
                "Rechazo no aprobado",
                supervisor.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "Observaciones: " + observacionesLimpias,
                EstadoCaso.PENDIENTE_APROBACION_RECHAZO,
                EstadoCaso.EN_REVISION
        );

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- Archivos del expediente que el Supervisor revisa ----------
    @Transactional(readOnly = true)
    public Evidencia obtenerEvidencia(Long idCaso, Usuario supervisor, String tipo) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, supervisor);
        return evidenciaRepository.findFirstByCasoIdAndTipo(caso.getId(), tipo)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));
    }

    @Transactional(readOnly = true)
    public String obtenerRutaReporteAtencion(Long idCaso, Usuario supervisor) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, supervisor);
        if (caso.getInformeAtencionRuta() == null) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        return caso.getInformeAtencionRuta();
    }

    // ---------- Métodos de apoyo ----------
    // El principal del token viene desconectado, por eso recargamos el Técnico
    private void sumarCasoAlTecnico(String correoTecnico) {
        Usuario tecnicoActual = usuarioRepository.findByCorreo(correoTecnico)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el Técnico del caso"));

        tecnicoActual.setCantidadCasosAsignados(tecnicoActual.getCantidadCasosAsignados() + 1);
        usuarioRepository.save(tecnicoActual);
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

    private Caso buscarCasoEnEstado(Long idCaso, Usuario supervisor, String estadoEsperado) {
        Caso caso = buscarCasoDeLaBandeja(idCaso, supervisor);

        if (!estadoEsperado.equals(caso.getEstado().getEstado())) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        return caso;
    }

    private Caso buscarCasoDeLaBandeja(Long idCaso, Usuario supervisor) {
        validarRolSupervisor(supervisor);

        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        // El Supervisor solo ve los casos de su propio departamento
        if (!caso.getDepartamento().getId().equals(supervisor.getIdDepartamento().getId())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        return caso;
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolSupervisor(Usuario supervisor) {
        if (supervisor == null || supervisor.getIdRol() == null
                || !ROL_SUPERVISOR.equals(supervisor.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }

    private EstadoCaso buscarEstado(String nombreEstado) {
        return estadoCasoRepository.findByEstado(nombreEstado)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado " + nombreEstado));
    }
}
