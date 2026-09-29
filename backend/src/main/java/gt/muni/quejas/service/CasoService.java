package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoConsultaCasoRespuesta;
import gt.muni.quejas.dto.DtoRegistroCasoRespuesta;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Categoria;
import gt.muni.quejas.model.Ciudadano;
import gt.muni.quejas.model.CorrelativoSeguimiento;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.CorrelativoSeguimientoRepository;
import gt.muni.quejas.repository.EstadoCasoRepository;
import gt.muni.quejas.util.TipoCaso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class CasoService {

    public static final String MENSAJE_EXITO = "Su caso fue registrado exitosamente. Su código de seguimiento es: %s. Consérvelo para darle seguimiento.";
    private static final Pattern CODIGO_CASO = Pattern.compile("MG-\\d{4}-\\d{4}");

    private final VerificacionCiudadanoService verificacionCiudadanoService;
    private final CatalogoCacheService catalogoCacheService;
    private final EstadoCasoRepository estadoCasoRepository;
    private final CorrelativoSeguimientoRepository correlativoSeguimientoRepository;
    private final CasoRepository casoRepository;
    private final EvidenciaStorageService evidenciaStorageService;
    private final BitacoraCambioService bitacoraCambioService;
    private final NotificacionCasoService notificacionCasoService;

    public CasoService(VerificacionCiudadanoService verificacionCiudadanoService,
                       CatalogoCacheService catalogoCacheService,
                       EstadoCasoRepository estadoCasoRepository,
                       CorrelativoSeguimientoRepository correlativoSeguimientoRepository,
                       CasoRepository casoRepository,
                       EvidenciaStorageService evidenciaStorageService,
                       BitacoraCambioService bitacoraCambioService,
                       NotificacionCasoService notificacionCasoService) {
        this.verificacionCiudadanoService = verificacionCiudadanoService;
        this.catalogoCacheService = catalogoCacheService;
        this.estadoCasoRepository = estadoCasoRepository;
        this.correlativoSeguimientoRepository = correlativoSeguimientoRepository;
        this.casoRepository = casoRepository;
        this.evidenciaStorageService = evidenciaStorageService;
        this.bitacoraCambioService = bitacoraCambioService;
        this.notificacionCasoService = notificacionCasoService;
    }

    @Transactional
    public DtoRegistroCasoRespuesta registrar(String tokenSesion,
                                              String tipoCaso,
                                              Long idCategoria,
                                              String direccionProblema,
                                              String descripcion,
                                              MultipartFile evidencia,
                                              String ipOrigen) {
        Ciudadano ciudadano = verificacionCiudadanoService.validarSesion(tokenSesion);
        validarFormulario(tipoCaso, idCategoria, direccionProblema, descripcion);

        Categoria categoria = catalogoCacheService.obtenerCategoriaActiva(idCategoria)
                .orElseThrow(() -> new NegocioException(CodigoError.CATEGORIA_REQUERIDA));
        if (!categoria.admiteTipo(tipoCaso)) {
            throw new NegocioException(CodigoError.CATEGORIA_REQUERIDA);
        }

        EstadoCaso estadoRegistrado = estadoCasoRepository.findByEstado(EstadoCaso.REGISTRADO)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado Registrado"));

        String codigo = generarCodigoSeguimiento();

        Caso caso = new Caso();
        caso.setCodigoSeguimiento(codigo);
        caso.setCiudadano(ciudadano);
        caso.setTipoCaso(tipoCaso);
        caso.setCategoria(categoria);
        caso.setDepartamento(categoria.getDepartamento());
        caso.setDireccion(direccionProblema.trim());
        caso.setDescripcion(descripcion.trim());
        caso.setEstado(estadoRegistrado);
        caso.setIpOrigen(ipOrigen);
        caso.setCreateAt(LocalDateTime.now());
        caso = casoRepository.save(caso);

        evidenciaStorageService.guardarSiPresente(caso, evidencia);

        notificacionCasoService.notificarRegistro(caso);

        String mensaje = String.format(MENSAJE_EXITO, codigo);

        bitacoraCambioService.registrar(
                "Registro de caso",
                ciudadano.getDpi(),
                caso,
                ipOrigen,
                codigo
        );

        return new DtoRegistroCasoRespuesta(
                caso.getId(),
                codigo,
                mensaje
        );
    }

    private void validarFormulario(String tipoCaso, Long idCategoria, String direccion, String descripcion) {
        boolean tipoVacio = tipoCaso == null || tipoCaso.isBlank();
        boolean categoriaVacia = idCategoria == null;
        boolean direccionVacia = direccion == null || direccion.isBlank();
        boolean descripcionVacia = descripcion == null || descripcion.isBlank();

        if (tipoVacio && categoriaVacia && direccionVacia && descripcionVacia) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        if (tipoVacio || !TipoCaso.esValido(tipoCaso)) {
            throw new NegocioException(CodigoError.TIPO_CASO_REQUERIDO);
        }
        if (categoriaVacia) {
            throw new NegocioException(CodigoError.CATEGORIA_REQUERIDA);
        }
        if (direccionVacia) {
            throw new NegocioException(CodigoError.DIRECCION_REQUERIDA);
        }
        if (direccion.trim().length() > 250 || descripcionVacia || descripcion.trim().length() > 2000) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
    }

    private String generarCodigoSeguimiento() {
        int anio = LocalDate.now().getYear();
        CorrelativoSeguimiento correlativo = correlativoSeguimientoRepository.lockByAnio(anio)
                .orElseGet(() -> {
                    CorrelativoSeguimiento nuevo = new CorrelativoSeguimiento();
                    nuevo.setAnio(anio);
                    nuevo.setUltimo(0);
                    return correlativoSeguimientoRepository.save(nuevo);
                });
        correlativo.setUltimo(correlativo.getUltimo() + 1);
        correlativoSeguimientoRepository.save(correlativo);
        return String.format("MG-%04d-%d", correlativo.getUltimo(), anio);
    }


    @Transactional(readOnly = true)
    public DtoConsultaCasoRespuesta consultarEstado(String codigoSeguimiento) {
        String codigo = codigoSeguimiento == null ? "" : codigoSeguimiento.trim();
        if (!CODIGO_CASO.matcher(codigo).matches()) {
            throw new NegocioException(CodigoError.CASO_NO_ENCONTRADO);
        }

        Caso caso = casoRepository.findByCodigoSeguimiento(codigo)
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO));

        return new DtoConsultaCasoRespuesta(
                caso.getCodigoSeguimiento(),
                caso.getEstado().getEstado(),
                caso.getCreateAt(),
                caso.getDepartamento().getNombre(),
                caso.getDescripcion(),
                ""
        );
    }



}
