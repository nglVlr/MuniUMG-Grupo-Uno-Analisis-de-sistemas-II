package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCajaTecnico;
import gt.muni.quejas.dto.DtoCatalogosDashboard;
import gt.muni.quejas.dto.DtoCategoriaDashboard;
import gt.muni.quejas.dto.DtoCeldaCalor;
import gt.muni.quejas.dto.DtoConteo;
import gt.muni.quejas.dto.DtoDashboardRespuesta;
import gt.muni.quejas.dto.DtoEnlaceFlujo;
import gt.muni.quejas.dto.DtoFilaTecnico;
import gt.muni.quejas.dto.DtoFiltrosDashboard;
import gt.muni.quejas.dto.DtoHistorialCaso;
import gt.muni.quejas.dto.DtoIndicadorTarjeta;
import gt.muni.quejas.dto.DtoMapaCalor;
import gt.muni.quejas.dto.DtoNodoJerarquia;
import gt.muni.quejas.dto.DtoOpcionCatalogo;
import gt.muni.quejas.dto.DtoPuntoCalendario;
import gt.muni.quejas.dto.DtoPuntoDispersion;
import gt.muni.quejas.dto.DtoPuntoPareto;
import gt.muni.quejas.dto.DtoSerieDashboard;
import gt.muni.quejas.dto.DtoUsuarioDashboard;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.BitacoraCambio;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Categoria;
import gt.muni.quejas.model.Departamento;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.MotivoRechazo;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.BitacoraCambioRepository;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.CategoriaRepository;
import gt.muni.quejas.repository.CiudadanoRepository;
import gt.muni.quejas.repository.DepartamentoRepository;
import gt.muni.quejas.repository.MotivoRechazoRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import gt.muni.quejas.util.TipoCaso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// CU-19 Dashboard de gráficas: pantalla exclusiva del Admin. Los 10 filtros se aplican
// una sola vez sobre los casos y de ese mismo resultado salen todas las gráficas.
// La bitácora solo se lee: nunca se escribe ni se modifica desde aquí.
@Service
public class DashboardService {

    private static final String ROL_ADMIN = "Admin";
    private static final int MAXIMO_DIAS_RANGO = 1830;
    private static final Pattern DPI = Pattern.compile("^\\d{13}$");

    // ---------- Métricas ----------
    public static final String METRICA_CASOS = "casos";
    public static final String METRICA_RESUELTOS = "casosResueltos";
    public static final String METRICA_PORCENTAJE = "porcentajeResolucion";
    public static final String METRICA_TIEMPO = "tiempoPromedioResolucion";
    public static final String METRICA_ACCIONES = "accionesBitacora";
    public static final String METRICA_LOGIN_OK = "inicioSesionExitoso";
    public static final String METRICA_LOGIN_FALLIDO = "inicioSesionFallido";
    public static final String METRICA_ERROR_NOTIFICACION = "erroresNotificacion";

    // ---------- Agrupaciones ----------
    public static final String POR_ESTADO = "estado";
    public static final String POR_TIPO = "tipo";
    public static final String POR_DEPARTAMENTO = "departamento";
    public static final String POR_CATEGORIA = "categoria";
    public static final String POR_TECNICO = "tecnico";
    public static final String POR_CARGO = "cargo";
    public static final String POR_USUARIO = "usuario";
    public static final String POR_CIUDADANO = "ciudadano";
    public static final String POR_MOTIVO = "motivoRechazo";
    public static final String POR_DIA = "dia";
    public static final String POR_SEMANA = "semana";
    public static final String POR_MES = "mes";
    public static final String POR_DIA_SEMANA = "diaSemana";
    public static final String POR_HORA = "hora";
    public static final String POR_ACCION = "accion";
    // No es una agrupación del selector: solo se usa para desglosar las acciones
    // de cada cargo en lo que hizo con el caso
    private static final String POR_CLASIFICACION = "clasificacion";

    // Formas de datos que entiende la pantalla, para deshabilitar las gráficas que no aplican
    private static final String FORMA_CATEGORIA = "categoria-valor";
    private static final String FORMA_TEMPORAL = "temporal";

    // Formato del valor, para las etiquetas de la gráfica
    private static final String FORMATO_ENTERO = "entero";
    private static final String FORMATO_PORCENTAJE = "porcentaje";
    private static final String FORMATO_DIAS = "dias";

    // ---------- Acciones de la bitácora (tal como las escriben los demás servicios) ----------
    private static final String ACCION_REGISTRO = "Registro de caso";
    private static final String ACCION_FIRMA = "Firma de oficio de resolución";
    private static final String ACCION_APROBACION_RECHAZO = "Aprobación de rechazo";
    private static final String ACCION_LOGIN_OK = "Inicio de sesión";
    private static final String ACCION_LOGIN_FALLIDO = "Inicio de sesión fallido";
    private static final String ACCION_ERROR_NOTIFICACION = "Error envío de notificación";

    // Ancho de cada tramo de la campana de Gauss, en días
    private static final double ANCHO_TRAMO = 2.0;

    private static final String SIN_ASIGNAR = "Sin asignar";
    private static final String SIN_RECHAZO = "Sin rechazo";
    private static final String CARGO_CERRADO = "Caso cerrado";

    private static final String[] DIAS_SEMANA = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private static final List<String> ESTADOS_ORDEN = List.of(
            EstadoCaso.REGISTRADO, EstadoCaso.EN_REVISION, EstadoCaso.EN_ATENCION,
            EstadoCaso.EN_SUPERVISION, EstadoCaso.PENDIENTE_APROBACION_RECHAZO,
            EstadoCaso.PENDIENTE_FIRMA, EstadoCaso.RESUELTO, EstadoCaso.RECHAZADO,
            EstadoCaso.REQUIERE_AMPLIACION);

    private static final List<String> TIPOS_ORDEN = List.of(
            TipoCaso.QUEJA, TipoCaso.RECLAMO, TipoCaso.DENUNCIA, TipoCaso.SUGERENCIA);

    private static final List<String> CARGOS = List.of(
            "Ciudadano", "Auxiliar", "Tecnico", "Supervisor", "Jefe", ROL_ADMIN);

    // Etapas del embudo, en el orden en que avanza un caso
    private static final List<String> ETAPAS_EMBUDO = List.of(
            EstadoCaso.REGISTRADO, EstadoCaso.EN_REVISION, EstadoCaso.EN_ATENCION,
            EstadoCaso.EN_SUPERVISION, EstadoCaso.PENDIENTE_FIRMA, EstadoCaso.RESUELTO);

    // Estado al que pasa el caso según la acción que quedó en la bitácora.
    // Cada equivalencia se verificó leyendo el servicio que escribe esa acción.
    private static final Map<String, String> ESTADO_POR_ACCION = Map.ofEntries(
            Map.entry(ACCION_REGISTRO, EstadoCaso.REGISTRADO),
            Map.entry("Aceptación de caso", EstadoCaso.EN_REVISION),
            Map.entry("Asignación de Técnico", EstadoCaso.EN_ATENCION),
            Map.entry("Finalización de atención", EstadoCaso.EN_SUPERVISION),
            Map.entry("Aprobación de reporte de atención", EstadoCaso.PENDIENTE_FIRMA),
            Map.entry(ACCION_FIRMA, EstadoCaso.RESUELTO),
            Map.entry("Devolución al Técnico", EstadoCaso.EN_ATENCION),
            Map.entry("Devolución a Supervisión", EstadoCaso.EN_SUPERVISION),
            Map.entry("Rechazo de caso", EstadoCaso.PENDIENTE_APROBACION_RECHAZO),
            Map.entry("Rechazo con ampliación", EstadoCaso.REQUIERE_AMPLIACION),
            Map.entry(ACCION_APROBACION_RECHAZO, EstadoCaso.RECHAZADO),
            Map.entry("Ampliación de caso", EstadoCaso.EN_REVISION),
            // El Supervisor que no aprueba el rechazo regresa el caso a revisión
            Map.entry("Rechazo no aprobado", EstadoCaso.EN_REVISION));

    // Clasificación de las acciones por lo que hizo el cargo
    private static final List<String> ACCIONES_APROBO = List.of(
            "Aceptación de caso", "Aprobación de reporte de atención",
            ACCION_APROBACION_RECHAZO, ACCION_FIRMA);
    private static final List<String> ACCIONES_RECHAZO = List.of(
            "Rechazo de caso", "Rechazo con ampliación", "Rechazo no aprobado");
    private static final List<String> ACCIONES_DEVOLVIO = List.of(
            "Devolución al Técnico", "Devolución a Supervisión");

    private final CasoRepository casoRepository;
    private final BitacoraCambioRepository bitacoraCambioRepository;
    private final UsuarioRepository usuarioRepository;
    private final DepartamentoRepository departamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MotivoRechazoRepository motivoRechazoRepository;
    private final CiudadanoRepository ciudadanoRepository;

    public DashboardService(CasoRepository casoRepository,
                            BitacoraCambioRepository bitacoraCambioRepository,
                            UsuarioRepository usuarioRepository,
                            DepartamentoRepository departamentoRepository,
                            CategoriaRepository categoriaRepository,
                            MotivoRechazoRepository motivoRechazoRepository,
                            CiudadanoRepository ciudadanoRepository) {
        this.casoRepository = casoRepository;
        this.bitacoraCambioRepository = bitacoraCambioRepository;
        this.usuarioRepository = usuarioRepository;
        this.departamentoRepository = departamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.motivoRechazoRepository = motivoRechazoRepository;
        this.ciudadanoRepository = ciudadanoRepository;
    }

