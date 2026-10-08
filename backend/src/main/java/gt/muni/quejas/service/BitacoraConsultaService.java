package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoBitacora;
import gt.muni.quejas.dto.DtoPaginaBitacora;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// CU-15 Bitácora de actividades: consulta de solo lectura, exclusiva del Admin.
// La bitácora es append-only: no hay forma de modificar ni borrar sus registros.
@Service
public class BitacoraConsultaService {

    private static final String ROL_ADMIN = "Admin";
    private static final int TAMANO_PAGINA = 25;
    private static final int MAXIMO_DIAS_RANGO = 1830;

    private final BitacoraCambioRepository bitacoraCambioRepository;

    public BitacoraConsultaService(BitacoraCambioRepository bitacoraCambioRepository) {
        this.bitacoraCambioRepository = bitacoraCambioRepository;
    }

    // ---------- CU-15 pasos 3 y 4: consulta con filtros ----------
    @Transactional(readOnly = true)
    public DtoPaginaBitacora consultar(LocalDate desde, LocalDate hasta, String usuario,
                                       String accion, String entidad, String rol,
                                       int pagina, Usuario admin) {
        validarRolAdmin(admin);
        validarRango(desde, hasta);

        Page<BitacoraCambio> resultado = bitacoraCambioRepository.consultar(
                desde, hasta,
                vacio(usuario) ? null : "%" + usuario.trim().toLowerCase() + "%",
                vacio(accion) ? null : accion.trim(),
                vacio(entidad) ? null : entidad.trim(),
                vacio(rol) ? null : rol.trim(),
                PageRequest.of(Math.max(pagina, 0), TAMANO_PAGINA));

        List<DtoBitacora> registros = new ArrayList<>();
        List<BitacoraCambio> contenido = resultado.getContent();
        for (int i = 0; i < contenido.size(); i++) {
            BitacoraCambio b = contenido.get(i);
            registros.add(new DtoBitacora(
                    b.getId(),
                    b.getAccion(),
                    b.getUsuario(),
                    b.getRol() == null ? "" : b.getRol(),
                    b.getEntidad() == null ? "" : b.getEntidad(),
                    b.getCaso() == null ? "" : b.getCaso().getCodigoSeguimiento(),
                    b.getFecha(),
                    b.getHora(),
                    b.getIpOrigen(),
                    b.getDetalle() == null ? "" : b.getDetalle(),
                    b.getValorAnterior() == null ? "" : b.getValorAnterior(),
                    b.getValorNuevo() == null ? "" : b.getValorNuevo()));
        }

        return new DtoPaginaBitacora(
                registros,
                resultado.getNumber(),
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }

    // ---------- Acciones existentes, para alimentar el filtro ----------
    @Transactional(readOnly = true)
    public List<String> listarAcciones(Usuario admin) {
        validarRolAdmin(admin);
        return bitacoraCambioRepository.listarAcciones();
    }

    // ---------- CU-15: exportación del listado ----------
    public String exportarCsv(DtoPaginaBitacora pagina) {
        StringBuilder csv = new StringBuilder();
        csv.append("Fecha;Hora;Accion;Usuario;Rol;Entidad;Caso;IP de origen;Valor anterior;Valor nuevo;Detalle\n");

        List<DtoBitacora> registros = pagina.registros();
        for (int i = 0; i < registros.size(); i++) {
            DtoBitacora b = registros.get(i);
            csv.append(b.fecha()).append(';')
                    .append(b.hora()).append(';')
                    .append(limpiar(b.accion())).append(';')
                    .append(limpiar(b.usuario())).append(';')
                    .append(limpiar(b.rol())).append(';')
                    .append(limpiar(b.entidad())).append(';')
                    .append(limpiar(b.codigoSeguimiento())).append(';')
                    .append(limpiar(b.ipOrigen())).append(';')
                    .append(limpiar(b.valorAnterior())).append(';')
                    .append(limpiar(b.valorNuevo())).append(';')
                    .append(limpiar(b.detalle())).append('\n');
        }
        return csv.toString();
    }

    // ---------- Métodos de apoyo ----------
    private String limpiar(String texto) {
        return texto == null ? "" : texto.replace(';', ',').replace('\n', ' ');
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null
                || hasta.isBefore(desde)
                || ChronoUnit.DAYS.between(desde, hasta) > MAXIMO_DIAS_RANGO) {
            throw new NegocioException(CodigoError.RANGO_FECHAS_INVALIDO);
        }
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolAdmin(Usuario admin) {
        if (admin == null || admin.getIdRol() == null || !ROL_ADMIN.equals(admin.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }
}
