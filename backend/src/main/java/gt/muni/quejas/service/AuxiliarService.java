package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleAuxiliar;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoMotivoRechazo;
import gt.muni.quejas.dto.DtoRechazoAuxiliar;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.model.MotivoRechazo;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.EstadoCasoRepository;
import gt.muni.quejas.repository.EvidenciaRepository;
import gt.muni.quejas.repository.MotivoRechazoRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuxiliarService {

    // Mensajes tal de las Reglas de Negocio (AN01 y AN02)
    public static final String MENSAJE_CASO_ASIGNADO = "El caso fue asignado exitosamente.";
    public static final String MENSAJE_CASO_PROCESADO = "El caso fue procesado exitosamente.";

    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MotivoRechazoRepository motivoRechazoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final BitacoraCambioService bitacoraCambioService;
    private final NotificacionCasoService notificacionCasoService;

    public AuxiliarService(CasoRepository casoRepository,
                           EstadoCasoRepository estadoCasoRepository,
                           UsuarioRepository usuarioRepository,
                           MotivoRechazoRepository motivoRechazoRepository,
                           EvidenciaRepository evidenciaRepository,
                           BitacoraCambioService bitacoraCambioService,
                           NotificacionCasoService notificacionCasoService) {
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.usuarioRepository = usuarioRepository;
        this.motivoRechazoRepository = motivoRechazoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.bitacoraCambioService = bitacoraCambioService;
        this.notificacionCasoService = notificacionCasoService;
    }

    // ---------- Paso 2.3.2: RN16 Bandeja Auxiliar ----------
    @Transactional(readOnly = true)
    public List<DtoCasoBandeja> listarBandeja() {
        EstadoCaso estadoRegistrado = buscarEstado(EstadoCaso.REGISTRADO);
        EstadoCaso estadoEnRevision = buscarEstado(EstadoCaso.EN_REVISION);

        List<EstadoCaso> estadosVisibles = new ArrayList<>();
        estadosVisibles.add(estadoRegistrado);
        estadosVisibles.add(estadoEnRevision);

        // Un caso "Aceptado" pasa a En Revisión y debe seguir viéndose en la bandeja
        // hasta que se le asigne Técnico, por eso consultamos ambos estados.
        List<Caso> casos = casoRepository.findByEstadoInOrderByCreateAtAsc(estadosVisibles);

        // Convertimos cada Caso en un DtoCasoBandeja
        List<DtoCasoBandeja> resultado = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            DtoCasoBandeja fila = new DtoCasoBandeja(
                    caso.getId(),
                    caso.getCodigoSeguimiento(),
                    caso.getCreateAt(),
                    caso.getTipoCaso(),
                    caso.getCategoria().getNombre(),
                    caso.getEstado().getEstado()
            );
            resultado.add(fila);
        }
        return resultado;
    }

    // ---------- Paso 2.3.4: RN17 Detalle Auxiliar ----------
    @Transactional(readOnly = true)
    public DtoCasoDetalleAuxiliar obtenerDetalle(Long idCaso) {
        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        boolean tieneEvidencia = evidenciaRepository.existsByCasoId(caso.getId());

        return new DtoCasoDetalleAuxiliar(
                caso.getId(),
                caso.getCodigoSeguimiento(),
                caso.getCreateAt(),
                caso.getTipoCaso(),
                caso.getCategoria().getNombre(),
                caso.getDescripcion(),
                caso.getDireccion(),
                caso.getEstado().getEstado(),
                tieneEvidencia
        );
    }

    // ---------- Paso 2.3.6 y 2.3.7: el Auxiliar acepta el caso ----------
    @Transactional
    public DtoCasoDetalleAuxiliar aceptarCaso(Long idCaso, Usuario auxiliar, String ip) {
        Caso caso = buscarCasoRegistrado(idCaso);

        String estadoAnterior = caso.getEstado().getEstado();
        EstadoCaso enRevision = buscarEstado(EstadoCaso.EN_REVISION);
        caso.setEstado(enRevision);
        casoRepository.save(caso);

        bitacoraCambioService.registrarCambio(
                "Aceptación de caso",
                auxiliar.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "El Auxiliar aceptó el caso",
                estadoAnterior,
                enRevision.getEstado()
        );

        return obtenerDetalle(caso.getId());
    }

    // ---------- Paso 2.3.8 al 2.3.10: asignar Técnico ----------
    @Transactional
    public DtoMensaje asignarTecnico(Long idCaso, Usuario auxiliar, String ip) {
        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        if (!EstadoCaso.EN_REVISION.equals(caso.getEstado().getEstado())) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }

        Long idDepartamento = caso.getDepartamento().getId();
        List<Usuario> tecnicosActivos = usuarioRepository.buscarTecnicosActivosPorDepartamento(idDepartamento);

        // FA09: no hay técnico activo en el departamento
        if (tecnicosActivos.isEmpty()) {
            throw new NegocioException(CodigoError.TECNICO_NO_DISPONIBLE);
        }

        EstadoCaso enAtencion = buscarEstado(EstadoCaso.EN_ATENCION);

        // Buscamos, con un for normal, al técnico con menos casos "En Atención".
        // La carga se cuenta sobre los casos mismos y no con el contador del usuario:
        // así la repartición siempre usa el dato real, aunque el contador quede
        // desfasado por alguna carga de datos hecha fuera del sistema.
        Usuario tecnicoElegido = null;
        long menorCantidadCasos = -1;

        for (int i = 0; i < tecnicosActivos.size(); i++) {
            Usuario tecnicoActual = tecnicosActivos.get(i);
            long cantidadCasos = casoRepository.countByTecnicoAndEstado(tecnicoActual, enAtencion);

            if (tecnicoElegido == null || cantidadCasos < menorCantidadCasos) {
                tecnicoElegido = tecnicoActual;
                menorCantidadCasos = cantidadCasos;
            }
        }

        // El contador sigue el mismo movimiento que la carga real: sube aquí y baja
        // cuando el Técnico termina la atención (CU-05)
        tecnicoElegido.setCantidadCasosAsignados((int) menorCantidadCasos + 1);
        usuarioRepository.save(tecnicoElegido);

        String estadoAnterior = caso.getEstado().getEstado();
        caso.setTecnico(tecnicoElegido);
        caso.setEstado(enAtencion);
        casoRepository.save(caso);

        notificacionCasoService.notificarAsignacionTecnico(caso, auxiliar, ip);

        bitacoraCambioService.registrarCambio(
                "Asignación de Técnico",
                auxiliar.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "Caso asignado al Técnico " + tecnicoElegido.getNombre(),
                estadoAnterior,
                enAtencion.getEstado()
        );

        return new DtoMensaje(MENSAJE_CASO_ASIGNADO);
    }

    // ---------- FA02 y FA03: el Auxiliar rechaza el caso ----------
    @Transactional
    public DtoMensaje rechazarCaso(Long idCaso, DtoRechazoAuxiliar dto, Usuario auxiliar, String ip) {
        Caso caso = buscarCasoRegistrado(idCaso);

        if (dto.idMotivoRechazo() == null || dto.comentario() == null || dto.comentario().isBlank()) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        if (dto.comentario().trim().length() > 500) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }

        MotivoRechazo motivo = motivoRechazoRepository.findById(dto.idMotivoRechazo())
                .orElseThrow(() -> new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS));

        caso.setMotivoRechazo(motivo);
        caso.setComentarioRechazo(dto.comentario().trim());

        boolean necesitaAmpliacion = Boolean.TRUE.equals(dto.aplicaAmpliacion());

        if (necesitaAmpliacion) {
            // FA03: Rechazo con ampliación
            EstadoCaso requiereAmpliacion = buscarEstado(EstadoCaso.REQUIERE_AMPLIACION);
            caso.setEstado(requiereAmpliacion);
            casoRepository.save(caso);

            notificacionCasoService.notificarAmpliacion(caso, auxiliar, ip);

            bitacoraCambioService.registrarCambio(
                    "Rechazo con ampliación",
                    auxiliar.getCui(),
                    BitacoraCambioService.ENTIDAD_CASO,
                    caso,
                    ip,
                    "Motivo: " + motivo.getMotivo(),
                    EstadoCaso.REGISTRADO,
                    EstadoCaso.REQUIERE_AMPLIACION
            );
        } else {
            // FA02: rechazo normal, queda pendiente de que el Supervisor lo apruebe
            EstadoCaso pendienteAprobacion = buscarEstado(EstadoCaso.PENDIENTE_APROBACION_RECHAZO);
            caso.setEstado(pendienteAprobacion);
            casoRepository.save(caso);

            bitacoraCambioService.registrarCambio(
                    "Rechazo de caso",
                    auxiliar.getCui(),
                    BitacoraCambioService.ENTIDAD_CASO,
                    caso,
                    ip,
                    "Motivo: " + motivo.getMotivo(),
                    EstadoCaso.REGISTRADO,
                    EstadoCaso.PENDIENTE_APROBACION_RECHAZO
            );
        }

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- Catálogo de motivos para el formulario de rechazo (RN05) ----------
    @Transactional(readOnly = true)
    public List<DtoMotivoRechazo> listarMotivosRechazo() {
        List<MotivoRechazo> motivos = motivoRechazoRepository.findByActivoTrue();

        List<DtoMotivoRechazo> resultado = new ArrayList<>();
        for (int i = 0; i < motivos.size(); i++) {
            MotivoRechazo motivo = motivos.get(i);
            resultado.add(new DtoMotivoRechazo(motivo.getId(), motivo.getMotivo()));
        }
        return resultado;
    }

    // ---------- FA06: Ver documento adjunto ----------
    @Transactional(readOnly = true)
    public Evidencia obtenerEvidencia(Long idCaso) {
        return evidenciaRepository.findFirstByCasoId(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));
    }

    // ---------- Métodos de apoyo ----------
    private Caso buscarCasoRegistrado(Long idCaso) {
        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        if (!EstadoCaso.REGISTRADO.equals(caso.getEstado().getEstado())) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        return caso;
    }

    private EstadoCaso buscarEstado(String nombreEstado) {
        return estadoCasoRepository.findByEstado(nombreEstado)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado " + nombreEstado));
    }
}