    // ---------- Listas del panel de filtros ----------
    @Transactional(readOnly = true)
    public DtoCatalogosDashboard catalogos(Usuario admin) {
        validarRolAdmin(admin);

        // El Admin ve todos los departamentos (RN06)
        List<DtoOpcionCatalogo> departamentos = new ArrayList<>();
        List<Departamento> todos = departamentoRepository.findAll();
        for (int i = 0; i < todos.size(); i++) {
            departamentos.add(new DtoOpcionCatalogo(todos.get(i).getId(), todos.get(i).getNombre()));
        }

        // La categoría depende del departamento y del tipo de caso
        List<DtoCategoriaDashboard> categorias = new ArrayList<>();
        List<Categoria> activas = categoriaRepository.findActivasConDepartamento();
        for (int i = 0; i < activas.size(); i++) {
            Categoria categoria = activas.get(i);
            categorias.add(new DtoCategoriaDashboard(
                    categoria.getId(),
                    categoria.getNombre(),
                    categoria.getDepartamento().getId(),
                    new ArrayList<>(categoria.getTiposCaso())));
        }

        List<DtoOpcionCatalogo> motivos = new ArrayList<>();
        List<MotivoRechazo> motivosActivos = motivoRechazoRepository.findAll();
        for (int i = 0; i < motivosActivos.size(); i++) {
            motivos.add(new DtoOpcionCatalogo(
                    motivosActivos.get(i).getId(), motivosActivos.get(i).getMotivo()));
        }

        // El Técnico del filtro depende del departamento
        List<DtoUsuarioDashboard> tecnicos = new ArrayList<>();
        List<DtoUsuarioDashboard> usuarios = new ArrayList<>();
        List<Usuario> empleados = usuarioRepository.findAll();
        for (int i = 0; i < empleados.size(); i++) {
            Usuario empleado = empleados.get(i);
            String rol = empleado.getIdRol() == null ? "" : empleado.getIdRol().getRol();
            Long idDepartamento = empleado.getIdDepartamento() == null
                    ? null : empleado.getIdDepartamento().getId();
            DtoUsuarioDashboard fila = new DtoUsuarioDashboard(
                    empleado.getCui(), empleado.getNombre(), rol, idDepartamento);
            usuarios.add(fila);
            if ("Tecnico".equals(rol)) {
                tecnicos.add(fila);
            }
        }

        return new DtoCatalogosDashboard(
                departamentos, categorias, TIPOS_ORDEN, ESTADOS_ORDEN,
                tecnicos, usuarios, motivos, CARGOS);
    }

    // ---------- Consulta genérica: métrica, agrupación y filtros ----------
    @Transactional(readOnly = true)
    public DtoDashboardRespuesta consultar(DtoFiltrosDashboard filtros, String metrica,
                                           String agrupacion, String codigoSeguimiento,
                                           Usuario admin) {
        validarRolAdmin(admin);
        validarRango(filtros.desde(), filtros.hasta());
        validarMetrica(metrica);
        validarAgrupacion(agrupacion);
        String dpi = validarCiudadano(filtros.dpiCiudadano());

        boolean porResolucion = DtoFiltrosDashboard.MODO_RESOLUCION.equals(filtros.modoFecha());

        // 1. Los filtros de caso se aplican una sola vez
        List<Caso> casos = casoRepository.buscarParaDashboard(
                filtros.idDepartamento(),
                vacio(filtros.tipoCaso()) ? null : filtros.tipoCaso(),
                filtros.idCategoria(),
                vacio(filtros.estado()) ? null : filtros.estado(),
                vacio(filtros.cuiTecnico()) ? null : filtros.cuiTecnico(),
                filtros.idMotivoRechazo(),
                dpi);

        List<BitacoraCambio> historial = cargarHistorial(casos);
        Map<Long, LocalDateTime> resoluciones = fechasResolucion(historial);
        Map<Long, LocalDateTime> cierres = fechasCierre(historial, resoluciones);

        casos = filtrarPorFecha(casos, filtros, resoluciones, porResolucion);
        casos = filtrarPorCargo(casos, filtros.cargo());
        historial = recortarHistorial(historial, casos);

        // 2. Actividad del sistema: no pertenece a un caso, respeta fecha,
        // departamento del usuario, cargo y usuario
        Map<String, Usuario> porCui = mapaUsuarios();
        List<BitacoraCambio> actividad = filtrarActividad(
                bitacoraCambioRepository.buscarPorRango(filtros.desde(), filtros.hasta()),
                filtros, porCui);

        boolean deSistema = esMetricaSistema(metrica);

        // 3. Serie principal
        List<Grupo> grupos = deSistema
                ? agruparActividad(actividad, agrupacion, metrica, porCui)
                : agruparCasos(casos, agrupacion, resoluciones, porResolucion, dpi);

        List<String> categorias = ordenarEtiquetas(grupos, agrupacion);
        List<Double> valores = new ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) {
            valores.add(valor(buscar(grupos, categorias.get(i)), metrica));
        }

        List<DtoSerieDashboard> series = new ArrayList<>();
        series.add(new DtoSerieDashboard(tituloMetrica(metrica), valores));

        // 4. Desglose, para las barras agrupadas, apiladas y las líneas múltiples
        String nombreDesglose = nombreDesglose(metrica, agrupacion);
        List<DtoSerieDashboard> desglosadas = desglosar(
                casos, actividad, categorias, agrupacion, nombreDesglose, metrica,
                resoluciones, porResolucion, dpi, porCui);

        boolean sinResultados = deSistema ? actividad.isEmpty() : casos.isEmpty();

