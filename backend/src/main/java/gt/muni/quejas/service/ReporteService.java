package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoConteo;
import gt.muni.quejas.dto.DtoHistorialCaso;
import gt.muni.quejas.dto.DtoReporteEstadistico;
import gt.muni.quejas.dto.DtoTrazabilidadCaso;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.util.TipoCaso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// CU-14 Reportería: reportes estadísticos de los casos y trazabilidad de un caso
// concreto (lo que antes era el CU-13).
@Service
public class ReporteService {

    private static final String ROL_ADMIN = "Admin";
    private static final String ROL_SUPERVISOR = "Supervisor";
    private static final String ROL_JEFE = "Jefe";
    private static final int MAXIMO_DIAS_RANGO = 1830;

    private final CasoRepository casoRepository;
    private final BitacoraCambioRepository bitacoraCambioRepository;

    public ReporteService(CasoRepository casoRepository,
                          BitacoraCambioRepository bitacoraCambioRepository) {
        this.casoRepository = casoRepository;
        this.bitacoraCambioRepository = bitacoraCambioRepository;
    }

    // ---------- CU-14 pasos 3 y 4: reporte estadístico ----------
    @Transactional(readOnly = true)
    public DtoReporteEstadistico generarEstadisticas(LocalDate fechaInicio, LocalDate fechaFin,
                                                     Long idDepartamento, String tipoCaso,
                                                     String estado, Usuario usuario) {
        validarRolReportes(usuario);
        validarRango(fechaInicio, fechaFin);

        if (tipoCaso != null && !tipoCaso.isBlank() && !TipoCaso.esValido(tipoCaso)) {
            throw new NegocioException(CodigoError.TIPO_CASO_REQUERIDO);
        }

        // El Supervisor y la Jefatura solo ven su propio departamento
        Long departamentoConsulta = departamentoPermitido(idDepartamento, usuario);

        List<Caso> casos = casoRepository.buscarParaReporte(
                fechaInicio.atStartOfDay(),
                fechaFin.plusDays(1).atStartOfDay(),
                departamentoConsulta,
                vacio(tipoCaso) ? null : tipoCaso,
                vacio(estado) ? null : estado);

        List<DtoConteo> porEstado = new ArrayList<>();
        List<DtoConteo> porTipo = new ArrayList<>();
        List<DtoConteo> porDepartamento = new ArrayList<>();
        List<DtoConteo> porCategoria = new ArrayList<>();

        long resueltos = 0;
        long diasAcumulados = 0;

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            sumar(porEstado, caso.getEstado().getEstado());
            sumar(porTipo, caso.getTipoCaso());
            sumar(porDepartamento, caso.getDepartamento().getNombre());
            sumar(porCategoria, caso.getCategoria().getNombre());

            // El tiempo de atención se mide sobre los casos que llegaron a Resuelto
            if (EstadoCaso.RESUELTO.equals(caso.getEstado().getEstado())) {
                resueltos++;
                diasAcumulados = diasAcumulados + diasHastaResolucion(caso);
            }
        }

        double promedio = resueltos == 0 ? 0 : (double) diasAcumulados / resueltos;

        return new DtoReporteEstadistico(
                fechaInicio, fechaFin, casos.size(), resueltos,
                Math.round(promedio * 10.0) / 10.0,
                porEstado, porTipo, porDepartamento, porCategoria);
    }

    // ---------- CU-14: trazabilidad de un caso ----------
    @Transactional(readOnly = true)
    public DtoTrazabilidadCaso generarTrazabilidad(String codigoSeguimiento, Usuario usuario) {
        validarRolReportes(usuario);

        if (vacio(codigoSeguimiento)) {
            throw new NegocioException(CodigoError.CODIGO_SEGUIMIENTO_REQUERIDO);
        }

        Caso caso = casoRepository.findByCodigoSeguimiento(codigoSeguimiento.trim().toUpperCase())
                .orElseThrow(() -> new NegocioException(CodigoError.CASO_NO_ENCONTRADO_AUXILIAR));

        // El Supervisor y la Jefatura solo consultan casos de su departamento
        String rol = usuario.getIdRol().getRol();
        if ((ROL_SUPERVISOR.equals(rol) || ROL_JEFE.equals(rol))
                && !caso.getDepartamento().getId().equals(usuario.getIdDepartamento().getId())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }

        List<BitacoraCambio> registros = bitacoraCambioRepository.findByCasoIdOrderByIdAsc(caso.getId());
        List<DtoHistorialCaso> historial = new ArrayList<>();
        for (int i = 0; i < registros.size(); i++) {
            BitacoraCambio registro = registros.get(i);
            historial.add(new DtoHistorialCaso(
                    registro.getAccion(),
                    registro.getUsuario(),
                    registro.getFecha(),
                    registro.getHora(),
                    registro.getIpOrigen(),
                    registro.getDetalle() == null ? "" : registro.getDetalle()));
        }

        return new DtoTrazabilidadCaso(
                caso.getId(),
                caso.getCodigoSeguimiento(),
                caso.getCreateAt(),
                caso.getTipoCaso(),
                caso.getCategoria().getNombre(),
                caso.getDepartamento().getNombre(),
                caso.getEstado().getEstado(),
                caso.getCiudadano().getNombre(),
                caso.getTecnico() == null ? "" : caso.getTecnico().getNombre(),
                historial);
    }

    // ---------- Métodos de apoyo ----------

    // Suma uno al conteo de esa etiqueta, o la agrega si es la primera vez
    private void sumar(List<DtoConteo> conteos, String etiqueta) {
        for (int i = 0; i < conteos.size(); i++) {
            if (conteos.get(i).etiqueta().equals(etiqueta)) {
                conteos.set(i, new DtoConteo(etiqueta, conteos.get(i).cantidad() + 1));
                return;
            }
        }
        conteos.add(new DtoConteo(etiqueta, 1));
    }

    // Del registro del caso a la firma del oficio, que es cuando queda Resuelto
    private long diasHastaResolucion(Caso caso) {
        List<BitacoraCambio> registros = bitacoraCambioRepository.findByCasoIdOrderByIdAsc(caso.getId());
        for (int i = registros.size() - 1; i >= 0; i--) {
            BitacoraCambio registro = registros.get(i);
            if ("Firma de oficio de resolución".equals(registro.getAccion())) {
                LocalDateTime firma = LocalDateTime.of(registro.getFecha(), registro.getHora());
                long dias = ChronoUnit.DAYS.between(caso.getCreateAt(), firma);
                return dias < 0 ? 0 : dias;
            }
        }
        return 0;
    }

    // FA01: el rango de fechas debe ser válido
    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null
                || fin.isBefore(inicio)
                || ChronoUnit.DAYS.between(inicio, fin) > MAXIMO_DIAS_RANGO) {
            throw new NegocioException(CodigoError.RANGO_FECHAS_INVALIDO);
        }
    }

    private Long departamentoPermitido(Long idDepartamento, Usuario usuario) {
        String rol = usuario.getIdRol().getRol();
        if (ROL_SUPERVISOR.equals(rol) || ROL_JEFE.equals(rol)) {
            return usuario.getIdDepartamento().getId();
        }
        return idDepartamento;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolReportes(Usuario usuario) {
        if (usuario == null || usuario.getIdRol() == null) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
        String rol = usuario.getIdRol().getRol();
        if (!ROL_ADMIN.equals(rol) && !ROL_SUPERVISOR.equals(rol) && !ROL_JEFE.equals(rol)) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }
}
