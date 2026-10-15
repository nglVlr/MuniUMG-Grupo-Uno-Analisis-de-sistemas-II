package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoActividad;
import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoCasoDetalleAuxiliar;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TecnicoService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_ACTIVIDAD_REGISTRADA = "La actividad fue registrada correctamente.";
    public static final String MENSAJE_CASO_PROCESADO = "El caso fue procesado exitosamente.";

    private static final String ROL_TECNICO = "Tecnico";
    private static final int MAXIMO_DETALLE = 500;

    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final ActividadCasoRepository actividadCasoRepository;
    private final EvidenciaStorageService evidenciaStorageService;
    private final InformeAtencionService informeAtencionService;
    private final BitacoraCambioService bitacoraCambioService;

    public TecnicoService(CasoRepository casoRepository,
                          EstadoCasoRepository estadoCasoRepository,
                          UsuarioRepository usuarioRepository,
                          EvidenciaRepository evidenciaRepository,
                          ActividadCasoRepository actividadCasoRepository,
                          EvidenciaStorageService evidenciaStorageService,
                          InformeAtencionService informeAtencionService,
                          BitacoraCambioService bitacoraCambioService) {
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.usuarioRepository = usuarioRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.actividadCasoRepository = actividadCasoRepository;
        this.evidenciaStorageService = evidenciaStorageService;
        this.informeAtencionService = informeAtencionService;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    // ---------- CU-05 paso 5: formulario asignación técnico ----------
    @Transactional(readOnly = true)
    public List<DtoCasoBandeja> listarCasosAsignados(Usuario tecnico) {
        validarRolTecnico(tecnico);

        EstadoCaso enAtencion = buscarEstado(EstadoCaso.EN_ATENCION);
        List<Caso> casos = casoRepository.findByTecnicoAndEstadoOrderByCreateAtAsc(tecnico, enAtencion);

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

    // ---------- CU-05 paso 7: formulario detalle técnico ----------
    @Transactional(readOnly = true)
    public DtoCasoDetalleAuxiliar obtenerDetalle(Long idCaso, Usuario tecnico) {
        Caso caso = buscarCasoEnAtencion(idCaso, tecnico);

        // El botón Evidencia muestra el archivo del ciudadano, no el soporte de atención
        boolean tieneEvidencia = evidenciaRepository.existsByCasoIdAndTipo(caso.getId(), Evidencia.TIPO_REGISTRO);

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

    // ---------- CU-05 paso 10: formulario de actividades (RN19) ----------
    @Transactional(readOnly = true)
    public List<DtoActividad> listarActividades(Long idCaso, Usuario tecnico) {
        Caso caso = buscarCasoEnAtencion(idCaso, tecnico);
        List<ActividadCaso> actividades = actividadCasoRepository.findByCasoIdOrderByFechaAsc(caso.getId());

        List<DtoActividad> resultado = new ArrayList<>();
        for (int i = 0; i < actividades.size(); i++) {
            ActividadCaso actividad = actividades.get(i);
            resultado.add(new DtoActividad(
                    caso.getCodigoSeguimiento(),
                    actividad.getId(),
                    actividad.getDetalle(),
                    actividad.getFecha()
            ));
        }
        return resultado;
    }

    // ---------- CU-05 pasos 13 al 18: nueva actividad (RN20) ----------
    @Transactional
    public DtoMensaje registrarActividad(Long idCaso, String detalle, Usuario tecnico, String ip) {
        Caso caso = buscarCasoEnAtencion(idCaso, tecnico);
        String detalleLimpio = validarDetalle(detalle, CodigoError.CAMPOS_OBLIGATORIOS);

        guardarActividad(caso, tecnico, detalleLimpio);

        bitacoraCambioService.registrar(
                "Registro de actividad",
                tecnico.getCui(),
                caso,
                ip,
                "El Técnico registró una actividad del caso"
        );

        return new DtoMensaje(MENSAJE_ACTIVIDAD_REGISTRADA);
    }

    // ---------- CU-05 FA02: Finalizar caso ----------
    @Transactional
    public DtoMensaje finalizarAtencion(Long idCaso, String detalle, MultipartFile soporte, Usuario tecnico, String ip) {
        Caso caso = buscarCasoEnAtencion(idCaso, tecnico);

        // El soporte es obligatorio al finalizar (RN20)
        if (soporte == null || soporte.isEmpty()) {
            throw new NegocioException(CodigoError.REPORTE_ATENCION_INCOMPLETO);
        }
        String detalleLimpio = validarDetalle(detalle, CodigoError.REPORTE_ATENCION_INCOMPLETO);

        guardarActividad(caso, tecnico, detalleLimpio);
        evidenciaStorageService.guardarSiPresente(caso, soporte, Evidencia.TIPO_ATENCION);

        // FA02 paso 6: el caso pasa a En Supervisión (RN04)
        String estadoAnterior = caso.getEstado().getEstado();
        EstadoCaso enSupervision = buscarEstado(EstadoCaso.EN_SUPERVISION);
        caso.setEstado(enSupervision);

        // FA02 paso 7: reporte de atención del caso (AN04 # 2)
        List<ActividadCaso> actividades = actividadCasoRepository.findByCasoIdOrderByFechaAsc(caso.getId());
        List<String> nombresEvidencia = buscarNombresEvidencia(caso);
        caso.setInformeAtencionRuta(informeAtencionService.generar(caso, actividades, nombresEvidencia));
        casoRepository.save(caso);

        // El caso deja de estar En Atención, así que baja la carga del Técnico
        descontarCasoDelTecnico(tecnico);

        bitacoraCambioService.registrarCambio(
                "Finalización de atención",
                tecnico.getCui(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "El Técnico finalizó la atención y el caso pasó a En Supervisión",
                estadoAnterior,
                EstadoCaso.EN_SUPERVISION
        );

        return new DtoMensaje(MENSAJE_CASO_PROCESADO);
    }

    // ---------- CU-05 botón Evidencia: archivo adjunto del ciudadano ----------
    @Transactional(readOnly = true)
    public Evidencia obtenerEvidenciaRegistro(Long idCaso, Usuario tecnico) {
        Caso caso = buscarCasoEnAtencion(idCaso, tecnico);
        return evidenciaRepository.findFirstByCasoIdAndTipo(caso.getId(), Evidencia.TIPO_REGISTRO)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));
    }

    // ---------- Métodos de apoyo ----------
    private List<String> buscarNombresEvidencia(Caso caso) {
        List<Evidencia> evidencias = evidenciaRepository.findByCasoIdOrderByIdAsc(caso.getId());

        List<String> nombres = new ArrayList<>();
        for (int i = 0; i < evidencias.size(); i++) {
            nombres.add(evidencias.get(i).getNombreArchivo());
        }
        return nombres;
    }

    private void guardarActividad(Caso caso, Usuario tecnico, String detalle) {
        ActividadCaso actividad = new ActividadCaso();
        actividad.setCaso(caso);
        actividad.setTecnico(tecnico);
        actividad.setDetalle(detalle);
        actividad.setFecha(LocalDateTime.now());
        actividadCasoRepository.save(actividad);
    }

    // El principal del token viene desconectado, por eso recargamos el Técnico
    // desde el repositorio antes de bajarle el contador. El contador se deja igual a
    // los casos que realmente le quedan En Atención, que ya no incluyen este caso.
    private void descontarCasoDelTecnico(Usuario tecnico) {
        Usuario tecnicoActual = usuarioRepository.findByCorreo(tecnico.getCorreo())
                .orElseThrow(() -> new NegocioException(CodigoError.ACCESO_DENEGADO));

        long cantidad = casoRepository.countByTecnicoAndEstado(
                tecnicoActual, buscarEstado(EstadoCaso.EN_ATENCION));
        tecnicoActual.setCantidadCasosAsignados((int) cantidad);
        usuarioRepository.save(tecnicoActual);
    }

    private String validarDetalle(String detalle, CodigoError codigoError) {
        if (detalle == null || detalle.isBlank()) {
            throw new NegocioException(codigoError);
        }
        String detalleLimpio = detalle.trim();
        if (detalleLimpio.length() > MAXIMO_DETALLE) {
            throw new NegocioException(codigoError);
        }
        return detalleLimpio;
    }

    private Caso buscarCasoEnAtencion(Long idCaso, Usuario tecnico) {
        validarRolTecnico(tecnico);

        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        if (!EstadoCaso.EN_ATENCION.equals(caso.getEstado().getEstado())) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR);
        }
        if (caso.getTecnico() == null || !caso.getTecnico().getCui().equals(tecnico.getCui())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        return caso;
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolTecnico(Usuario tecnico) {
        if (tecnico == null || tecnico.getIdRol() == null
                || !ROL_TECNICO.equals(tecnico.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }

    private EstadoCaso buscarEstado(String nombreEstado) {
        return estadoCasoRepository.findByEstado(nombreEstado)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado " + nombreEstado));
    }
}
