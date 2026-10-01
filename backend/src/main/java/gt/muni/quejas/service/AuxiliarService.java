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

    // Mensajes tal cual están en las Reglas de Negocio (AN01 y AN02)
    public static final String MENSAJE_CASO_ASIGNADO = "El caso fue asignado exitosamente.";
    public static final String MENSAJE_CASO_PROCESADO = "El caso fue procesado exitosamente.";

    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MotivoRechazoRepository motivoRechazoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final BitacoraCambioService bitacoraCambioService;
    private final EmailService emailService;

    public AuxiliarService(CasoRepository casoRepository,
                           EstadoCasoRepository estadoCasoRepository,
                           UsuarioRepository usuarioRepository,
                           MotivoRechazoRepository motivoRechazoRepository,
                           EvidenciaRepository evidenciaRepository,
                           BitacoraCambioService bitacoraCambioService,
                           EmailService emailService) {
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.usuarioRepository = usuarioRepository;
        this.motivoRechazoRepository = motivoRechazoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.bitacoraCambioService = bitacoraCambioService;
        this.emailService = emailService;
    }

    // ---------- Paso 2.3.2: RN16 Bandeja Auxiliar ----------
    @Transactional(readOnly = true)
    public List<DtoCasoBandeja> listarBandeja() {
        EstadoCaso estadoRegistrado = buscarEstado(EstadoCaso.REGISTRADO);
        EstadoCaso estadoEnRevision = buscarEstado(EstadoCaso.EN_REVISION);

        List<EstadoCaso> estadosVisibles = new ArrayList<>();
        estadosVisibles.add(estadoRegistrado);
        estadosVisibles.add(estadoEnRevision);
        
        List<Caso> casos = casoRepository.findByEstadoOrderByCreateAtAsc(estadoRegistrado);

        // Convertimos cada Caso en un DtoCasoBandeja con un for normal
        List<DtoCasoBandeja> resultado = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            DtoCasoBandeja fila = new DtoCasoBandeja(
                    caso.getId(),
                    caso.getCodigoSeguimiento(),
                    caso.getCreateAt(),
                    caso.getTipoCaso(),
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

        EstadoCaso enRevision = buscarEstado(EstadoCaso.EN_REVISION);
        caso.setEstado(enRevision);
        casoRepository.save(caso);

        bitacoraCambioService.registrar(
                "Aceptación de caso",
                auxiliar.getCui(),
                caso,
                ip,
                "El Auxiliar aceptó el caso"
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

        // Buscamos, con un for normal, al técnico con menos casos "En Atención"
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

        caso.setTecnico(tecnicoElegido);
        caso.setEstado(enAtencion);
        casoRepository.save(caso);

        bitacoraCambioService.registrar(
                "Asignación de Técnico",
                auxiliar.getCui(),
                caso,
                ip,
                "Caso asignado al Técnico " + tecnicoElegido.getNombre()
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

            emailService.enviarCorreo(
                    caso.getCiudadano().getCorreo(),
                    "Su caso requiere ampliación de información - Municipalidad de Granados",
                    "Su caso " + caso.getCodigoSeguimiento() + " requiere ampliación de información. "
                            + "Comentario del Auxiliar: " + caso.getComentarioRechazo()
            );

            bitacoraCambioService.registrar(
                    "Rechazo con ampliación",
                    auxiliar.getCui(),
                    caso,
                    ip,
                    "Motivo: " + motivo.getMotivo()
            );
        } else {
            // FA02: rechazo normal, queda pendiente de que el Supervisor lo apruebe
            EstadoCaso pendienteAprobacion = buscarEstado(EstadoCaso.PENDIENTE_APROBACION_RECHAZO);
            caso.setEstado(pendienteAprobacion);
            casoRepository.save(caso);

            bitacoraCambioService.registrar(
                    "Rechazo de caso",
                    auxiliar.getCui(),
                    caso,
                    ip,
                    "Motivo: " + motivo.getMotivo()
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