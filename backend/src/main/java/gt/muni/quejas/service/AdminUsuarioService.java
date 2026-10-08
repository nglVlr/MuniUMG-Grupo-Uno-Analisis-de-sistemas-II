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
    private static final String ESTADO_ACTIVO = "Activo";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DepartamentoRepository departamentoRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final CasoRepository casoRepository;
    private final EstadoCasoRepository estadoCasoRepository;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraCambioService bitacoraCambioService;

    public AdminUsuarioService(UsuarioRepository usuarioRepository,
                               RolRepository rolRepository,
                               DepartamentoRepository departamentoRepository,
                               EstadoUsuarioRepository estadoUsuarioRepository,
                               CasoRepository casoRepository,
                               EstadoCasoRepository estadoCasoRepository,
                               PasswordEncoder passwordEncoder,
                               BitacoraCambioService bitacoraCambioService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.departamentoRepository = departamentoRepository;
        this.estadoUsuarioRepository = estadoUsuarioRepository;
        this.casoRepository = casoRepository;
        this.estadoCasoRepository = estadoCasoRepository;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    // ---------- CU-10 paso 2: listado de usuarios internos ----------
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

    // ---------- CU-10 paso 4: listas del formulario ----------
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

    // ---------- CU-10 pasos 5 al 9: Nuevo Usuario ----------
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

    // ---------- CU-10 pasos 5 al 9: editar un usuario existente ----------
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
        if (!estadoNuevo.getEstado().equals(usuario.getIdEstado().getEstado())) {
            validarSinCasosActivos(usuario, estadoNuevo);
        }

        usuario.setNombre(dto.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setIdRol(buscarRol(dto.idRol()));
        usuario.setIdDepartamento(buscarDepartamento(dto.idDepartamento()));
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

    // ---------- CU-10 FA01: cambio del estado de disponibilidad ----------
    @Transactional
    public DtoMensaje cambiarEstado(String cui, Long idEstado, Usuario admin, String ip) {
        validarRolAdmin(admin);

        Usuario usuario = buscarUsuario(cui);
        EstadoUsuarios estadoNuevo = buscarEstado(idEstado);
        String estadoAnterior = usuario.getIdEstado().getEstado();

        validarSinCasosActivos(usuario, estadoNuevo);

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

    // ---------- CU-10 FA04: reasignar los casos activos a otro Técnico ----------
    @Transactional
    public DtoMensaje reasignarCasos(String cuiOrigen, String cuiDestino, Usuario admin, String ip) {
        validarRolAdmin(admin);

        Usuario origen = buscarUsuario(cuiOrigen);
        if (cuiDestino == null || cuiDestino.isBlank()) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        Usuario destino = buscarUsuario(cuiDestino.trim());

        if (origen.getCui().equals(destino.getCui())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }
        // El destino debe poder recibir casos: Técnico activo del mismo departamento
        if (!ROL_TECNICO.equals(destino.getIdRol().getRol())
                || !ESTADO_ACTIVO.equals(destino.getIdEstado().getEstado())
                || !destino.getIdDepartamento().getId().equals(origen.getIdDepartamento().getId())) {
            throw new NegocioException(CodigoError.DATOS_USUARIO_INVALIDOS);
        }

        EstadoCaso enAtencion = estadoCasoRepository.findByEstado(EstadoCaso.EN_ATENCION)
                .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No existe el estado En Atención"));

        List<Caso> casos = casoRepository.findByTecnicoAndEstadoOrderByCreateAtAsc(origen, enAtencion);
        for (int i = 0; i < casos.size(); i++) {
            Caso caso = casos.get(i);
            caso.setTecnico(destino);
            casoRepository.save(caso);
        }

        destino.setCantidadCasosAsignados(destino.getCantidadCasosAsignados() + casos.size());
        origen.setCantidadCasosAsignados(0);
        usuarioRepository.save(destino);
        usuarioRepository.save(origen);

        bitacoraCambioService.registrarCambio(
                "Reasignación de casos",
                admin.getCui(),
                BitacoraCambioService.ENTIDAD_USUARIO,
                null,
                ip,
                "Se reasignaron " + casos.size() + " casos de " + origen.getCorreo()
                        + " a " + destino.getCorreo()
        );

        return new DtoMensaje(MENSAJE_USUARIO_GUARDADO);
    }

    // ---------- Métodos de apoyo ----------
    // FA04: no se puede inactivar, bloquear o ausentar a quien tiene casos en atención
    private void validarSinCasosActivos(Usuario usuario, EstadoUsuarios estadoNuevo) {
        if (ESTADO_ACTIVO.equals(estadoNuevo.getEstado())) {
            return;
        }
        if (usuario.getCantidadCasosAsignados() > 0) {
            throw new NegocioException(CodigoError.USUARIO_CON_CASOS_ACTIVOS);
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
                usuario.getCantidadCasosAsignados()
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
