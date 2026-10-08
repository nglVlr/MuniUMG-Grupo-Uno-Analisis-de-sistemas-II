package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCatalogoElemento;
import gt.muni.quejas.dto.DtoGuardarCatalogo;
import gt.muni.quejas.dto.DtoMensaje;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Categoria;
import gt.muni.quejas.model.Departamento;
import gt.muni.quejas.model.MotivoRechazo;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.CasoRepository;
import gt.muni.quejas.repository.CategoriaRepository;
import gt.muni.quejas.repository.DepartamentoRepository;
import gt.muni.quejas.repository.MotivoRechazoRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import gt.muni.quejas.util.TipoCaso;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// CU-11 Administración de Catálogos: departamentos, categorías del problema y
// motivos de rechazo. Cada cambio limpia la caché para que el registro de casos
// vea el catálogo actualizado.
@Service
public class AdminCatalogoService {

    // Mensajes tal cual de las Reglas de Negocio (AN01)
    public static final String MENSAJE_CATALOGO_ACTUALIZADO = "El catálogo fue actualizado exitosamente.";

    public static final String CATALOGO_DEPARTAMENTOS = "departamentos";
    public static final String CATALOGO_CATEGORIAS = "categorias";
    public static final String CATALOGO_MOTIVOS = "motivos-rechazo";

    private static final String ROL_ADMIN = "Admin";
    private static final int MAXIMO_NOMBRE = 150;

    private final DepartamentoRepository departamentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MotivoRechazoRepository motivoRechazoRepository;
    private final CasoRepository casoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BitacoraCambioService bitacoraCambioService;

    public AdminCatalogoService(DepartamentoRepository departamentoRepository,
                                CategoriaRepository categoriaRepository,
                                MotivoRechazoRepository motivoRechazoRepository,
                                CasoRepository casoRepository,
                                UsuarioRepository usuarioRepository,
                                BitacoraCambioService bitacoraCambioService) {
        this.departamentoRepository = departamentoRepository;
        this.categoriaRepository = categoriaRepository;
        this.motivoRechazoRepository = motivoRechazoRepository;
        this.casoRepository = casoRepository;
        this.usuarioRepository = usuarioRepository;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    // ---------- CU-11 paso 4: listado del catálogo seleccionado ----------
    @Transactional(readOnly = true)
    public List<DtoCatalogoElemento> listar(String catalogo, Usuario admin) {
        validarRolAdmin(admin);

        List<DtoCatalogoElemento> resultado = new ArrayList<>();

        if (CATALOGO_DEPARTAMENTOS.equals(catalogo)) {
            List<Departamento> departamentos = departamentoRepository.findAll();
            for (int i = 0; i < departamentos.size(); i++) {
                Departamento departamento = departamentos.get(i);
                resultado.add(new DtoCatalogoElemento(
                        departamento.getId(), departamento.getNombre(), departamento.isActivo(),
                        null, null, null, departamentoEnUso(departamento.getId())));
            }
            return resultado;
        }

        if (CATALOGO_CATEGORIAS.equals(catalogo)) {
            List<Categoria> categorias = categoriaRepository.findAll();
            for (int i = 0; i < categorias.size(); i++) {
                Categoria categoria = categorias.get(i);
                resultado.add(new DtoCatalogoElemento(
                        categoria.getId(), categoria.getNombre(), categoria.isActivo(),
                        categoria.getDepartamento().getId(), categoria.getDepartamento().getNombre(),
                        new ArrayList<>(categoria.getTiposCaso()),
                        casoRepository.countByCategoriaId(categoria.getId()) > 0));
            }
            return resultado;
        }

        if (CATALOGO_MOTIVOS.equals(catalogo)) {
            List<MotivoRechazo> motivos = motivoRechazoRepository.findAll();
            for (int i = 0; i < motivos.size(); i++) {
                MotivoRechazo motivo = motivos.get(i);
                resultado.add(new DtoCatalogoElemento(
                        motivo.getId(), motivo.getMotivo(), Boolean.TRUE.equals(motivo.getActivo()),
                        null, null, null,
                        casoRepository.countByMotivoRechazoId(motivo.getId()) > 0));
            }
            return resultado;
        }

        throw new NegocioException(CodigoError.VALIDACION, "Catálogo no válido");
    }

    // ---------- CU-11 pasos 5 al 8: agregar un elemento ----------
    @Transactional
    @CacheEvict(value = "catalogos", allEntries = true)
    public DtoMensaje crear(String catalogo, DtoGuardarCatalogo dto, Usuario admin, String ip) {
        validarRolAdmin(admin);
        String nombre = validarNombre(dto);

        if (CATALOGO_DEPARTAMENTOS.equals(catalogo)) {
            if (departamentoRepository.existsByNombreIgnoreCase(nombre)) {
                throw new NegocioException(CodigoError.ELEMENTO_DUPLICADO);
            }
            Departamento departamento = new Departamento();
            departamento.setNombre(nombre);
            departamento.setActivo(activoDe(dto));
            departamentoRepository.save(departamento);

        } else if (CATALOGO_CATEGORIAS.equals(catalogo)) {
            if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
                throw new NegocioException(CodigoError.ELEMENTO_DUPLICADO);
            }
            Categoria categoria = new Categoria();
            categoria.setNombre(nombre);
            categoria.setActivo(activoDe(dto));
            // FA02: la categoría necesita su departamento responsable
            categoria.setDepartamento(buscarDepartamento(dto.idDepartamento()));
            categoria.setTiposCaso(validarTiposCaso(dto.tiposCaso()));
            categoriaRepository.save(categoria);

        } else if (CATALOGO_MOTIVOS.equals(catalogo)) {
            if (motivoRechazoRepository.existsByMotivoIgnoreCase(nombre)) {
                throw new NegocioException(CodigoError.ELEMENTO_DUPLICADO);
            }
            MotivoRechazo motivo = new MotivoRechazo();
            motivo.setMotivo(nombre);
            motivo.setActivo(activoDe(dto));
            motivoRechazoRepository.save(motivo);

        } else {
            throw new NegocioException(CodigoError.VALIDACION, "Catálogo no válido");
        }

        registrarEnBitacora(admin, ip, "Creación en catálogo", catalogo, nombre);
        return new DtoMensaje(MENSAJE_CATALOGO_ACTUALIZADO);
    }

