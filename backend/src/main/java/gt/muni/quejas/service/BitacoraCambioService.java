package gt.muni.quejas.service;

import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class BitacoraCambioService {

    private final BitacoraCambioRepository bitacoraCambioRepository;

    public BitacoraCambioService(BitacoraCambioRepository bitacoraCambioRepository) {
        this.bitacoraCambioRepository = bitacoraCambioRepository;
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
