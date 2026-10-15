package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCatalogosUsuario;
import gt.muni.quejas.dto.DtoGuardarUsuario;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.dto.DtoOpcionCatalogo;
import gt.muni.quejas.dto.DtoUsuarioAdmin;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Departamento;
import gt.muni.quejas.model.EstadoCaso;
import gt.muni.quejas.model.EstadoUsuarios;
import gt.muni.quejas.model.Rol;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.DepartamentoRepository;
import gt.muni.quejas.repository.EstadoCasoRepository;
import gt.muni.quejas.repository.EstadoUsuarioRepository;
import gt.muni.quejas.repository.RolRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import gt.muni.quejas.util.PasswordRn08;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUsuarioService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_USUARIO_GUARDADO = "El usuario fue guardado exitosamente.";

    private static final String ROL_ADMIN = "Admin";
    private static final String ROL_TECNICO = "Tecnico";
    private static final String ROL_AUXILIAR = "Auxiliar";
    private static final String ROL_SUPERVISOR = "Supervisor";
    private static final String ROL_JEFE = "Jefe";
    private static final String ESTADO_ACTIVO = "Activo";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DepartamentoRepository departamentoRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraCambioService bitacoraCambioService;
    private final NotificacionCasoService notificacionCasoService;

    public AdminUsuarioService(UsuarioRepository usuarioRepository,
                               RolRepository rolRepository,
                               DepartamentoRepository departamentoRepository,
                               EstadoUsuarioRepository estadoUsuarioRepository,
                               CasoRepository casoRepository,
                               EstadoCasoRepository estadoCasoRepository,
                               PasswordEncoder passwordEncoder,
                               BitacoraCambioService bitacoraCambioService,
                               NotificacionCasoService notificacionCasoService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.departamentoRepository = departamentoRepository;
        this.estadoUsuarioRepository = estadoUsuarioRepository;
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraCambioService = bitacoraCambioService;
        this.notificacionCasoService = notificacionCasoService;
    }

    // ---------- CU-10 paso 4: listado de usuarios internos ----------
    @Transactional(readOnly = true)
    public List<DtoUsuarioAdmin> listarUsuarios(Usuario admin) {
        validarRolAdmin(admin);

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<DtoUsuarioAdmin> resultado = new ArrayList<>();
        for (int i = 0; i < usuarios.size(); i++) {
            resultado.add(convertir(usuarios.get(i)));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public DtoUsuarioAdmin obtenerUsuario(String cui, Usuario admin) {
        validarRolAdmin(admin);
        return convertir(buscarUsuario(cui));
    }

    // ---------- CU-10 pasos 4 y 7: listas de los filtros y del formulario usuario ----------
    @Transactional(readOnly = true)
    public DtoCatalogosUsuario obtenerCatalogos(Usuario admin) {
        validarRolAdmin(admin);

        List<Rol> roles = rolRepository.findAll();
        List<DtoOpcionCatalogo> opcionesRol = new ArrayList<>();
        for (int i = 0; i < roles.size(); i++) {
            opcionesRol.add(new DtoOpcionCatalogo(roles.get(i).getId(), roles.get(i).getRol()));
        }

        List<Departamento> departamentos = departamentoRepository.findAll();
        List<DtoOpcionCatalogo> opcionesDepartamento = new ArrayList<>();
        for (int i = 0; i < departamentos.size(); i++) {
            opcionesDepartamento.add(new DtoOpcionCatalogo(
                    departamentos.get(i).getId(), departamentos.get(i).getNombre()));
        }

        List<EstadoUsuarios> estados = estadoUsuarioRepository.findAll();
        List<DtoOpcionCatalogo> opcionesEstado = new ArrayList<>();
        for (int i = 0; i < estados.size(); i++) {
            opcionesEstado.add(new DtoOpcionCatalogo(estados.get(i).getId(), estados.get(i).getEstado()));
        }

        return new DtoCatalogosUsuario(opcionesRol, opcionesDepartamento, opcionesEstado);
    }

    // ---------- CU-10 pasos 9 al 14: Nuevo Usuario ----------
    @Transactional
    public DtoMensaje crearUsuario(DtoGuardarUsuario dto, Usuario admin, String ip) {
        validarRolAdmin(admin);
        validarCamposObligatorios(dto, true);

        String cui = dto.cui().trim();
        String correo = dto.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCui(cui) || usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new NegocioException(CodigoError.USUARIO_DUPLICADO);
        }

        Usuario usuario = new Usuario();
        usuario.setCui(cui);
        usuario.setNombre(dto.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setIdRol(buscarRol(dto.idRol()));
        usuario.setIdDepartamento(buscarDepartamento(dto.idDepartamento()));
        usuario.setIdEstado(buscarEstado(dto.idEstado()));
        usuario.setIntentosFallidos(0);
        usuario.setCantidadCasosAsignados(0);
        usuario.setCreateAt(LocalDateTime.now());
        usuario.setUsuarioCreacion(admin.getCui());
        usuarioRepository.save(usuario);

        bitacoraCambioService.registrarCambio(
                "Creación de usuario",
                admin.getCui(),
                BitacoraCambioService.ENTIDAD_USUARIO,
                null,
                ip,
                "Se creó el usuario " + correo + " con rol " + usuario.getIdRol().getRol()
        );

        return new DtoMensaje(MENSAJE_USUARIO_GUARDADO);
    }

    // ---------- CU-10 FA03: editar un usuario existente ----------
    @Transactional
    public DtoMensaje editarUsuario(String cui, DtoGuardarUsuario dto, Usuario admin, String ip) {
        validarRolAdmin(admin);
        validarCamposObligatorios(dto, false);

        Usuario usuario = buscarUsuario(cui);
        String correo = dto.correo().trim().toLowerCase();

        // El correo es único: solo puede repetirse si es el del mismo usuario
        Usuario conEseCorreo = usuarioRepository.findByCorreo(correo).orElse(null);
        if (conEseCorreo != null && !conEseCorreo.getCui().equals(usuario.getCui())) {
            throw new NegocioException(CodigoError.USUARIO_DUPLICADO);
        }

        EstadoUsuarios estadoNuevo = buscarEstado(dto.idEstado());
        Rol rolNuevo = buscarRol(dto.idRol());
        Departamento departamentoNuevo = buscarDepartamento(dto.idDepartamento());

        // FA11: el estado, el rol y el departamento definen la bandeja que atiende el
        // usuario. Si tiene casos a su cargo primero hay que reasignarlos con el botón
        // Estado (FA08); de lo contrario esos casos dejarían de aparecer en cualquier
        // bandeja y nadie podría atenderlos.
        if (!estadoNuevo.getEstado().equals(usuario.getIdEstado().getEstado())) {
            validarSinCasosActivos(usuario, estadoNuevo);
        }
        if (!rolNuevo.getId().equals(usuario.getIdRol().getId())
                || !departamentoNuevo.getId().equals(usuario.getIdDepartamento().getId())) {
            validarSinCasosPendientes(usuario);
        }

        usuario.setNombre(dto.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setIdRol(rolNuevo);
        usuario.setIdDepartamento(departamentoNuevo);
        usuario.setIdEstado(estadoNuevo);

        // La contraseña solo cambia si el Admin escribió una nueva
        if (dto.password() != null && !dto.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.password()));
        }
        usuarioRepository.save(usuario);

        bitacoraCambioService.registrarCambio(
                "Edición de usuario",
                admin.getCui(),
                BitacoraCambioService.ENTIDAD_USUARIO,
                null,
                ip,
                "Se editó el usuario " + correo
        );

        return new DtoMensaje(MENSAJE_USUARIO_GUARDADO);
    }

    // ---------- CU-10 FA04: cambio del estado de disponibilidad ----------
    // FA08: cuiDestino viene vacío en el primer intento; si el usuario tiene casos
    // pendientes se devuelve el error y el Admin reenvía la petición con el destino.
    @Transactional
    public DtoMensaje cambiarEstado(String cui, Long idEstado, String cuiDestino, Usuario admin, String ip) {
        validarRolAdmin(admin);

        Usuario usuario = buscarUsuario(cui);
        EstadoUsuarios estadoNuevo = buscarEstado(idEstado);
        String estadoAnterior = usuario.getIdEstado().getEstado();

        if (!ESTADO_ACTIVO.equals(estadoNuevo.getEstado())) {
            if (contarCasosPorReasignar(usuario) > 0) {
                reasignarCasos(usuario, cuiDestino, admin, ip);
            } else {
                validarBandejaConResponsable(usuario);
            }
        }

        usuario.setIdEstado(estadoNuevo);

        // Al reactivarlo se le limpian los intentos fallidos que lo bloquearon
        if (ESTADO_ACTIVO.equals(estadoNuevo.getEstado())) {
            usuario.reiniciarIntentosFallidos();
        }
        usuarioRepository.save(usuario);

        bitacoraCambioService.registrarCambio(
                "Cambio de estado de usuario",
                admin.getCui(),
                BitacoraCambioService.ENTIDAD_USUARIO,
                null,
                ip,
                "El usuario " + usuario.getCorreo() + " pasó de " + estadoAnterior
                        + " a " + estadoNuevo.getEstado()
        );

        return new DtoMensaje(MENSAJE_USUARIO_GUARDADO);
    }

    // ---------- CU-10 FA08: reasignar los casos del Técnico antes de inactivarlo ----------
    private void reasignarCasos(Usuario origen, String cuiDestino, Usuario admin, String ip) {
        List<Usuario> candidatos = buscarTecnicosDestino(origen);

        // FA10: sin otro Técnico activo en el departamento los casos quedarían sin
        // responsable, así que el Técnico no puede salir de Activo
        if (candidatos.isEmpty()) {
            throw new NegocioException(CodigoError.SIN_USUARIO_DESTINO);
        }
        if (cuiDestino == null || cuiDestino.isBlank()) {
            throw new NegocioException(CodigoError.USUARIO_CON_CASOS_ACTIVOS);
        }

        Usuario destino = buscarDestinoEntreCandidatos(candidatos, cuiDestino.trim());
        int reasignados = trasladarCasos(origen, destino, admin, ip);

        String resumen = reasignados == 1
                ? "Se reasignó 1 caso de "
                : "Se reasignaron " + reasignados + " casos de ";

        bitacoraCambioService.registrarCambio(
                "Reasignación de casos",
                admin.getCui(),
                BitacoraCambioService.ENTIDAD_USUARIO,
                null,
                ip,
                resumen + origen.getCorreo() + " al Técnico " + destino.getCorreo()
        );
    }

    // El destino atiende la misma bandeja: un Técnico en estado Activo del mismo
    // departamento, porque el caso se asigna siempre dentro de su departamento (CU-04)
    private List<Usuario> buscarTecnicosDestino(Usuario origen) {
        List<Usuario> tecnicos = usuarioRepository.buscarTecnicosActivosPorDepartamento(
                origen.getIdDepartamento().getId());

        List<Usuario> candidatos = new ArrayList<>();
        for (int i = 0; i < tecnicos.size(); i++) {
            if (!tecnicos.get(i).getCui().equals(origen.getCui())) {
                candidatos.add(tecnicos.get(i));
            }
        }
        return candidatos;
    }

    private Usuario buscarDestinoEntreCandidatos(List<Usuario> candidatos, String cuiDestino) {
        for (int i = 0; i < candidatos.size(); i++) {
            if (candidatos.get(i).getCui().equals(cuiDestino)) {
                return candidatos.get(i);
            }
        }
        throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
    }

    private int trasladarCasos(Usuario origen, Usuario destino, Usuario admin, String ip) {
        List<Caso> casos = casoRepository.findByTecnicoAndEstadoInOrderByCreateAtAsc(
                origen, estadosACargoDelTecnico());

        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            caso.setTecnico(destino);
            casoRepository.save(caso);

            // RN35: el Técnico que recibe el caso se entera por correo, igual que
            // cuando el Auxiliar le asigna uno nuevo (CU-09)
            notificacionCasoService.notificarReasignacionTecnico(caso, admin, ip);
        }

        // El contador refleja únicamente los casos En Atención, que es la carga con la
        // que el Auxiliar reparte los casos nuevos (CU-04)
        EstadoCaso enAtencion = buscarEstadoCaso(EstadoCaso.EN_ATENCION);
        destino.setCantidadCasosAsignados(
                (int) casoRepository.countByTecnicoAndEstado(destino, enAtencion));
        origen.setCantidadCasosAsignados(
                (int) casoRepository.countByTecnicoAndEstado(origen, enAtencion));
        usuarioRepository.save(destino);
        usuarioRepository.save(origen);

        return casos.size();
    }

    // ---------- Casos a nombre del Técnico (FA08) ----------
    // Estados en los que el Técnico sigue siendo el responsable del caso, incluso
    // mientras el Supervisor o la Jefatura lo tienen en su bandeja y pueden devolverlo.
    private List<EstadoCaso> estadosACargoDelTecnico() {
        List<String> nombres = new ArrayList<>();
        nombres.add(EstadoCaso.EN_ATENCION);
        nombres.add(EstadoCaso.EN_SUPERVISION);
        nombres.add(EstadoCaso.PENDIENTE_FIRMA);
        nombres.add(EstadoCaso.REQUIERE_AMPLIACION);

        List<EstadoCaso> estados = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            estados.add(buscarEstadoCaso(nombres.get(i)));
        }
        return estados;
    }

    // ---------- Casos activos del usuario (RN26) ----------
    // Cada rol muestra los casos pendientes de la bandeja que atiende: el Técnico los
    // suyos En Atención (CU-05), el Auxiliar los recién registrados de todos los
    // departamentos (CU-04), el Supervisor y el Jefe los de su propio departamento
    // (CU-06, CU-07), y el Administrador siempre cero porque no atiende casos.
    private int contarCasosActivos(Usuario usuario) {
        String rol = usuario.getIdRol() == null ? "" : usuario.getIdRol().getRol();

        if (ROL_TECNICO.equals(rol)) {
            // Al terminar la atención el caso pasa a la bandeja del Supervisor y deja
            // de contarle, aunque siga a su cargo
            return (int) casoRepository.contarPorTecnicoYEstados(
                    usuario, List.of(EstadoCaso.EN_ATENCION));
        }
        if (ROL_AUXILIAR.equals(rol)) {
            return (int) casoRepository.contarPorEstados(
                    List.of(EstadoCaso.REGISTRADO, EstadoCaso.EN_REVISION));
        }
        if (ROL_SUPERVISOR.equals(rol)) {
            return (int) contarEnSuDepartamento(usuario,
                    List.of(EstadoCaso.EN_SUPERVISION, EstadoCaso.PENDIENTE_APROBACION_RECHAZO));
        }
        if (ROL_JEFE.equals(rol)) {
            return (int) contarEnSuDepartamento(usuario, List.of(EstadoCaso.PENDIENTE_FIRMA));
        }
        return 0;
    }

    private long contarEnSuDepartamento(Usuario usuario, List<String> estados) {
        if (usuario.getIdDepartamento() == null) {
            return 0;
        }
        return casoRepository.contarPorDepartamentoYEstados(
                usuario.getIdDepartamento().getId(), estados);
    }

    // FA08: casos que quedarían a nombre del Técnico si saliera de Activo. Solo el
    // Técnico tiene casos a su nombre: las demás bandejas son compartidas y ahí ningún
    // caso depende de un usuario en particular, así que no hay nada que reasignar.
    // Aquí sí cuentan los que ya están En Supervisión o Pendiente de Firma, porque el
    // Supervisor o la Jefatura pueden devolverlos y regresarían a una bandeja que
    // nadie atiende; por eso se reasignan aunque no le sumen casos activos.
    private int contarCasosPorReasignar(Usuario usuario) {
        if (!esTecnico(usuario)) {
            return 0;
        }
        return (int) casoRepository.countByTecnicoAndEstadoIn(usuario, estadosACargoDelTecnico());
    }

    private boolean esTecnico(Usuario usuario) {
        return usuario.getIdRol() != null && ROL_TECNICO.equals(usuario.getIdRol().getRol());
    }

    private EstadoCaso buscarEstadoCaso(String nombre) {
        return estadoCasoRepository.findByEstado(nombre)
                .orElseThrow(() -> new NegocioException(
                        CodigoError.ERROR_INTERNO, "No existe el estado " + nombre));
    }

    // ---------- Métodos de apoyo ----------
    // FA11: no se puede inactivar, bloquear o ausentar a quien tiene casos pendientes
    // desde la edición del usuario. La reasignación se hace desde el botón Estado.
    private void validarSinCasosActivos(Usuario usuario, EstadoUsuarios estadoNuevo) {
        if (ESTADO_ACTIVO.equals(estadoNuevo.getEstado())) {
            return;
        }
        validarSinCasosPendientes(usuario);
    }

    private void validarSinCasosPendientes(Usuario usuario) {
        if (contarCasosPorReasignar(usuario) > 0) {
            throw new NegocioException(CodigoError.USUARIO_CON_CASOS_ACTIVOS);
        }
        validarBandejaConResponsable(usuario);
    }

    // FA12: las bandejas del Auxiliar, del Supervisor y del Jefe son compartidas por
    // rol, así que no tienen casos propios que reasignar (FA08 es solo del Técnico).
    // Pero si el usuario deja de atender su bandeja y esta queda con casos pendientes
    // sin ningún otro usuario Activo de ese rol, esos casos no aparecerían en ninguna
    // bandeja y nadie podría continuarlos.
    private void validarBandejaConResponsable(Usuario usuario) {
        String rol = usuario.getIdRol() == null ? "" : usuario.getIdRol().getRol();

        if (ROL_TECNICO.equals(rol) || ROL_ADMIN.equals(rol)) {
            return;
        }
        if (contarCasosActivos(usuario) == 0) {
            return;
        }

        // El Auxiliar atiende los casos de todos los departamentos (CU-04); el
        // Supervisor y el Jefe solo los de su propio departamento (CU-06, CU-07)
        Long idDepartamento = null;
        if (!ROL_AUXILIAR.equals(rol)) {
            if (usuario.getIdDepartamento() == null) {
                return;
            }
            idDepartamento = usuario.getIdDepartamento().getId();
        }

        long otrosActivos = usuarioRepository.contarActivosDelRol(rol, idDepartamento, usuario.getCui());
        if (otrosActivos == 0) {
            throw new NegocioException(CodigoError.BANDEJA_SIN_RESPONSABLE);
        }
    }

    private void validarCamposObligatorios(DtoGuardarUsuario dto, boolean esNuevo) {
        if (dto == null
                || vacio(dto.nombre()) || vacio(dto.correo())
                || dto.idRol() == null || dto.idDepartamento() == null || dto.idEstado() == null) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        if (esNuevo && vacio(dto.cui())) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        // RN08: alfanumerica y de minimo 6 caracteres, igual que al iniciar sesion
        if (esNuevo && !PasswordRn08.esValida(dto.password())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
        // En edición la contraseña es opcional, pero si viene debe cumplir RN08
        if (!esNuevo && dto.password() != null && !dto.password().isBlank()
                && !PasswordRn08.esValida(dto.password())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
        if (esNuevo && !dto.cui().trim().matches("\\d{13}")) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
        if (!dto.correo().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private DtoUsuarioAdmin convertir(Usuario usuario) {
        return new DtoUsuarioAdmin(
                usuario.getCui(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getIdRol() == null ? null : usuario.getIdRol().getId(),
                usuario.getIdRol() == null ? "" : usuario.getIdRol().getRol(),
                usuario.getIdDepartamento() == null ? null : usuario.getIdDepartamento().getId(),
                usuario.getIdDepartamento() == null ? "" : usuario.getIdDepartamento().getNombre(),
                usuario.getIdEstado() == null ? null : usuario.getIdEstado().getId(),
                usuario.getIdEstado() == null ? "" : usuario.getIdEstado().getEstado(),
                contarCasosActivos(usuario)
        );
    }

    private Usuario buscarUsuario(String cui) {
        return usuarioRepository.findByCui(cui)
                .orElseThrow(() -> new NegocioException(CodigoError.USUARIO_NO_ENCONTRADO));
    }

    private Rol buscarRol(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS));
    }

    private Departamento buscarDepartamento(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS));
    }

    private EstadoUsuarios buscarEstado(Long id) {
        return estadoUsuarioRepository.findById(id)
                .orElseThrow(() -> new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS));
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolAdmin(Usuario admin) {
        if (admin == null || admin.getIdRol() == null || !ROL_ADMIN.equals(admin.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }
}
