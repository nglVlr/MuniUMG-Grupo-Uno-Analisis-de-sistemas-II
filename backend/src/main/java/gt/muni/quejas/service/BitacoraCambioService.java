package gt.muni.quejas.service;

import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

// CU-15 Bitácora de actividades: registra quién hizo qué, sobre qué entidad,
// desde qué IP y cuándo. Nunca interrumpe la operación principal.
@Service
public class BitacoraCambioService {

    private static final Logger log = LoggerFactory.getLogger(BitacoraCambioService.class);

    public static final String INGRESO_EXITOSO = "Inicio de sesión";
    public static final String INGRESO_FALLIDO = "Inicio de sesión fallido";
    public static final String CIERRE_SESION = "Cierre de sesión";

    public static final String ENTIDAD_CASO = "Caso";
    public static final String ENTIDAD_USUARIO = "Usuario";
    public static final String ENTIDAD_CATALOGO = "Catálogo";
    public static final String ENTIDAD_SESION = "Sesión";
    public static final String ROL_CIUDADANO = "Ciudadano";

    private final BitacoraCambioRepository bitacoraCambioRepository;
    private final UsuarioRepository usuarioRepository;

    public BitacoraCambioService(BitacoraCambioRepository bitacoraCambioRepository,
                                 UsuarioRepository usuarioRepository) {
        this.bitacoraCambioRepository = bitacoraCambioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIngreso(String accion, String usuario, String ipOrigen, String detalle) {
        String quien = usuario == null ? "" : usuario.trim();
        if (quien.length() > 50) {
            quien = quien.substring(0, 50);
        }
        if (quien.isBlank()) {
            quien = "sin usuario";
        }
        String ip = ipOrigen == null ? "" : ipOrigen.trim();
        if (ip.isBlank()) {
            ip = "0.0.0.0";
        } else if (ip.length() > 50) {
            ip = ip.substring(0, 50);
        }
        guardar(accion, quien, ENTIDAD_SESION, null, ip, detalle, null, null);
    }

    // Registro simple, sin valores anterior y nuevo
    public void registrar(String accion, String usuario, Caso caso, String ipOrigen, String detalle) {
        String entidad = caso == null ? ENTIDAD_SESION : ENTIDAD_CASO;
        guardar(accion, usuario, entidad, caso, ipOrigen, detalle, null, null);
    }

    // Registro sobre una entidad concreta, sin valores anterior y nuevo
    public void registrarCambio(String accion, String usuario, String entidad, Caso caso,
                                String ipOrigen, String detalle) {
        guardar(accion, usuario, entidad, caso, ipOrigen, detalle, null, null);
    }

    // Registro de un cambio, con lo que había antes y lo que quedó después
    public void registrarCambio(String accion, String usuario, String entidad, Caso caso,
                                String ipOrigen, String detalle, String valorAnterior, String valorNuevo) {
        guardar(accion, usuario, entidad, caso, ipOrigen, detalle, valorAnterior, valorNuevo);
    }

    private void guardar(String accion, String usuario, String entidad, Caso caso,
                         String ipOrigen, String detalle, String valorAnterior, String valorNuevo) {
        try {
            BitacoraCambio registro = new BitacoraCambio();
            registro.setAccion(accion);
            registro.setUsuario(usuario == null || usuario.isBlank() ? ROL_CIUDADANO : usuario);
            registro.setRol(buscarRol(usuario));
            registro.setEntidad(entidad);
            registro.setCaso(caso);
            registro.setFecha(LocalDate.now());
            // Sin nanosegundos: la hora se muestra y se exporta tal como se guarda
            registro.setHora(LocalTime.now().withNano(0));
            registro.setIpOrigen(ipOrigen);
            registro.setDetalle(detalle);
            registro.setValorAnterior(recortar(valorAnterior));
            registro.setValorNuevo(recortar(valorNuevo));
            bitacoraCambioRepository.save(registro);
        } catch (Exception ex) {
            // La bitácora nunca debe tumbar la operación que la invocó
            log.error("No fue posible registrar en bitácora la acción {}", accion, ex);
        }
    }

    // El usuario puede ser el CUI de un empleado o el DPI de un ciudadano
    private String buscarRol(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return ROL_CIUDADANO;
        }
        return usuarioRepository.findByCui(usuario.trim())
                .map(u -> u.getIdRol() == null ? ROL_CIUDADANO : u.getIdRol().getRol())
                .orElseGet(() -> usuarioRepository.findByCorreo(usuario.trim())
                        .map(u -> u.getIdRol() == null ? ROL_CIUDADANO : u.getIdRol().getRol())
                        .orElse(ROL_CIUDADANO));
    }

    private String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.length() > 250 ? valor.substring(0, 250) : valor;
    }
}
