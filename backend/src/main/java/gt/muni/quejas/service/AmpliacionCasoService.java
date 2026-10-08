package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCasoAmpliacion;
import gt.muni.quejas.dto.DtoCasoBandeja;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Ciudadano;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.EstadoCasoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

// CU-18 Ampliación de casos: el ciudadano completa la información que le pidió
// el Auxiliar y reenvía el caso a revisión.
@Service
public class AmpliacionCasoService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_CASO_REENVIADO =
            "Su caso fue reenviado exitosamente y se encuentra nuevamente en revisión.";

    private static final int MAXIMO_INFORMACION = 2000;

    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final VerificacionCiudadanoService verificacionCiudadanoService;
    private final EvidenciaStorageService evidenciaStorageService;
    private final BitacoraCambioService bitacoraCambioService;

    public AmpliacionCasoService(CasoRepository casoRepository,
                                 EstadoCasoRepository estadoCasoRepository,
                                 VerificacionCiudadanoService verificacionCiudadanoService,
                                 EvidenciaStorageService evidenciaStorageService,
                                 BitacoraCambioService bitacoraCambioService) {
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.verificacionCiudadanoService = verificacionCiudadanoService;
        this.evidenciaStorageService = evidenciaStorageService;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    // ---------- CU-18 pasos 4 y 5: casos del ciudadano que requieren ampliación ----------
    @Transactional(readOnly = true)
    public List<DtoCasoBandeja> listarCasosEnAmpliacion(String tokenSesion) {
        Ciudadano ciudadano = verificacionCiudadanoService.validarSesion(tokenSesion);

        EstadoCaso requiereAmpliacion = buscarEstado(EstadoCaso.REQUIERE_AMPLIACION);
        List<Caso> casos = casoRepository.findByCiudadanoDpiAndEstadoOrderByCreateAtAsc(
                ciudadano.getDpi(), requiereAmpliacion);

        // FA02: no existen casos que requieran ampliación
        if (casos.isEmpty()) {
            throw new NegocioException(CodigoError.SIN_CASOS_AMPLIACION);
        }

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

    // ---------- CU-18 paso 7: detalle del caso y las instrucciones del Auxiliar ----------
    @Transactional(readOnly = true)
    public DtoCasoAmpliacion obtenerDetalle(Long idCaso, String tokenSesion) {
        Caso caso = buscarCasoDelCiudadano(idCaso, tokenSesion);

        return new DtoCasoAmpliacion(
                caso.getId(),
                caso.getCodigoSeguimiento(),
                caso.getCreateAt(),
                caso.getTipoCaso(),
                caso.getCategoria().getNombre(),
                caso.getDescripcion(),
                caso.getDireccion(),
                caso.getEstado().getEstado(),
                caso.getMotivoRechazo() == null ? "" : caso.getMotivoRechazo().getMotivo(),
                caso.getComentarioRechazo() == null ? "" : caso.getComentarioRechazo(),
                caso.getInformacionAmpliacion() == null ? "" : caso.getInformacionAmpliacion()
        );
    }

    // ---------- CU-18 pasos 9 al 14: el ciudadano reenvía el caso ----------
    @Transactional
    public DtoMensaje reenviarCaso(Long idCaso, String tokenSesion, String informacion,
                                   MultipartFile evidencia, String ip) {
        Caso caso = buscarCasoDelCiudadano(idCaso, tokenSesion);

        // FA04: la información solicitada es obligatoria
        if (informacion == null || informacion.isBlank()) {
            throw new NegocioException(CodigoError.INFORMACION_AMPLIACION_REQUERIDA);
        }
        String informacionLimpia = informacion.trim();
        if (informacionLimpia.length() > MAXIMO_INFORMACION) {
            throw new NegocioException(CodigoError.INFORMACION_AMPLIACION_REQUERIDA);
        }

        caso.setInformacionAmpliacion(informacionLimpia);

        // FA03: la evidencia adicional es opcional, y se valida igual que en el registro
        evidenciaStorageService.guardarSiPresente(caso, evidencia, Evidencia.TIPO_REGISTRO);

        // El caso vuelve con el Auxiliar para que lo revise de nuevo (RN04)
        caso.setEstado(buscarEstado(EstadoCaso.EN_REVISION));
        casoRepository.save(caso);

        bitacoraCambioService.registrarCambio(
                "Ampliación de caso",
                caso.getCiudadano().getDpi(),
                BitacoraCambioService.ENTIDAD_CASO,
                caso,
                ip,
                "El ciudadano amplió la información y reenvió el caso a revisión",
                EstadoCaso.REQUIERE_AMPLIACION,
                EstadoCaso.EN_REVISION
        );

        return new DtoMensaje(MENSAJE_CASO_REENVIADO);
    }

    // ---------- Métodos de apoyo ----------
    // El ciudadano solo puede ampliar sus propios casos, y solo los que lo requieren
    private Caso buscarCasoDelCiudadano(Long idCaso, String tokenSesion) {
        Ciudadano ciudadano = verificacionCiudadanoService.validarSesion(tokenSesion);

        Caso caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO));

        if (!caso.getCiudadano().getDpi().equals(ciudadano.getDpi())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        if (!EstadoCaso.REQUIERE_AMPLIACION.equals(caso.getEstado().getEstado())) {
            throw new NegocioException(CodigoError.SIN_CASOS_AMPLIACION);
        }
        return caso;
    }

    private EstadoCaso buscarEstado(String nombreEstado) {
        return estadoCasoRepository.findByEstado(nombreEstado)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado " + nombreEstado));
    }
}