    // ---------- CU-11 pasos 5 al 8: editar o inactivar un elemento ----------
    @Transactional
    @CacheEvict(value = "catalogos", allEntries = true)
    public DtoMensaje editar(String catalogo, Long id, DtoGuardarCatalogo dto, Usuario admin, String ip) {
        validarRolAdmin(admin);
        String nombre = validarNombre(dto);

        if (CATALOGO_DEPARTAMENTOS.equals(catalogo)) {
            Departamento departamento = buscarDepartamento(id);
            departamento.setNombre(nombre);
            departamento.setActivo(activoDe(dto));
            departamentoRepository.save(departamento);

        } else if (CATALOGO_CATEGORIAS.equals(catalogo)) {
            Categoria categoria = categoriaRepository.findById(id)
                    .orElseThrow(() -> new NegocioException(CodigoError.VALIDACION, "La categoría no existe"));
            categoria.setNombre(nombre);
            categoria.setActivo(activoDe(dto));
            if (dto.idDepartamento() != null) {
                categoria.setDepartamento(buscarDepartamento(dto.idDepartamento()));
            }
            categoria.setTiposCaso(validarTiposCaso(dto.tiposCaso()));
            categoriaRepository.save(categoria);

        } else if (CATALOGO_MOTIVOS.equals(catalogo)) {
            MotivoRechazo motivo = motivoRechazoRepository.findById(id)
                    .orElseThrow(() -> new NegocioException(CodigoError.VALIDACION, "El motivo no existe"));
            motivo.setMotivo(nombre);
            motivo.setActivo(activoDe(dto));
            motivoRechazoRepository.save(motivo);

        } else {
            throw new NegocioException(CodigoError.VALIDACION, "Catálogo no válido");
        }

        registrarEnBitacora(admin, ip, "Edición en catálogo", catalogo, nombre);
        return new DtoMensaje(MENSAJE_CATALOGO_ACTUALIZADO);
    }

