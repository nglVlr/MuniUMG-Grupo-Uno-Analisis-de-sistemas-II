package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoConfirmarRecuperacion;
import gt.muni.quejas.dto.DtoSolicitudRecuperacion;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.TokenRecuperacion;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.TokenRecuperacionRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RecuperacionPasswordService {
    private static final int MINUTOS_EXPIRACION = 3;
    private static final String URL_BASE_RESTABLECER = "http://localhost:8081/restablecer-password?token=";

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public RecuperacionPasswordService(UsuarioRepository usuarioRepository,
                                       TokenRecuperacionRepository tokenRecuperacionRepository,
                                       PasswordEncoder passwordEncoder,
                                       EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void solicitarRecuperacion(DtoSolicitudRecuperacion dto) {
        usuarioRepository.findByCorreo(dto.correo()).ifPresent(usuario -> {
            invalidarTokensAnteriores(usuario);

            TokenRecuperacion token = new TokenRecuperacion();
            token.setToken(UUID.randomUUID().toString());
            token.setUsuario(usuario);
            token.setFechaCreacion(LocalDateTime.now());
            token.setFechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION));
            token.setUsado(false);
            tokenRecuperacionRepository.save(token);

            enviarCorreoRecuperacion(usuario, token.getToken());
        });
    }

    @Transactional
    public void confirmarRecuperacion(DtoConfirmarRecuperacion dto) {
        if (!dto.password().equals(dto.confirmarPassword())) {
            throw new NegocioException(CodigoError.VALIDACION, "Las contraseñas no coinciden");
        }

        TokenRecuperacion token = tokenRecuperacionRepository.findByToken(dto.token())
                .orElseThrow(() -> new NegocioException(CodigoError.TOKEN_RECUPERACION_INVALIDO));

        if (!token.esValido()) {
            throw new NegocioException(CodigoError.TOKEN_RECUPERACION_INVALIDO);
        }

        Usuario usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRecuperacionRepository.save(token);
    }

    private void invalidarTokensAnteriores(Usuario usuario) {
        var tokensAnteriores = tokenRecuperacionRepository.findByUsuarioAndUsadoFalse(usuario);
        tokensAnteriores.forEach(t -> t.setUsado(true));
        tokenRecuperacionRepository.saveAll(tokensAnteriores);
    }

    private void enviarCorreoRecuperacion(Usuario usuario, String token) {
        String enlace = URL_BASE_RESTABLECER + token;
        String asunto = "Recuperación de contraseña - Sistema de Quejas";
        String cuerpo = "Hola " + usuario.getNombre() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña. "
                + "Este enlace es válido por " + MINUTOS_EXPIRACION + " minutos:\n\n"
                + enlace + "\n\n"
                + "Si no solicitaste este cambio, ignora este correo.";

        emailService.enviarCorreo(usuario.getCorreo(), asunto, cuerpo);
    }
}
