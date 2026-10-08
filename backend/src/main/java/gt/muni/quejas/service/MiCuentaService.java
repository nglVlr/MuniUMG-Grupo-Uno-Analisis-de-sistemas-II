package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoActualizarCuenta;
import gt.muni.quejas.dto.DtoCambioPassword;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoMiCuenta;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.UsuarioRepository;
import gt.muni.quejas.util.PasswordRn08;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// CU-12 Administrar Cuenta: cada usuario interno consulta y actualiza sus
// propios datos y cambia su contraseña.
@Service
public class MiCuentaService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_DATOS_ACTUALIZADOS = "Sus datos fueron actualizados exitosamente.";

    private static final int MAXIMO_NOMBRE = 150;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraCambioService bitacoraCambioService;

    public MiCuentaService(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           BitacoraCambioService bitacoraCambioService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    // ---------- CU-12 paso 2: el sistema muestra los datos del usuario ----------
    @Transactional(readOnly = true)
    public DtoMiCuenta obtenerMiCuenta(Usuario usuario) {
        Usuario actual = recargar(usuario);

        return new DtoMiCuenta(
                actual.getCui(),
                actual.getNombre(),
                actual.getCorreo(),
                actual.getIdRol() == null ? "" : actual.getIdRol().getRol(),
                actual.getIdDepartamento() == null ? "" : actual.getIdDepartamento().getNombre()
        );
    }

    // ---------- CU-12 pasos 4 al 8: el usuario guarda sus datos ----------
    @Transactional
    public DtoMensaje actualizarDatos(DtoActualizarCuenta dto, Usuario usuario, String ip) {
        if (dto == null || dto.nombre() == null || dto.nombre().isBlank()) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        String nombre = dto.nombre().trim();
        if (nombre.length() > MAXIMO_NOMBRE) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }

        Usuario actual = recargar(usuario);
        actual.setNombre(nombre);
        usuarioRepository.save(actual);

        bitacoraCambioService.registrar(
                "Actualización de cuenta",
                actual.getCui(),
                null,
                ip,
                "El usuario actualizó sus datos"
        );

        return new DtoMensaje(MENSAJE_DATOS_ACTUALIZADOS);
    }

    // ---------- CU-12 FA01: cambio de contraseña ----------
    @Transactional
    public DtoMensaje cambiarPassword(DtoCambioPassword dto, Usuario usuario, String ip) {
        if (dto == null || vacio(dto.passwordActual())
                || vacio(dto.passwordNueva()) || vacio(dto.passwordConfirmacion())) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }

        Usuario actual = recargar(usuario);

        // FA02: la contraseña actual debe coincidir con la que ya tiene
        if (!passwordEncoder.matches(dto.passwordActual(), actual.getPassword())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
        if (!dto.passwordNueva().equals(dto.passwordConfirmacion())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
        // RN08: alfanumerica y de minimo 6 caracteres, igual que al iniciar sesion
        if (!PasswordRn08.esValida(dto.passwordNueva())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }

        actual.setPassword(passwordEncoder.encode(dto.passwordNueva()));
        usuarioRepository.save(actual);

        bitacoraCambioService.registrar(
                "Cambio de contraseña",
                actual.getCui(),
                null,
                ip,
                "El usuario cambió su contraseña"
        );

        return new DtoMensaje(MENSAJE_DATOS_ACTUALIZADOS);
    }

    // ---------- Métodos de apoyo ----------
    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // El principal del token viene desconectado, por eso lo recargamos
    private Usuario recargar(Usuario usuario) {
        if (usuario == null) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        return usuarioRepository.findByCorreo(usuario.getCorreo())
                .orElseThrow(() -> new NegocioException(CodigoError.ACCESO_DENEGADO));
    }
}