    // ---------- CU-11 FA01: eliminar solo si el elemento no está en uso ----------
    @Transactional
    @CacheEvict(value = "catalogos", allEntries = true)
    public DtoMensaje eliminar(String catalogo, Long id, Usuario admin, String ip) {
        validarRolAdmin(admin);
        String nombre;

        if (CATALOGO_DEPARTAMENTOS.equals(catalogo)) {
            Departamento departamento = buscarDepartamento(id);
            if (departamentoEnUso(id)) {
                throw new NegocioException(CodigoError.ELEMENTO_EN_USO);
            }
            nombre = departamento.getNombre();
            departamentoRepository.delete(departamento);

        } else if (CATALOGO_CATEGORIAS.equals(catalogo)) {
            Categoria categoria = categoriaRepository.findById(id)
                    .orElseThrow(() -> new NegocioException(CodigoError.VALIDACION, "La categoría no existe"));
            if (casoRepository.countByCategoriaId(id) > 0) {
                throw new NegocioException(CodigoError.ELEMENTO_EN_USO);
            }
            nombre = categoria.getNombre();
            categoriaRepository.delete(categoria);

        } else if (CATALOGO_MOTIVOS.equals(catalogo)) {
            MotivoRechazo motivo = motivoRechazoRepository.findById(id)
                    .orElseThrow(() -> new NegocioException(CodigoError.VALIDACION, "El motivo no existe"));
            if (casoRepository.countByMotivoRechazoId(id) > 0) {
                throw new NegocioException(CodigoError.ELEMENTO_EN_USO);
            }
            nombre = motivo.getMotivo();
            motivoRechazoRepository.delete(motivo);

        } else {
            throw new NegocioException(CodigoError.VALIDACION, "Catálogo no válido");
        }

        registrarEnBitacora(admin, ip, "Eliminación en catálogo", catalogo, nombre);
        return new DtoMensaje(MENSAJE_CATALOGO_ACTUALIZADO);
    }

    // ---------- Tipos de caso que puede atender una categoría (RN03) ----------
    @Transactional(readOnly = true)
    public List<String> listarTiposCaso(Usuario admin) {
        validarRolAdmin(admin);

        List<String> tipos = new ArrayList<>();
        tipos.add(TipoCaso.QUEJA);
        tipos.add(TipoCaso.RECLAMO);
        tipos.add(TipoCaso.DENUNCIA);
        tipos.add(TipoCaso.SUGERENCIA);
        return tipos;
    }

    // ---------- Métodos de apoyo ----------
    // Un departamento está en uso si tiene categorías, usuarios o casos
    private boolean departamentoEnUso(Long id) {
        return categoriaRepository.countByDepartamentoId(id) > 0
                || usuarioRepository.countByIdDepartamentoId(id) > 0
                || casoRepository.countByDepartamentoId(id) > 0;
    }

    private String validarNombre(DtoGuardarCatalogo dto) {
        if (dto == null || dto.nombre() == null || dto.nombre().isBlank()) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        String nombre = dto.nombre().trim();
        if (nombre.length() > MAXIMO_NOMBRE) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        return nombre;
    }

    private boolean activoDe(DtoGuardarCatalogo dto) {
        return dto.activo() == null || Boolean.TRUE.equals(dto.activo());
    }

    private Set<String> validarTiposCaso(List<String> tiposCaso) {
        Set<String> resultado = new HashSet<>();
        if (tiposCaso == null) {
            return resultado;
        }
        for (int i = 0; i < tiposCaso.size(); i++) {
            String tipo = tiposCaso.get(i);
            if (!TipoCaso.esValido(tipo)) {
                throw new NegocioException(CodigoError.TIPO_CASO_REQUERIDO);
            }
            resultado.add(tipo);
        }
        return resultado;
    }

    private Departamento buscarDepartamento(Long id) {
        if (id == null) {
            throw new NegocioException(CodigoError.CAMPOS_OBLIGATORIOS);
        }
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new NegocioException(CodigoError.VALIDACION, "El departamento no existe"));
    }

    private void registrarEnBitacora(Usuario admin, String ip, String accion, String catalogo, String nombre) {
        bitacoraCambioService.registrar(
                accion,
                admin.getCui(),
                null,
                ip,
                "Catálogo " + catalogo + ": " + nombre
        );
    }

    // getAuthorities() siempre devuelve ROLE_USER, así que el rol se valida aquí
    private void validarRolAdmin(Usuario admin) {
        if (admin == null || admin.getIdRol() == null || !ROL_ADMIN.equals(admin.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }
}
