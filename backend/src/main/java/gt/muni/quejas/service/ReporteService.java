package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoConteo;
import gt.muni.quejas.dto.DtoOpcionCatalogo;
import gt.muni.quejas.dto.DtoHistorialCaso;
import gt.muni.quejas.dto.DtoReporteEstadistico;
import gt.muni.quejas.dto.DtoTrazabilidadCaso;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Departamento;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.DepartamentoRepository;
import gt.muni.quejas.util.TipoCaso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final DepartamentoRepository departamentoRepository;

    public ReporteService(CasoRepository casoRepository,
                          BitacoraCambioRepository bitacoraCambioRepository,
                          DepartamentoRepository departamentoRepository) {
        this.casoRepository = casoRepository;
        this.bitacoraCambioRepository = bitacoraCambioRepository;
        this.departamentoRepository = departamentoRepository;
    }

    // ---------- RN32: lista del filtro Departamento ----------
    // El Supervisor y el Jefe de Departamento solo consultan su propio departamento,
    // así que la lista les muestra únicamente ese; el Administrador ve todo el
    // catálogo (RN06).
    @Transactional(readOnly = true)
    public List<DtoOpcionCatalogo> listarDepartamentos(Usuario usuario) {
        validarRolReportes(usuario);

        List<DtoOpcionCatalogo> opciones = new ArrayList<>();
        String rol = usuario.getIdRol().getRol();

        if (ROL_SUPERVISOR.equals(rol) || ROL_JEFE.equals(rol)) {
            Departamento propio = usuario.getIdDepartamento();
            if (propio != null) {
                opciones.add(new DtoOpcionCatalogo(propio.getId(), propio.getNombre()));
            }
            return opciones;
        }

        List<Departamento> departamentos = departamentoRepository.findAll();
        for (int i = 0; i < departamentos.size(); i++) {
            opciones.add(new DtoOpcionCatalogo(
                    departamentos.get(i).getId(), departamentos.get(i).getNombre()));
        }
        return opciones;
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

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            sumar(porEstado, caso.getEstado().getEstado());
            sumar(porTipo, caso.getTipoCaso());
            sumar(porDepartamento, caso.getDepartamento().getNombre());
            sumar(porCategoria, caso.getCategoria().getNombre());
        }

        return new DtoReporteEstadistico(
                fechaInicio, fechaFin, casos.size(),
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
