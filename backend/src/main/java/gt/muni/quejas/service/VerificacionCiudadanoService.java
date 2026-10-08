package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCiudadano;
import gt.muni.quejas.dto.DtoConfirmarVerificacion;
import gt.muni.quejas.dto.DtoSolicitudVerificacion;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Ciudadano;
import gt.muni.quejas.model.TokenVerificacionCiudadano;
import gt.muni.quejas.repository.CiudadanoRepository;
import gt.muni.quejas.repository.TokenVerificacionCiudadanoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VerificacionCiudadanoService {

    public static final int MINUTOS_EXPIRACION_CODIGO = 5;
    public static final int MINUTOS_EXPIRACION_SESION = 30;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CatalogoCacheService catalogoCacheService;
    private final CiudadanoRepository ciudadanoRepository;
    private final TokenVerificacionCiudadanoRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final BitacoraCambioService bitacoraCambioService;

    public VerificacionCiudadanoService(CatalogoCacheService catalogoCacheService,
                                        CiudadanoRepository ciudadanoRepository,
                                        TokenVerificacionCiudadanoRepository tokenRepository,
                                        PasswordEncoder passwordEncoder,
                                        EmailService emailService,
                                        BitacoraCambioService bitacoraCambioService) {
        this.catalogoCacheService = catalogoCacheService;
        this.ciudadanoRepository = ciudadanoRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    @Transactional
    public void solicitarCodigo(DtoSolicitudVerificacion dto) {
        String dpi = normalizarDpi(dto == null ? null : dto.dpi());
        validarFormatoDpi(dpi);

        Ciudadano ciudadano = resolverCiudadano(dpi);

        tokenRepository.findByCiudadanoAndUsadoFalse(ciudadano).forEach(token -> {
            token.setUsado(true);
            tokenRepository.save(token);
        });

        String codigo = generarCodigo();
        TokenVerificacionCiudadano token = new TokenVerificacionCiudadano();
        token.setCiudadano(ciudadano);
        token.setCodigoHash(passwordEncoder.encode(codigo));
        token.setFechaCreacion(LocalDateTime.now());
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION_CODIGO));
        token.setUsado(false);
        tokenRepository.save(token);

        emailService.enviarCorreo(
                ciudadano.getCorreo(),
                "Código de verificación - Municipalidad de Granados",
                "Hola " + ciudadano.getNombre() + ",\n\n"
                        + "Su código de verificación es: " + codigo + "\n"
                        + "Este código vence en " + MINUTOS_EXPIRACION_CODIGO + " minutos.\n\n"
                        + "Municipalidad de Granados, B.V."
        );
    }

    @Transactional
    public DtoCiudadano confirmarCodigo(DtoConfirmarVerificacion dto, String ipOrigen) {
        String dpi = normalizarDpi(dto == null ? null : dto.dpi());
        try {
            validarFormatoDpi(dpi);
            if (dto == null || dto.codigo() == null || dto.codigo().isBlank()) {
                throw new NegocioException(CodigoError.CODIGO_VERIFICACION_INVALIDO);
            }

            Ciudadano ciudadano = resolverCiudadano(dpi);

            List<TokenVerificacionCiudadano> tokens = tokenRepository.findByCiudadanoAndUsadoFalse(ciudadano);
            TokenVerificacionCiudadano vigente = tokens.stream()
                    .filter(TokenVerificacionCiudadano::codigoVigente)
                    .filter(token -> passwordEncoder.matches(dto.codigo().trim(), token.getCodigoHash()))
                    .findFirst()
                    .orElseThrow(() -> new NegocioException(CodigoError.CODIGO_VERIFICACION_INVALIDO));

            vigente.setUsado(true);
            vigente.setTokenSesion(UUID.randomUUID().toString());
            vigente.setFechaExpiracionSesion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION_SESION));
            vigente.setSesionUsada(false);
            tokenRepository.save(vigente);

            bitacoraCambioService.registrarIngreso(
                    BitacoraCambioService.INGRESO_EXITOSO, dpi, ipOrigen, "Ciudadano");

            return new DtoCiudadano(
                    ciudadano.getDpi(),
                    ciudadano.getNombre(),
                    ciudadano.getTelefono(),
                    ciudadano.getCorreo(),
                    vigente.getTokenSesion()
            );
        } catch (NegocioException ex) {
            bitacoraCambioService.registrarIngreso(
                    BitacoraCambioService.INGRESO_FALLIDO, dpi, ipOrigen, "Ciudadano");
            throw ex;
        }
    }

    @Transactional
    public Ciudadano validarSesion(String tokenSesion) {
        return buscarSesionVigente(tokenSesion).getCiudadano();
    }

    @Transactional
    public void cerrarSesion(String tokenSesion, String ipOrigen) {
        if (tokenSesion == null || tokenSesion.isBlank()) {
            return;
        }
        tokenRepository.findByTokenSesion(tokenSesion.trim()).ifPresent(token -> {
            token.setSesionUsada(true);
            tokenRepository.save(token);

            // CU-15: el cierre de sesión del ciudadano queda en bitácora
            bitacoraCambioService.registrarIngreso(
                    BitacoraCambioService.CIERRE_SESION, token.getCiudadano().getDpi(), ipOrigen, "Ciudadano");
        });
    }

    private TokenVerificacionCiudadano buscarSesionVigente(String tokenSesion) {
        if (tokenSesion == null || tokenSesion.isBlank()) {
            throw new NegocioException(CodigoError.SESION_REGISTRO_INVALIDA);
        }
        TokenVerificacionCiudadano token = tokenRepository.findByTokenSesion(tokenSesion.trim())
                .orElseThrow(() -> new NegocioException(CodigoError.SESION_REGISTRO_INVALIDA));
        if (!token.sesionVigente()) {
            throw new NegocioException(CodigoError.SESION_REGISTRO_INVALIDA);
        }
        return token;
    }

    public static void validarFormatoDpi(String dpi) {
        if (dpi == null || !dpi.matches("\\d{13}")) {
            throw new NegocioException(CodigoError.DPI_INVALIDO);
        }
    }

    public static String normalizarDpi(String dpi) {
        return dpi == null ? null : dpi.trim();
    }

    private Ciudadano resolverCiudadano(String dpi) {
        catalogoCacheService.buscarCiudadano(dpi)
                .orElseThrow(() -> new NegocioException(CodigoError.CIUDADANO_NO_ENCONTRADO));
        return ciudadanoRepository.findById(dpi)
                .orElseThrow(() -> new NegocioException(CodigoError.CIUDADANO_NO_ENCONTRADO));
    }

    private String generarCodigo() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