        return new DtoDashboardRespuesta(
                metrica,
                agrupacion,
                esTemporal(agrupacion) ? FORMA_TEMPORAL : FORMA_CATEGORIA,
                formato(metrica),
                nombreDesglose,
                sinResultados,
                casos.size(),
                categorias,
                series,
                desglosadas,
                indicadores(casos, actividad, resoluciones, porResolucion, filtros),
                embudo(casos, historial),
                sankey(historial),
                treemap(casos),
                sunburst(casos),
                arbol(filtros.idDepartamento()),
                histograma(casos, resoluciones),
                distribucionDias(casos, resoluciones),
                mediaDias(casos, resoluciones),
                desviacionDias(casos, resoluciones),
                boxplot(casos, resoluciones),
                dispersion(casos, resoluciones),
                calorDiaHora(casos, resoluciones, porResolucion),
                calorCategoriaMes(casos, resoluciones, porResolucion),
                calendario(casos, resoluciones, porResolucion),
                pareto(casos),
                cascada(casos),
                acumuladoAbiertos(casos, cierres),
                mesesDe(casos, resoluciones, porResolucion),
                recibidosPorMes(casos, resoluciones, porResolucion),
                tiempoPorMes(casos, resoluciones, porResolucion),
                tablaTecnicos(casos, resoluciones),
                rankingCiudadanos(casos, dpi),
                rechazosCiudadanos(casos, dpi, false),
                rechazosCiudadanos(casos, dpi, true),
                codigoTrazabilidad(casos, codigoSeguimiento),
                trazabilidad(casos, historial, codigoSeguimiento));
    }

    // ================= Carga y filtrado =================

    private List<BitacoraCambio> cargarHistorial(List<Caso> casos) {
        if (casos.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            ids.add(casos.get(i).getId());
        }
        return bitacoraCambioRepository.buscarPorCasos(ids);
    }

    // La fecha y la hora están en columnas separadas, hay que combinarlas
    private LocalDateTime momento(BitacoraCambio registro) {
        return LocalDateTime.of(registro.getFecha(), registro.getHora());
    }

    // El caso se resuelve cuando la Jefatura firma el oficio de resolución
    private Map<Long, LocalDateTime> fechasResolucion(List<BitacoraCambio> historial) {
        Map<Long, LocalDateTime> resoluciones = new HashMap<>();
        for (int i = 0; i < historial.size(); i++) {
            BitacoraCambio registro = historial.get(i);
            if (registro.getCaso() != null && ACCION_FIRMA.equals(registro.getAccion())) {
                resoluciones.put(registro.getCaso().getId(), momento(registro));
            }
        }
        return resoluciones;
    }

    // Un caso se cierra al resolverse o al aprobarse su rechazo
    private Map<Long, LocalDateTime> fechasCierre(List<BitacoraCambio> historial,
                                                  Map<Long, LocalDateTime> resoluciones) {
        Map<Long, LocalDateTime> cierres = new HashMap<>(resoluciones);
        for (int i = 0; i < historial.size(); i++) {
            BitacoraCambio registro = historial.get(i);
            if (registro.getCaso() == null) {
                continue;
            }
            if (ACCION_APROBACION_RECHAZO.equals(registro.getAccion())
                    && EstadoCaso.RECHAZADO.equals(estadoDeAccion(registro))) {
                cierres.put(registro.getCaso().getId(), momento(registro));
            }
        }
        return cierres;
    }

    private List<Caso> filtrarPorFecha(List<Caso> casos, DtoFiltrosDashboard filtros,
                                       Map<Long, LocalDateTime> resoluciones, boolean porResolucion) {
        LocalDateTime inicio = filtros.desde().atStartOfDay();
        LocalDateTime fin = filtros.hasta().plusDays(1).atStartOfDay();

        List<Caso> resultado = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            // Por fecha de resolución solo entran los casos ya resueltos
            if (base == null) {
                continue;
            }
            if (!base.isBefore(inicio) && base.isBefore(fin)) {
                resultado.add(caso);
            }
        }
        return resultado;
    }

    // El cargo en las gráficas de casos son los casos que hoy están en manos de ese cargo
    private List<Caso> filtrarPorCargo(List<Caso> casos, String cargo) {
        if (vacio(cargo)) {
            return casos;
        }
        List<Caso> resultado = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            if (cargo.trim().equals(cargoPorEstado(caso.getEstado().getEstado()))) {
                resultado.add(caso);
            }
        }
        return resultado;
    }

    private String cargoPorEstado(String estado) {
        if (EstadoCaso.REGISTRADO.equals(estado) || EstadoCaso.EN_REVISION.equals(estado)) {
            return "Auxiliar";
        }
        if (EstadoCaso.EN_ATENCION.equals(estado)) {
            return "Tecnico";
        }
        if (EstadoCaso.EN_SUPERVISION.equals(estado)
                || EstadoCaso.PENDIENTE_APROBACION_RECHAZO.equals(estado)) {
            return "Supervisor";
        }
        if (EstadoCaso.PENDIENTE_FIRMA.equals(estado)) {
            return "Jefe";
        }
        if (EstadoCaso.REQUIERE_AMPLIACION.equals(estado)) {
            return "Ciudadano";
        }
        // Resuelto y Rechazado ya no están en manos de nadie
        return CARGO_CERRADO;
    }

    private List<BitacoraCambio> recortarHistorial(List<BitacoraCambio> historial, List<Caso> casos) {
        Map<Long, Boolean> permitidos = new HashMap<>();
        for (int i = 0; i < casos.size(); i++) {
            permitidos.put(casos.get(i).getId(), Boolean.TRUE);
        }
        List<BitacoraCambio> resultado = new ArrayList<>();
        for (int i = 0; i < historial.size(); i++) {
            BitacoraCambio registro = historial.get(i);
            if (registro.getCaso() != null && permitidos.containsKey(registro.getCaso().getId())) {
                resultado.add(registro);
            }
        }
        return resultado;
    }

    // La bitácora guarda el CUI del empleado en las acciones de caso y el correo con
    // el que se entró en los inicios de sesión, así que el mapa acepta las dos llaves
    private Map<String, Usuario> mapaUsuarios() {
        Map<String, Usuario> porCui = new HashMap<>();
        List<Usuario> empleados = usuarioRepository.findAll();
        for (int i = 0; i < empleados.size(); i++) {
            Usuario empleado = empleados.get(i);
            porCui.put(empleado.getCui(), empleado);
            porCui.put(empleado.getCorreo(), empleado);
        }
        return porCui;
    }

    private List<BitacoraCambio> filtrarActividad(List<BitacoraCambio> registros,
                                                  DtoFiltrosDashboard filtros,
                                                  Map<String, Usuario> porCui) {
        List<BitacoraCambio> resultado = new ArrayList<>();
        for (int i = 0; i < registros.size(); i++) {
            BitacoraCambio registro = registros.get(i);

            // El cargo de las gráficas de bitácora es el rol de quien hizo la acción
            if (!vacio(filtros.cargo()) && !filtros.cargo().trim().equals(registro.getRol())) {
                continue;
            }
            if (!vacio(filtros.cuiUsuario()) && !esDelUsuario(registro, filtros.cuiUsuario().trim(), porCui)) {
                continue;
            }
            if (filtros.idDepartamento() != null) {
                Usuario autor = porCui.get(registro.getUsuario());
                if (autor == null || autor.getIdDepartamento() == null
                        || !filtros.idDepartamento().equals(autor.getIdDepartamento().getId())) {
                    continue;
                }
            }
            resultado.add(registro);
        }
        return resultado;
    }

    // El registro puede traer el CUI del empleado o su correo
    private boolean esDelUsuario(BitacoraCambio registro, String cui, Map<String, Usuario> porCui) {
        if (cui.equals(registro.getUsuario())) {
            return true;
        }
        Usuario elegido = porCui.get(cui);
        return elegido != null && elegido.getCorreo().equals(registro.getUsuario());
    }

    // ================= Serie principal =================

    // Acumulado de una categoría de la gráfica
    private static class Grupo {
        private final String etiqueta;
        private long total;
        private long resueltos;
        private double sumaDias;
        private long conteoDias;

        private Grupo(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    private Grupo buscar(List<Grupo> grupos, String etiqueta) {
        for (int i = 0; i < grupos.size(); i++) {
            if (grupos.get(i).etiqueta.equals(etiqueta)) {
                return grupos.get(i);
            }
        }
        return null;
    }

    private Grupo obtener(List<Grupo> grupos, String etiqueta) {
        Grupo grupo = buscar(grupos, etiqueta);
        if (grupo == null) {
            grupo = new Grupo(etiqueta);
            grupos.add(grupo);
        }
        return grupo;
    }

    private List<Grupo> agruparCasos(List<Caso> casos, String agrupacion,
                                     Map<Long, LocalDateTime> resoluciones,
                                     boolean porResolucion, String dpiFiltrado) {
        List<Grupo> grupos = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            String etiqueta = etiquetaCaso(caso, agrupacion, resoluciones, porResolucion, dpiFiltrado);
            if (etiqueta == null) {
                continue;
            }
            Grupo grupo = obtener(grupos, etiqueta);
            grupo.total = grupo.total + 1;

            if (EstadoCaso.RESUELTO.equals(caso.getEstado().getEstado())) {
                grupo.resueltos = grupo.resueltos + 1;
                LocalDateTime resolucion = resoluciones.get(caso.getId());
                if (resolucion != null) {
                    grupo.sumaDias = grupo.sumaDias + dias(caso.getCreateAt(), resolucion);
                    grupo.conteoDias = grupo.conteoDias + 1;
                }
            }
        }
        return grupos;
    }

    private List<Grupo> agruparActividad(List<BitacoraCambio> actividad, String agrupacion,
                                         String metrica, Map<String, Usuario> porCui) {
        List<Grupo> grupos = new ArrayList<>();
        for (int i = 0; i < actividad.size(); i++) {
            BitacoraCambio registro = actividad.get(i);
            if (!cuentaParaMetrica(registro, metrica)) {
                continue;
            }
            String etiqueta = etiquetaActividad(registro, agrupacion, porCui);
            if (etiqueta == null) {
                continue;
            }
            Grupo grupo = obtener(grupos, etiqueta);
            grupo.total = grupo.total + 1;
        }
        return grupos;
    }

    private boolean cuentaParaMetrica(BitacoraCambio registro, String metrica) {
        if (METRICA_LOGIN_OK.equals(metrica)) {
            return ACCION_LOGIN_OK.equals(registro.getAccion());
        }
        if (METRICA_LOGIN_FALLIDO.equals(metrica)) {
            return ACCION_LOGIN_FALLIDO.equals(registro.getAccion());
        }
        if (METRICA_ERROR_NOTIFICACION.equals(metrica)) {
            return ACCION_ERROR_NOTIFICACION.equals(registro.getAccion());
        }
        return true;
    }

    private String etiquetaCaso(Caso caso, String agrupacion, Map<Long, LocalDateTime> resoluciones,
                                boolean porResolucion, String dpiFiltrado) {
        if (POR_ESTADO.equals(agrupacion)) {
            return caso.getEstado().getEstado();
        }
        if (POR_TIPO.equals(agrupacion)) {
            return caso.getTipoCaso();
        }
        if (POR_DEPARTAMENTO.equals(agrupacion)) {
            return caso.getDepartamento().getNombre();
        }
        if (POR_CATEGORIA.equals(agrupacion)) {
            return caso.getCategoria().getNombre();
        }
        if (POR_TECNICO.equals(agrupacion)) {
            return caso.getTecnico() == null ? SIN_ASIGNAR : caso.getTecnico().getNombre();
        }
        if (POR_CARGO.equals(agrupacion)) {
            return cargoPorEstado(caso.getEstado().getEstado());
        }
        if (POR_CIUDADANO.equals(agrupacion)) {
            return nombreCiudadano(caso, dpiFiltrado);
        }
        if (POR_MOTIVO.equals(agrupacion)) {
            return caso.getMotivoRechazo() == null ? SIN_RECHAZO : caso.getMotivoRechazo().getMotivo();
        }
        if (esTemporal(agrupacion)) {
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            return base == null ? null : etiquetaTemporal(base, agrupacion);
        }
        // usuario y accion solo aplican a las gráficas de bitácora
        return null;
    }

    private String etiquetaActividad(BitacoraCambio registro, String agrupacion,
                                     Map<String, Usuario> porCui) {
        if (POR_ACCION.equals(agrupacion)) {
            return registro.getAccion();
        }
        if (POR_CLASIFICACION.equals(agrupacion)) {
            return clasificacionAccion(registro.getAccion());
        }
        if (POR_CARGO.equals(agrupacion)) {
            return vacio(registro.getRol()) ? "Ciudadano" : registro.getRol();
        }
        if (POR_USUARIO.equals(agrupacion)) {
            Usuario autor = porCui.get(registro.getUsuario());
            return autor == null ? registro.getUsuario() : autor.getNombre();
        }
        if (POR_DEPARTAMENTO.equals(agrupacion)) {
            Usuario autor = porCui.get(registro.getUsuario());
            if (autor == null || autor.getIdDepartamento() == null) {
                return "Sin departamento";
            }
            return autor.getIdDepartamento().getNombre();
        }
        if (POR_ESTADO.equals(agrupacion)) {
            // El estado al que pasa el caso se deduce de la acción
            return estadoDeAccion(registro);
        }
        if (esTemporal(agrupacion)) {
            return etiquetaTemporal(momento(registro), agrupacion);
        }
        return null;
    }

    // Qué hizo el cargo con el caso: aprobó, rechazó o lo devolvió
    private String clasificacionAccion(String accion) {
        if (ACCIONES_APROBO.contains(accion)) {
            return "Aprobó";
        }
        if (ACCIONES_RECHAZO.contains(accion)) {
            return "Rechazó";
        }
        if (ACCIONES_DEVOLVIO.contains(accion)) {
            return "Devolvió";
        }
        return "Otras acciones";
    }

    // Tabla de equivalencias entre la acción de la bitácora y el estado del caso
    private String estadoDeAccion(BitacoraCambio registro) {
        String estado = ESTADO_POR_ACCION.get(registro.getAccion());
        if (estado == null) {
            return null;
        }
        // La aprobación del rechazo deja el caso en Rechazado o en Requiere Ampliación,
        // y el estado real quedó guardado en el valor nuevo del registro
        if (ACCION_APROBACION_RECHAZO.equals(registro.getAccion())
                && !vacio(registro.getValorNuevo())) {
            return registro.getValorNuevo();
        }
        return estado;
    }

    private String etiquetaTemporal(LocalDateTime momento, String agrupacion) {
        LocalDate fecha = momento.toLocalDate();
        if (POR_DIA.equals(agrupacion)) {
            return fecha.toString();
        }
        if (POR_SEMANA.equals(agrupacion)) {
            int anio = fecha.get(WeekFields.ISO.weekBasedYear());
            int semana = fecha.get(WeekFields.ISO.weekOfWeekBasedYear());
            return anio + "-S" + (semana < 10 ? "0" + semana : String.valueOf(semana));
        }
        if (POR_MES.equals(agrupacion)) {
            int mes = fecha.getMonthValue();
            return fecha.getYear() + "-" + (mes < 10 ? "0" + mes : String.valueOf(mes));
        }
        if (POR_DIA_SEMANA.equals(agrupacion)) {
            return DIAS_SEMANA[fecha.getDayOfWeek().getValue() - 1];
        }
        if (POR_HORA.equals(agrupacion)) {
            int hora = momento.getHour();
            return (hora < 10 ? "0" + hora : String.valueOf(hora)) + ":00";
        }
        return null;
    }

    private double valor(Grupo grupo, String metrica) {
        if (grupo == null) {
            return 0;
        }
        if (METRICA_RESUELTOS.equals(metrica)) {
            return grupo.resueltos;
        }
        if (METRICA_PORCENTAJE.equals(metrica)) {
            return grupo.total == 0 ? 0 : redondear(grupo.resueltos * 100.0 / grupo.total);
        }
        if (METRICA_TIEMPO.equals(metrica)) {
            return grupo.conteoDias == 0 ? 0 : redondear(grupo.sumaDias / grupo.conteoDias);
        }
        // Conteo de casos y todas las métricas de bitácora
        return grupo.total;
    }

    // Las agrupaciones de tiempo van en orden cronológico y las demás de mayor a menor
    private List<String> ordenarEtiquetas(List<Grupo> grupos, String agrupacion) {
        List<String> etiquetas = new ArrayList<>();
        for (int i = 0; i < grupos.size(); i++) {
            etiquetas.add(grupos.get(i).etiqueta);
        }

        if (POR_ESTADO.equals(agrupacion)) {
            return ordenarPorCatalogo(etiquetas, ESTADOS_ORDEN);
        }
        if (POR_TIPO.equals(agrupacion)) {
            return ordenarPorCatalogo(etiquetas, TIPOS_ORDEN);
        }
        if (POR_CARGO.equals(agrupacion)) {
            List<String> orden = new ArrayList<>(CARGOS);
            orden.add(CARGO_CERRADO);
            return ordenarPorCatalogo(etiquetas, orden);
        }
        if (POR_DIA_SEMANA.equals(agrupacion)) {
            return ordenarPorCatalogo(etiquetas, List.of(DIAS_SEMANA));
        }
        if (POR_DIA.equals(agrupacion) || POR_SEMANA.equals(agrupacion)
                || POR_MES.equals(agrupacion) || POR_HORA.equals(agrupacion)) {
            Collections.sort(etiquetas);
            return etiquetas;
        }

        // Ranking: primero las categorías con más casos
        List<Grupo> copia = new ArrayList<>(grupos);
        copia.sort((uno, otro) -> Long.compare(otro.total, uno.total));
        List<String> ordenadas = new ArrayList<>();
        for (int i = 0; i < copia.size(); i++) {
            ordenadas.add(copia.get(i).etiqueta);
        }
        return ordenadas;
    }

    private List<String> ordenarPorCatalogo(List<String> etiquetas, List<String> catalogo) {
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < catalogo.size(); i++) {
            if (etiquetas.contains(catalogo.get(i))) {
                resultado.add(catalogo.get(i));
            }
        }
        // Lo que no esté en el catálogo se agrega al final, sin perderse
        for (int i = 0; i < etiquetas.size(); i++) {
            if (!resultado.contains(etiquetas.get(i))) {
                resultado.add(etiquetas.get(i));
            }
        }
        return resultado;
    }

    // ================= Desglose para barras apiladas y líneas múltiples =================

    private String nombreDesglose(String metrica, String agrupacion) {
        if (esMetricaSistema(metrica)) {
            // Las acciones de cada cargo se parten en aprobó, rechazó y devolvió
            return POR_CARGO.equals(agrupacion) ? "Lo que hizo el cargo" : "Cargo";
        }
        return POR_ESTADO.equals(agrupacion) ? "Tipo de caso" : "Estado";
    }

    private List<DtoSerieDashboard> desglosar(List<Caso> casos, List<BitacoraCambio> actividad,
                                              List<String> categorias, String agrupacion,
                                              String nombreDesglose, String metrica,
                                              Map<Long, LocalDateTime> resoluciones,
                                              boolean porResolucion, String dpiFiltrado,
                                              Map<String, Usuario> porCui) {
        String segunda = segundaAgrupacion(nombreDesglose);
        List<String> nombres = new ArrayList<>();
        // Una lista de grupos por cada categoría del eje, en el mismo orden
        List<List<Grupo>> porCategoria = new ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) {
            porCategoria.add(new ArrayList<>());
        }

        if (esMetricaSistema(metrica)) {
            for (int i = 0; i < actividad.size(); i++) {
                BitacoraCambio registro = actividad.get(i);
                if (!cuentaParaMetrica(registro, metrica)) {
                    continue;
                }
                String etiqueta = etiquetaActividad(registro, agrupacion, porCui);
                String desglose = etiquetaActividad(registro, segunda, porCui);
                int posicion = categorias.indexOf(etiqueta);
                if (posicion < 0 || desglose == null) {
                    continue;
                }
                if (!nombres.contains(desglose)) {
                    nombres.add(desglose);
                }
                obtener(porCategoria.get(posicion), desglose).total += 1;
            }
        } else {
            for (int i = 0; i < casos.size(); i++) {
                Caso caso = casos.get(i);
                String etiqueta = etiquetaCaso(caso, agrupacion, resoluciones, porResolucion, dpiFiltrado);
                String desglose = etiquetaCaso(caso, segunda, resoluciones, porResolucion, dpiFiltrado);
                int posicion = categorias.indexOf(etiqueta);
                if (posicion < 0 || desglose == null) {
                    continue;
                }
                if (!nombres.contains(desglose)) {
                    nombres.add(desglose);
                }
                Grupo grupo = obtener(porCategoria.get(posicion), desglose);
                grupo.total += 1;
                if (EstadoCaso.RESUELTO.equals(caso.getEstado().getEstado())) {
                    grupo.resueltos += 1;
                    LocalDateTime resolucion = resoluciones.get(caso.getId());
                    if (resolucion != null) {
                        grupo.sumaDias += dias(caso.getCreateAt(), resolucion);
                        grupo.conteoDias += 1;
                    }
                }
            }
        }

        List<String> ordenados = POR_ESTADO.equals(segunda)
                ? ordenarPorCatalogo(nombres, ESTADOS_ORDEN)
                : nombres;

        List<DtoSerieDashboard> series = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            List<Double> valores = new ArrayList<>();
            for (int j = 0; j < categorias.size(); j++) {
                valores.add(valor(buscar(porCategoria.get(j), ordenados.get(i)), metrica));
            }
            series.add(new DtoSerieDashboard(ordenados.get(i), valores));
        }
        return series;
    }

    private String segundaAgrupacion(String nombreDesglose) {
        if ("Cargo".equals(nombreDesglose)) {
            return POR_CARGO;
        }
        if ("Lo que hizo el cargo".equals(nombreDesglose)) {
            return POR_CLASIFICACION;
        }
        if ("Tipo de caso".equals(nombreDesglose)) {
            return POR_TIPO;
        }
        return POR_ESTADO;
    }

    // ================= Tarjetas de indicadores =================

    private List<DtoIndicadorTarjeta> indicadores(List<Caso> casos, List<BitacoraCambio> actividad,
                                                  Map<Long, LocalDateTime> resoluciones,
                                                  boolean porResolucion, DtoFiltrosDashboard filtros) {
        long resueltos = 0;
        double sumaDias = 0;
        long conteoDias = 0;
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            if (EstadoCaso.RESUELTO.equals(caso.getEstado().getEstado())) {
                resueltos = resueltos + 1;
                LocalDateTime resolucion = resoluciones.get(caso.getId());
                if (resolucion != null) {
                    sumaDias = sumaDias + dias(caso.getCreateAt(), resolucion);
                    conteoDias = conteoDias + 1;
                }
            }
        }

        long loginOk = contarAccion(actividad, ACCION_LOGIN_OK);
        long loginFallido = contarAccion(actividad, ACCION_LOGIN_FALLIDO);
        long errores = contarAccion(actividad, ACCION_ERROR_NOTIFICACION);

        List<String> meses = ultimosMeses(filtros.hasta());
        boolean mesIncompleto = filtros.hasta().getDayOfMonth() < filtros.hasta().lengthOfMonth();
        List<Double> chispaCasos = chispaCasos(casos, meses, resoluciones, porResolucion, false);
        List<Double> chispaResueltos = chispaCasos(casos, meses, resoluciones, porResolucion, true);
        List<Double> chispaAcciones = chispaActividad(actividad, meses, null);
        List<Double> chispaLoginOk = chispaActividad(actividad, meses, ACCION_LOGIN_OK);
        List<Double> chispaLoginFallido = chispaActividad(actividad, meses, ACCION_LOGIN_FALLIDO);
        List<Double> chispaErrores = chispaActividad(actividad, meses, ACCION_ERROR_NOTIFICACION);

        List<DtoIndicadorTarjeta> tarjetas = new ArrayList<>();
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_CASOS, "Casos recibidos",
                casos.size(), FORMATO_ENTERO, variacion(chispaCasos, mesIncompleto), chispaCasos));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_RESUELTOS, "Casos resueltos",
                resueltos, FORMATO_ENTERO, variacion(chispaResueltos, mesIncompleto), chispaResueltos));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_PORCENTAJE, "Porcentaje de resolución",
                casos.isEmpty() ? 0 : redondear(resueltos * 100.0 / casos.size()),
                FORMATO_PORCENTAJE, 0, chispaResueltos));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_TIEMPO, "Tiempo promedio de resolución",
                conteoDias == 0 ? 0 : redondear(sumaDias / conteoDias),
                FORMATO_DIAS, 0, new ArrayList<>()));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_ACCIONES, "Acciones de bitácora",
                actividad.size(), FORMATO_ENTERO, variacion(chispaAcciones, mesIncompleto), chispaAcciones));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_LOGIN_OK, "Inicios de sesión exitosos",
                loginOk, FORMATO_ENTERO, variacion(chispaLoginOk, mesIncompleto), chispaLoginOk));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_LOGIN_FALLIDO, "Inicios de sesión fallidos",
                loginFallido, FORMATO_ENTERO, variacion(chispaLoginFallido, mesIncompleto), chispaLoginFallido));
        tarjetas.add(new DtoIndicadorTarjeta(METRICA_ERROR_NOTIFICACION, "Errores de notificación",
                errores, FORMATO_ENTERO, variacion(chispaErrores, mesIncompleto), chispaErrores));
        return tarjetas;
    }

    private long contarAccion(List<BitacoraCambio> actividad, String accion) {
        long total = 0;
        for (int i = 0; i < actividad.size(); i++) {
            if (accion.equals(actividad.get(i).getAccion())) {
                total = total + 1;
            }
        }
        return total;
    }

    private List<String> ultimosMeses(LocalDate hasta) {
        List<String> meses = new ArrayList<>();
        LocalDate mes = hasta.withDayOfMonth(1).minusMonths(11);
        for (int i = 0; i < 12; i++) {
            meses.add(etiquetaTemporal(mes.atStartOfDay(), POR_MES));
            mes = mes.plusMonths(1);
        }
        return meses;
    }

    private List<Double> chispaCasos(List<Caso> casos, List<String> meses,
                                     Map<Long, LocalDateTime> resoluciones,
                                     boolean porResolucion, boolean soloResueltos) {
        List<Double> valores = nuevaLista(meses.size());
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            if (soloResueltos && !EstadoCaso.RESUELTO.equals(caso.getEstado().getEstado())) {
                continue;
            }
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            if (base == null) {
                continue;
            }
            int posicion = meses.indexOf(etiquetaTemporal(base, POR_MES));
            if (posicion >= 0) {
                valores.set(posicion, valores.get(posicion) + 1);
            }
        }
        return valores;
    }

    private List<Double> chispaActividad(List<BitacoraCambio> actividad, List<String> meses, String accion) {
        List<Double> valores = nuevaLista(meses.size());
        for (int i = 0; i < actividad.size(); i++) {
            BitacoraCambio registro = actividad.get(i);
            if (accion != null && !accion.equals(registro.getAccion())) {
                continue;
            }
            int posicion = meses.indexOf(etiquetaTemporal(momento(registro), POR_MES));
            if (posicion >= 0) {
                valores.set(posicion, valores.get(posicion) + 1);
            }
        }
        return valores;
    }

    // Cambio de un mes contra el anterior, para la tarjeta con variación.
    // Si el rango termina a mitad de mes ese mes va incompleto y compararlo contra
    // uno completo daría una caída que no existe, así que se corre un mes atrás.
    private double variacion(List<Double> chispa, boolean mesIncompleto) {
        int ultimo = chispa.size() - 1;
        if (mesIncompleto) {
            ultimo = ultimo - 1;
        }
        if (ultimo < 1) {
            return 0;
        }
        double anterior = chispa.get(ultimo - 1);
        double actual = chispa.get(ultimo);
        if (anterior == 0) {
            return actual == 0 ? 0 : 100;
        }
        return redondear((actual - anterior) * 100.0 / anterior);
    }

    // ================= Gráficas con forma propia =================

    // Embudo: cuántos casos llegaron a cada etapa, según su historial
    private List<DtoConteo> embudo(List<Caso> casos, List<BitacoraCambio> historial) {
        List<DtoConteo> resultado = new ArrayList<>();
        for (int i = 0; i < ETAPAS_EMBUDO.size(); i++) {
            String etapa = ETAPAS_EMBUDO.get(i);
            Map<Long, Boolean> alcanzaron = new HashMap<>();
            for (int j = 0; j < historial.size(); j++) {
                BitacoraCambio registro = historial.get(j);
                if (registro.getCaso() != null && etapa.equals(estadoDeAccion(registro))) {
                    alcanzaron.put(registro.getCaso().getId(), Boolean.TRUE);
                }
            }
            resultado.add(new DtoConteo(etapa, alcanzaron.size()));
        }
        return resultado;
    }

    // Sankey: transiciones entre estados, incluyendo devoluciones y rechazos.
    // Una devolución o una ampliación regresa el caso a un estado anterior, y eso
    // sería un ciclo. Para que el diagrama siga avanzando de izquierda a derecha,
    // cada regreso abre una vuelta nueva y el estado se nombra con esa vuelta.
    private List<DtoEnlaceFlujo> sankey(List<BitacoraCambio> historial) {
        Map<Long, String> estadoPrevio = new HashMap<>();
        Map<Long, Integer> vueltaDelCaso = new HashMap<>();
        List<String> origenes = new ArrayList<>();
        List<String> destinos = new ArrayList<>();
        List<Long> conteos = new ArrayList<>();

        for (int i = 0; i < historial.size(); i++) {
            BitacoraCambio registro = historial.get(i);
            if (registro.getCaso() == null) {
                continue;
            }
            String nuevo = estadoDeAccion(registro);
            if (nuevo == null) {
                continue;
            }
            Long idCaso = registro.getCaso().getId();
            String previo = estadoPrevio.get(idCaso);
            int vuelta = vueltaDelCaso.get(idCaso) == null ? 1 : vueltaDelCaso.get(idCaso);

            if (previo == null) {
                estadoPrevio.put(idCaso, nuevo);
                vueltaDelCaso.put(idCaso, vuelta);
                continue;
            }
            if (previo.equals(nuevo)) {
                continue;
            }

            String nombrePrevio = conVuelta(previo, vuelta);
            // El caso regresó a un estado anterior: empieza otra vuelta
            if (ESTADOS_ORDEN.indexOf(nuevo) <= ESTADOS_ORDEN.indexOf(previo)) {
                vuelta = vuelta + 1;
            }
            String nombreNuevo = conVuelta(nuevo, vuelta);

            int posicion = posicionEnlace(origenes, destinos, nombrePrevio, nombreNuevo);
            if (posicion < 0) {
                origenes.add(nombrePrevio);
                destinos.add(nombreNuevo);
                conteos.add(1L);
            } else {
                conteos.set(posicion, conteos.get(posicion) + 1);
            }

            estadoPrevio.put(idCaso, nuevo);
            vueltaDelCaso.put(idCaso, vuelta);
        }

        List<DtoEnlaceFlujo> enlaces = new ArrayList<>();
        for (int i = 0; i < origenes.size(); i++) {
            enlaces.add(new DtoEnlaceFlujo(origenes.get(i), destinos.get(i), conteos.get(i)));
        }
        return enlaces;
    }

    private String conVuelta(String estado, int vuelta) {
        return vuelta <= 1 ? estado : estado + " (vuelta " + vuelta + ")";
    }

    private int posicionEnlace(List<String> origenes, List<String> destinos,
                               String origen, String destino) {
        for (int i = 0; i < origenes.size(); i++) {
            if (origenes.get(i).equals(origen) && destinos.get(i).equals(destino)) {
                return i;
            }
        }
        return -1;
    }

    // Treemap: departamento y categoría
    private List<DtoNodoJerarquia> treemap(List<Caso> casos) {
        List<String> departamentos = new ArrayList<>();
        List<List<Grupo>> categorias = new ArrayList<>();

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            String departamento = caso.getDepartamento().getNombre();
            int posicion = departamentos.indexOf(departamento);
            if (posicion < 0) {
                departamentos.add(departamento);
                categorias.add(new ArrayList<>());
                posicion = departamentos.size() - 1;
            }
            obtener(categorias.get(posicion), caso.getCategoria().getNombre()).total += 1;
        }

        List<DtoNodoJerarquia> nodos = new ArrayList<>();
        for (int i = 0; i < departamentos.size(); i++) {
            List<DtoNodoJerarquia> hijos = new ArrayList<>();
            List<Grupo> grupos = categorias.get(i);
            long total = 0;
            for (int j = 0; j < grupos.size(); j++) {
                hijos.add(new DtoNodoJerarquia(grupos.get(j).etiqueta, grupos.get(j).total, new ArrayList<>()));
                total = total + grupos.get(j).total;
            }
            nodos.add(new DtoNodoJerarquia(departamentos.get(i), total, hijos));
        }
        return nodos;
    }

    // Sunburst: departamento, tipo y estado
    private List<DtoNodoJerarquia> sunburst(List<Caso> casos) {
        List<DtoNodoJerarquia> raices = new ArrayList<>();
        List<Departamento> departamentos = departamentoRepository.findAll();

        for (int i = 0; i < departamentos.size(); i++) {
            String departamento = departamentos.get(i).getNombre();
            List<DtoNodoJerarquia> nodosTipo = new ArrayList<>();
            long totalDepartamento = 0;

            for (int t = 0; t < TIPOS_ORDEN.size(); t++) {
                String tipo = TIPOS_ORDEN.get(t);
                List<Grupo> estados = new ArrayList<>();
                for (int c = 0; c < casos.size(); c++) {
                    Caso caso = casos.get(c);
                    if (departamento.equals(caso.getDepartamento().getNombre())
                            && tipo.equals(caso.getTipoCaso())) {
                        obtener(estados, caso.getEstado().getEstado()).total += 1;
                    }
                }
                if (estados.isEmpty()) {
                    continue;
                }
                List<DtoNodoJerarquia> nodosEstado = new ArrayList<>();
                long totalTipo = 0;
                for (int e = 0; e < estados.size(); e++) {
                    nodosEstado.add(new DtoNodoJerarquia(
                            estados.get(e).etiqueta, estados.get(e).total, new ArrayList<>()));
                    totalTipo = totalTipo + estados.get(e).total;
                }
                nodosTipo.add(new DtoNodoJerarquia(tipo, totalTipo, nodosEstado));
                totalDepartamento = totalDepartamento + totalTipo;
            }

            if (totalDepartamento > 0) {
                raices.add(new DtoNodoJerarquia(departamento, totalDepartamento, nodosTipo));
            }
        }
        return raices;
    }

    // Árbol: departamento, roles y usuarios
    private List<DtoNodoJerarquia> arbol(Long idDepartamento) {
        List<Usuario> empleados = usuarioRepository.findAll();
        List<DtoNodoJerarquia> raices = new ArrayList<>();
        List<Departamento> departamentos = departamentoRepository.findAll();

        for (int i = 0; i < departamentos.size(); i++) {
            Departamento departamento = departamentos.get(i);
            if (idDepartamento != null && !idDepartamento.equals(departamento.getId())) {
                continue;
            }
            List<String> roles = new ArrayList<>();
            List<List<DtoNodoJerarquia>> personas = new ArrayList<>();

            for (int j = 0; j < empleados.size(); j++) {
                Usuario empleado = empleados.get(j);
                if (empleado.getIdDepartamento() == null
                        || !departamento.getId().equals(empleado.getIdDepartamento().getId())) {
                    continue;
                }
                String rol = empleado.getIdRol() == null ? "Sin rol" : empleado.getIdRol().getRol();
                int posicion = roles.indexOf(rol);
                if (posicion < 0) {
                    roles.add(rol);
                    personas.add(new ArrayList<>());
                    posicion = roles.size() - 1;
                }
                personas.get(posicion).add(
                        new DtoNodoJerarquia(empleado.getNombre(), 1, new ArrayList<>()));
            }

            List<DtoNodoJerarquia> nodosRol = new ArrayList<>();
            for (int j = 0; j < roles.size(); j++) {
                nodosRol.add(new DtoNodoJerarquia(roles.get(j), personas.get(j).size(), personas.get(j)));
            }
            if (!nodosRol.isEmpty()) {
                raices.add(new DtoNodoJerarquia(departamento.getNombre(), nodosRol.size(), nodosRol));
            }
        }
        return raices;
    }

    // Histograma de los días que tardó la resolución
    private List<DtoConteo> histograma(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        String[] rangos = {"0 a 2 días", "3 a 5 días", "6 a 10 días", "11 a 20 días",
                "21 a 30 días", "Más de 30 días"};
        long[] conteos = new long[rangos.length];

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime resolucion = resoluciones.get(caso.getId());
            if (resolucion == null) {
                continue;
            }
            double dias = dias(caso.getCreateAt(), resolucion);
            int posicion;
            if (dias <= 2) {
                posicion = 0;
            } else if (dias <= 5) {
                posicion = 1;
            } else if (dias <= 10) {
                posicion = 2;
            } else if (dias <= 20) {
                posicion = 3;
            } else if (dias <= 30) {
                posicion = 4;
            } else {
                posicion = 5;
            }
            conteos[posicion] = conteos[posicion] + 1;
        }

        List<DtoConteo> resultado = new ArrayList<>();
        for (int i = 0; i < rangos.length; i++) {
            resultado.add(new DtoConteo(rangos[i], conteos[i]));
        }
        return resultado;
    }

    // Campana de Gauss: los días de resolución repartidos en tramos de dos días.
    // La pantalla dibuja encima la curva normal con la media y la desviación.
    private List<DtoConteo> distribucionDias(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<Double> dias = diasDeResolucion(casos, resoluciones);
        List<DtoConteo> tramos = new ArrayList<>();
        if (dias.isEmpty()) {
            return tramos;
        }

        double maximo = 0;
        for (int i = 0; i < dias.size(); i++) {
            if (dias.get(i) > maximo) {
                maximo = dias.get(i);
            }
        }
        int cantidadTramos = (int) Math.ceil(maximo / ANCHO_TRAMO) + 1;
        if (cantidadTramos > 40) {
            cantidadTramos = 40;
        }
        long[] conteos = new long[cantidadTramos];

        for (int i = 0; i < dias.size(); i++) {
            int tramo = (int) (dias.get(i) / ANCHO_TRAMO);
            if (tramo >= cantidadTramos) {
                tramo = cantidadTramos - 1;
            }
            conteos[tramo] = conteos[tramo] + 1;
        }

        // La etiqueta es el centro del tramo, para que la curva caiga sobre las barras
        for (int i = 0; i < cantidadTramos; i++) {
            double centro = i * ANCHO_TRAMO + ANCHO_TRAMO / 2.0;
            tramos.add(new DtoConteo(String.valueOf(redondear(centro)), conteos[i]));
        }
        return tramos;
    }

    private double mediaDias(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<Double> dias = diasDeResolucion(casos, resoluciones);
        if (dias.isEmpty()) {
            return 0;
        }
        double suma = 0;
        for (int i = 0; i < dias.size(); i++) {
            suma = suma + dias.get(i);
        }
        return redondear(suma / dias.size());
    }

    private double desviacionDias(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<Double> dias = diasDeResolucion(casos, resoluciones);
        if (dias.size() < 2) {
            return 0;
        }
        double media = mediaDias(casos, resoluciones);
        double suma = 0;
        for (int i = 0; i < dias.size(); i++) {
            double diferencia = dias.get(i) - media;
            suma = suma + diferencia * diferencia;
        }
        return redondear(Math.sqrt(suma / (dias.size() - 1)));
    }

    private List<Double> diasDeResolucion(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<Double> dias = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime resolucion = resoluciones.get(caso.getId());
            if (resolucion != null) {
                dias.add(dias(caso.getCreateAt(), resolucion));
            }
        }
        return dias;
    }

    // Diagrama de caja de los tiempos de cada Técnico
    private List<DtoCajaTecnico> boxplot(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<String> tecnicos = new ArrayList<>();
        List<List<Double>> tiempos = new ArrayList<>();

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime resolucion = resoluciones.get(caso.getId());
            if (caso.getTecnico() == null || resolucion == null) {
                continue;
            }
            String nombre = caso.getTecnico().getNombre();
            int posicion = tecnicos.indexOf(nombre);
            if (posicion < 0) {
                tecnicos.add(nombre);
                tiempos.add(new ArrayList<>());
                posicion = tecnicos.size() - 1;
            }
            tiempos.get(posicion).add(dias(caso.getCreateAt(), resolucion));
        }

        List<DtoCajaTecnico> cajas = new ArrayList<>();
        for (int i = 0; i < tecnicos.size(); i++) {
            List<Double> valores = tiempos.get(i);
            Collections.sort(valores);
            cajas.add(new DtoCajaTecnico(
                    tecnicos.get(i),
                    redondear(valores.get(0)),
                    redondear(percentil(valores, 25)),
                    redondear(percentil(valores, 50)),
                    redondear(percentil(valores, 75)),
                    redondear(valores.get(valores.size() - 1))));
        }
        return cajas;
    }

    private double percentil(List<Double> ordenados, int porcentaje) {
        if (ordenados.isEmpty()) {
            return 0;
        }
        int posicion = (int) Math.round((porcentaje / 100.0) * (ordenados.size() - 1));
        return ordenados.get(posicion);
    }

    // Dispersión y burbujas: carga del Técnico contra su tiempo promedio
    private List<DtoPuntoDispersion> dispersion(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<Grupo> grupos = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            if (caso.getTecnico() == null) {
                continue;
            }
            Grupo grupo = obtener(grupos, caso.getTecnico().getNombre());
            grupo.total += 1;
            LocalDateTime resolucion = resoluciones.get(caso.getId());
            if (resolucion != null) {
                grupo.resueltos += 1;
                grupo.sumaDias += dias(caso.getCreateAt(), resolucion);
                grupo.conteoDias += 1;
            }
        }

        List<DtoPuntoDispersion> puntos = new ArrayList<>();
        for (int i = 0; i < grupos.size(); i++) {
            Grupo grupo = grupos.get(i);
            puntos.add(new DtoPuntoDispersion(
                    grupo.etiqueta,
                    grupo.total,
                    grupo.conteoDias == 0 ? 0 : redondear(grupo.sumaDias / grupo.conteoDias),
                    grupo.resueltos));
        }
        return puntos;
    }

    // Mapa de calor: día de la semana contra hora
    private DtoMapaCalor calorDiaHora(List<Caso> casos, Map<Long, LocalDateTime> resoluciones,
                                      boolean porResolucion) {
        List<String> horas = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            horas.add((i < 10 ? "0" + i : String.valueOf(i)) + ":00");
        }
        List<String> dias = List.of(DIAS_SEMANA);
        long[][] matriz = new long[dias.size()][horas.size()];

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            if (base == null) {
                continue;
            }
            int fila = base.getDayOfWeek().getValue() - 1;
            matriz[fila][base.getHour()] = matriz[fila][base.getHour()] + 1;
        }
        return new DtoMapaCalor(dias, horas, celdas(matriz));
    }

    // Mapa de calor: categoría contra mes
    private DtoMapaCalor calorCategoriaMes(List<Caso> casos, Map<Long, LocalDateTime> resoluciones,
                                           boolean porResolucion) {
        List<String> meses = mesesDe(casos, resoluciones, porResolucion);
        List<String> categorias = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            String nombre = casos.get(i).getCategoria().getNombre();
            if (!categorias.contains(nombre)) {
                categorias.add(nombre);
            }
        }
        Collections.sort(categorias);

        long[][] matriz = new long[categorias.size()][meses.size()];
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            if (base == null) {
                continue;
            }
            int fila = categorias.indexOf(caso.getCategoria().getNombre());
            int columna = meses.indexOf(etiquetaTemporal(base, POR_MES));
            if (fila >= 0 && columna >= 0) {
                matriz[fila][columna] = matriz[fila][columna] + 1;
            }
        }
        return new DtoMapaCalor(categorias, meses, celdas(matriz));
    }

    private List<DtoCeldaCalor> celdas(long[][] matriz) {
        List<DtoCeldaCalor> celdas = new ArrayList<>();
        for (int fila = 0; fila < matriz.length; fila++) {
            for (int columna = 0; columna < matriz[fila].length; columna++) {
                celdas.add(new DtoCeldaCalor(fila, columna, matriz[fila][columna]));
            }
        }
        return celdas;
    }

    private List<DtoPuntoCalendario> calendario(List<Caso> casos, Map<Long, LocalDateTime> resoluciones,
                                                boolean porResolucion) {
        List<Grupo> grupos = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            if (base != null) {
                obtener(grupos, base.toLocalDate().toString()).total += 1;
            }
        }
        List<DtoPuntoCalendario> puntos = new ArrayList<>();
        for (int i = 0; i < grupos.size(); i++) {
            puntos.add(new DtoPuntoCalendario(grupos.get(i).etiqueta, grupos.get(i).total));
        }
        return puntos;
    }

    // Pareto de los motivos de rechazo
    private List<DtoPuntoPareto> pareto(List<Caso> casos) {
        List<Grupo> grupos = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            if (caso.getMotivoRechazo() == null) {
                continue;
            }
            obtener(grupos, caso.getMotivoRechazo().getMotivo()).total += 1;
            total = total + 1;
        }
        grupos.sort((uno, otro) -> Long.compare(otro.total, uno.total));

        List<DtoPuntoPareto> puntos = new ArrayList<>();
        long acumulado = 0;
        for (int i = 0; i < grupos.size(); i++) {
            acumulado = acumulado + grupos.get(i).total;
            puntos.add(new DtoPuntoPareto(
                    grupos.get(i).etiqueta,
                    grupos.get(i).total,
                    total == 0 ? 0 : redondear(acumulado * 100.0 / total)));
        }
        return puntos;
    }

    // Cascada: de los casos recibidos a los resueltos. Los valores negativos restan.
    // El rechazo en trámite se muestra aparte del rechazo ya aprobado.
    private List<DtoConteo> cascada(List<Caso> casos) {
        long resueltos = 0;
        long rechazados = 0;
        long enTramite = 0;
        for (int i = 0; i < casos.size(); i++) {
            String estado = casos.get(i).getEstado().getEstado();
            if (EstadoCaso.RESUELTO.equals(estado)) {
                resueltos = resueltos + 1;
            } else if (EstadoCaso.RECHAZADO.equals(estado) || EstadoCaso.REQUIERE_AMPLIACION.equals(estado)) {
                rechazados = rechazados + 1;
            } else if (EstadoCaso.PENDIENTE_APROBACION_RECHAZO.equals(estado)) {
                enTramite = enTramite + 1;
            }
        }
        long enProceso = casos.size() - resueltos - rechazados - enTramite;

        List<DtoConteo> pasos = new ArrayList<>();
        pasos.add(new DtoConteo("Recibidos", casos.size()));
        pasos.add(new DtoConteo("Rechazados", -rechazados));
        pasos.add(new DtoConteo("Rechazo en trámite", -enTramite));
        pasos.add(new DtoConteo("En proceso", -enProceso));
        pasos.add(new DtoConteo("Resueltos", resueltos));
        return pasos;
    }

    // Escalonada: casos abiertos acumulados al cierre de cada mes
    private List<DtoConteo> acumuladoAbiertos(List<Caso> casos, Map<Long, LocalDateTime> cierres) {
        List<String> meses = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            String mes = etiquetaTemporal(casos.get(i).getCreateAt(), POR_MES);
            if (!meses.contains(mes)) {
                meses.add(mes);
            }
        }
        Collections.sort(meses);

        List<DtoConteo> resultado = new ArrayList<>();
        for (int i = 0; i < meses.size(); i++) {
            String mes = meses.get(i);
            long abiertos = 0;
            for (int j = 0; j < casos.size(); j++) {
                Caso caso = casos.get(j);
                if (etiquetaTemporal(caso.getCreateAt(), POR_MES).compareTo(mes) > 0) {
                    continue;
                }
                LocalDateTime cierre = cierres.get(caso.getId());
                if (cierre == null || etiquetaTemporal(cierre, POR_MES).compareTo(mes) > 0) {
                    abiertos = abiertos + 1;
                }
            }
            resultado.add(new DtoConteo(mes, abiertos));
        }
        return resultado;
    }

    private List<String> mesesDe(List<Caso> casos, Map<Long, LocalDateTime> resoluciones,
                                 boolean porResolucion) {
        List<String> meses = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            if (base == null) {
                continue;
            }
            String mes = etiquetaTemporal(base, POR_MES);
            if (!meses.contains(mes)) {
                meses.add(mes);
            }
        }
        Collections.sort(meses);
        return meses;
    }

    // Combinada: casos recibidos por mes junto al tiempo promedio de ese mes
    private List<Double> recibidosPorMes(List<Caso> casos, Map<Long, LocalDateTime> resoluciones,
                                         boolean porResolucion) {
        List<String> meses = mesesDe(casos, resoluciones, porResolucion);
        List<Double> valores = nuevaLista(meses.size());
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime base = porResolucion ? resoluciones.get(caso.getId()) : caso.getCreateAt();
            if (base == null) {
                continue;
            }
            int posicion = meses.indexOf(etiquetaTemporal(base, POR_MES));
            if (posicion >= 0) {
                valores.set(posicion, valores.get(posicion) + 1);
            }
        }
        return valores;
    }

    private List<Double> tiempoPorMes(List<Caso> casos, Map<Long, LocalDateTime> resoluciones,
                                      boolean porResolucion) {
        List<String> meses = mesesDe(casos, resoluciones, porResolucion);
        List<Double> sumas = nuevaLista(meses.size());
        List<Double> conteos = nuevaLista(meses.size());

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            LocalDateTime resolucion = resoluciones.get(caso.getId());
            LocalDateTime base = porResolucion ? resolucion : caso.getCreateAt();
            if (resolucion == null || base == null) {
                continue;
            }
            int posicion = meses.indexOf(etiquetaTemporal(base, POR_MES));
            if (posicion >= 0) {
                sumas.set(posicion, sumas.get(posicion) + dias(caso.getCreateAt(), resolucion));
                conteos.set(posicion, conteos.get(posicion) + 1);
            }
        }

        List<Double> promedios = new ArrayList<>();
        for (int i = 0; i < meses.size(); i++) {
            promedios.add(conteos.get(i) == 0 ? 0 : redondear(sumas.get(i) / conteos.get(i)));
        }
        return promedios;
    }

    // Tabla de Técnicos con semáforo por tiempo promedio de resolución
    private List<DtoFilaTecnico> tablaTecnicos(List<Caso> casos, Map<Long, LocalDateTime> resoluciones) {
        List<String> nombres = new ArrayList<>();
        List<String> departamentos = new ArrayList<>();
        List<Grupo> grupos = new ArrayList<>();

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            if (caso.getTecnico() == null) {
                continue;
            }
            String nombre = caso.getTecnico().getNombre();
            if (!nombres.contains(nombre)) {
                nombres.add(nombre);
                departamentos.add(caso.getDepartamento().getNombre());
            }
            Grupo grupo = obtener(grupos, nombre);
            if (EstadoCaso.EN_ATENCION.equals(caso.getEstado().getEstado())) {
                grupo.total += 1;
            }
            LocalDateTime resolucion = resoluciones.get(caso.getId());
            if (resolucion != null) {
                grupo.resueltos += 1;
                grupo.sumaDias += dias(caso.getCreateAt(), resolucion);
                grupo.conteoDias += 1;
            }
        }

        List<DtoFilaTecnico> filas = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            Grupo grupo = buscar(grupos, nombres.get(i));
            double promedio = grupo == null || grupo.conteoDias == 0
                    ? 0 : redondear(grupo.sumaDias / grupo.conteoDias);
            filas.add(new DtoFilaTecnico(
                    nombres.get(i),
                    departamentos.get(i),
                    grupo == null ? 0 : grupo.total,
                    grupo == null ? 0 : grupo.resueltos,
                    promedio,
                    semaforo(promedio, grupo == null ? 0 : grupo.resueltos)));
        }
        return filas;
    }

    // Verde hasta 10 días, amarillo hasta 20 y rojo arriba de eso.
    // Sin casos resueltos todavía no hay tiempo que calificar, queda en amarillo.
    private String semaforo(double promedio, long resueltos) {
        if (resueltos == 0) {
            return "amarillo";
        }
        if (promedio <= 10) {
            return "verde";
        }
        if (promedio <= 20) {
            return "amarillo";
        }
        return "rojo";
    }

    // El ciudadano se muestra por nombre; el DPI completo solo cuando se filtra por él
    private String nombreCiudadano(Caso caso, String dpiFiltrado) {
        String nombre = caso.getCiudadano().getNombre();
        if (dpiFiltrado != null && dpiFiltrado.equals(caso.getCiudadano().getDpi())) {
            return nombre + " (" + caso.getCiudadano().getDpi() + ")";
        }
        return nombre;
    }

    private List<DtoConteo> rankingCiudadanos(List<Caso> casos, String dpiFiltrado) {
        List<Grupo> grupos = new ArrayList<>();
        for (int i = 0; i < casos.size(); i++) {
            obtener(grupos, nombreCiudadano(casos.get(i), dpiFiltrado)).total += 1;
        }
        grupos.sort((uno, otro) -> Long.compare(otro.total, uno.total));

        List<DtoConteo> resultado = new ArrayList<>();
        for (int i = 0; i < grupos.size() && i < 10; i++) {
            resultado.add(new DtoConteo(grupos.get(i).etiqueta, grupos.get(i).total));
        }
        return resultado;
    }

    // Cuántos casos le rechazaron a cada ciudadano. Rechazados son Rechazado y
    // Requiere Ampliación; Pendiente de Aprobación de Rechazo es rechazo en trámite.
    private List<DtoConteo> rechazosCiudadanos(List<Caso> casos, String dpiFiltrado, boolean enTramite) {
        List<DtoConteo> ranking = rankingCiudadanos(casos, dpiFiltrado);
        List<DtoConteo> resultado = new ArrayList<>();

        for (int i = 0; i < ranking.size(); i++) {
            String ciudadano = ranking.get(i).etiqueta();
            long cantidad = 0;
            for (int j = 0; j < casos.size(); j++) {
                Caso caso = casos.get(j);
                if (!ciudadano.equals(nombreCiudadano(caso, dpiFiltrado))) {
                    continue;
                }
                String estado = caso.getEstado().getEstado();
                boolean rechazoTramite = EstadoCaso.PENDIENTE_APROBACION_RECHAZO.equals(estado);
                boolean rechazado = EstadoCaso.RECHAZADO.equals(estado)
                        || EstadoCaso.REQUIERE_AMPLIACION.equals(estado);
                if (enTramite ? rechazoTramite : rechazado) {
                    cantidad = cantidad + 1;
                }
            }
            resultado.add(new DtoConteo(ciudadano, cantidad));
        }
        return resultado;
    }

    // Línea de tiempo de la trazabilidad de un caso
    private String codigoTrazabilidad(List<Caso> casos, String codigoSeguimiento) {
        Caso caso = casoTrazabilidad(casos, codigoSeguimiento);
        return caso == null ? "" : caso.getCodigoSeguimiento();
    }

    private Caso casoTrazabilidad(List<Caso> casos, String codigoSeguimiento) {
        if (casos.isEmpty()) {
            return null;
        }
        if (!vacio(codigoSeguimiento)) {
            String codigo = codigoSeguimiento.trim().toUpperCase();
            for (int i = 0; i < casos.size(); i++) {
                if (codigo.equals(casos.get(i).getCodigoSeguimiento())) {
                    return casos.get(i);
                }
            }
            return null;
        }
        // Sin código se muestra el caso más reciente del resultado filtrado
        return casos.get(casos.size() - 1);
    }

    private List<DtoHistorialCaso> trazabilidad(List<Caso> casos, List<BitacoraCambio> historial,
                                                String codigoSeguimiento) {
        Caso caso = casoTrazabilidad(casos, codigoSeguimiento);
        List<DtoHistorialCaso> lineas = new ArrayList<>();
        if (caso == null) {
            return lineas;
        }
        for (int i = 0; i < historial.size(); i++) {
            BitacoraCambio registro = historial.get(i);
            if (registro.getCaso() == null || !caso.getId().equals(registro.getCaso().getId())) {
                continue;
            }
            lineas.add(new DtoHistorialCaso(
                    registro.getAccion(),
                    registro.getUsuario(),
                    registro.getFecha(),
                    registro.getHora(),
                    registro.getIpOrigen(),
                    registro.getDetalle() == null ? "" : registro.getDetalle()));
        }
        return lineas;
    }

    // ================= Métodos de apoyo =================

    private boolean esMetricaSistema(String metrica) {
        return METRICA_ACCIONES.equals(metrica) || METRICA_LOGIN_OK.equals(metrica)
                || METRICA_LOGIN_FALLIDO.equals(metrica) || METRICA_ERROR_NOTIFICACION.equals(metrica);
    }

    private boolean esTemporal(String agrupacion) {
        return POR_DIA.equals(agrupacion) || POR_SEMANA.equals(agrupacion)
                || POR_MES.equals(agrupacion) || POR_DIA_SEMANA.equals(agrupacion)
                || POR_HORA.equals(agrupacion);
    }

    private String formato(String metrica) {
        if (METRICA_PORCENTAJE.equals(metrica)) {
            return FORMATO_PORCENTAJE;
        }
        if (METRICA_TIEMPO.equals(metrica)) {
            return FORMATO_DIAS;
        }
        return FORMATO_ENTERO;
    }

    private String tituloMetrica(String metrica) {
        if (METRICA_RESUELTOS.equals(metrica)) {
            return "Casos resueltos";
        }
        if (METRICA_PORCENTAJE.equals(metrica)) {
            return "Porcentaje de resolución";
        }
        if (METRICA_TIEMPO.equals(metrica)) {
            return "Tiempo promedio de resolución";
        }
        if (METRICA_ACCIONES.equals(metrica)) {
            return "Acciones de bitácora";
        }
        if (METRICA_LOGIN_OK.equals(metrica)) {
            return "Inicios de sesión exitosos";
        }
        if (METRICA_LOGIN_FALLIDO.equals(metrica)) {
            return "Inicios de sesión fallidos";
        }
        if (METRICA_ERROR_NOTIFICACION.equals(metrica)) {
            return "Errores de notificación";
        }
        return "Casos";
    }

    // Tiempo de resolución: firma del oficio menos el registro del caso
    private double dias(LocalDateTime registro, LocalDateTime resolucion) {
        return ChronoUnit.MINUTES.between(registro, resolucion) / 1440.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private List<Double> nuevaLista(int tamano) {
        List<Double> valores = new ArrayList<>();
        for (int i = 0; i < tamano; i++) {
            valores.add(0.0);
        }
        return valores;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // AN02 # 30: el rango de fechas debe ser válido
    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null
                || hasta.isBefore(desde)
                || ChronoUnit.DAYS.between(desde, hasta) > MAXIMO_DIAS_RANGO) {
            throw new NegocioException(CodigoError.RANGO_FECHAS_INVALIDO);
        }
    }

    // AN02 # 12 y # 14: el DPI lleva 13 dígitos y debe existir el ciudadano
    private String validarCiudadano(String dpiCiudadano) {
        if (vacio(dpiCiudadano)) {
            return null;
        }
        String dpi = dpiCiudadano.trim();
        if (!DPI.matcher(dpi).matches()) {
            throw new NegocioException(CodigoError.DPI_INVALIDO);
        }
        if (!ciudadanoRepository.existsById(dpi)) {
            throw new NegocioException(CodigoError.CIUDADANO_NO_ENCONTRADO);
        }
        return dpi;
    }

    private void validarMetrica(String metrica) {
        boolean valida = METRICA_CASOS.equals(metrica) || METRICA_RESUELTOS.equals(metrica)
                || METRICA_PORCENTAJE.equals(metrica) || METRICA_TIEMPO.equals(metrica)
                || esMetricaSistema(metrica);
        if (!valida) {
            throw new NegocioException(CodigoError.VALIDACION);
        }
    }

    private void validarAgrupacion(String agrupacion) {
        boolean valida = POR_ESTADO.equals(agrupacion) || POR_TIPO.equals(agrupacion)
                || POR_DEPARTAMENTO.equals(agrupacion) || POR_CATEGORIA.equals(agrupacion)
                || POR_TECNICO.equals(agrupacion) || POR_CARGO.equals(agrupacion)
                || POR_USUARIO.equals(agrupacion) || POR_CIUDADANO.equals(agrupacion)
                || POR_MOTIVO.equals(agrupacion) || POR_ACCION.equals(agrupacion)
                || esTemporal(agrupacion);
        if (!valida) {
            throw new NegocioException(CodigoError.VALIDACION);
        }
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolAdmin(Usuario admin) {
        if (admin == null || admin.getIdRol() == null || !ROL_ADMIN.equals(admin.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }
}
