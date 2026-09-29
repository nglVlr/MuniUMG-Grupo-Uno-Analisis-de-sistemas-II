package gt.muni.quejas.service;

import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class BitacoraCambioService {

    private final BitacoraCambioRepository bitacoraCambioRepository;

    public BitacoraCambioService(BitacoraCambioRepository bitacoraCambioRepository) {
        this.bitacoraCambioRepository = bitacoraCambioRepository;
    }

    public static final String INGRESO_EXITOSO = "Inicio de sesión";
    public static final String INGRESO_FALLIDO = "Inicio de sesión fallido";

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
        registrar(accion, quien, null, ip, detalle);
    }

    public void registrar(String accion, String usuario, Caso caso, String ipOrigen, String detalle) {
        BitacoraCambio registro = new BitacoraCambio();
        registro.setAccion(accion);
        registro.setUsuario(usuario == null || usuario.isBlank() ? "Ciudadano" : usuario);
        registro.setCaso(caso);
        registro.setFecha(LocalDate.now());
        registro.setHora(LocalTime.now());
        registro.setIpOrigen(ipOrigen);
        registro.setDetalle(detalle);
        bitacoraCambioRepository.save(registro);
    }
}
