package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoCampanaGauss;
import gt.muni.quejas.dto.DtoCategoria2;
import gt.muni.quejas.dto.DtoDispersionTecnico;
import gt.muni.quejas.dto.DtoFiltros2;
import gt.muni.quejas.dto.DtoOpcionCatalogo;
import gt.muni.quejas.dto.DtoValorEtiqueta;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Categoria;
import gt.muni.quejas.model.Departamento;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.CategoriaRepository;
import gt.muni.quejas.repository.Dashboard2Repository;
import gt.muni.quejas.repository.DepartamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard 2: cuatro graficas sencillas, exclusivas del Admin.
 *
 * Cada metodo hace lo mismo: valida, llama a su consulta y pasa las filas a una
 * lista de DTO. Toda la cuenta la hace la base de datos, aqui no se calcula nada.
 */
@Service
public class Dashboard2Service {

    private static final String ROL_ADMIN = "Admin";
    private static final int MAXIMO_DIAS_RANGO = 1830;

    private final Dashboard2Repository dashboard2Repository;
    private final DepartamentoRepository departamentoRepository;
    private final CategoriaRepository categoriaRepository;

    public Dashboard2Service(Dashboard2Repository dashboard2Repository,
                             DepartamentoRepository departamentoRepository,
                             CategoriaRepository categoriaRepository) {
        this.dashboard2Repository = dashboard2Repository;
        this.departamentoRepository = departamentoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // ---------- Lo que llena los selectores de la pantalla ----------
    @Transactional(readOnly = true)
    public DtoFiltros2 filtros(Usuario admin) {
        validarAdmin(admin);

        List<DtoOpcionCatalogo> departamentos = new ArrayList<>();
        List<Departamento> todos = departamentoRepository.findAll();
        for (int i = 0; i < todos.size(); i++) {
            departamentos.add(new DtoOpcionCatalogo(todos.get(i).getId(), todos.get(i).getNombre()));
        }

        // Cada categoria lleva su departamento y los tipos de caso que admite,
        // para que la pantalla muestre solo las que corresponden a lo elegido
        List<DtoCategoria2> categorias = new ArrayList<>();
        List<Categoria> activas = categoriaRepository.findActivasConDepartamento();
        for (int i = 0; i < activas.size(); i++) {
            Categoria categoria = activas.get(i);
            categorias.add(new DtoCategoria2(
                    categoria.getId(),
                    categoria.getNombre(),
                    categoria.getDepartamento().getId(),
                    new ArrayList<>(categoria.getTiposCaso())));
        }

        List<String> tipos = List.of("Queja", "Reclamo", "Denuncia", "Sugerencia");

        return new DtoFiltros2(departamentos, tipos, categorias);
    }

    // ---------- 1. Barras verticales: casos por estado ----------
    @Transactional(readOnly = true)
    public List<DtoValorEtiqueta> casosPorEstado(LocalDate desde, LocalDate hasta,
                                                 Long idDepartamento, String tipoCaso,
                                                 Long idCategoria, Usuario admin) {
        validar(admin, desde, hasta);

        List<Dashboard2Repository.FilaValorEtiqueta> filas = dashboard2Repository.casosPorEstado(
                desde, hasta, idDepartamento, vacio(tipoCaso), idCategoria);

        return aListaDeValores(filas);
    }

    // ---------- 2. Pastel: casos por tipo de caso ----------
    @Transactional(readOnly = true)
    public List<DtoValorEtiqueta> casosPorTipo(LocalDate desde, LocalDate hasta,
                                               Long idDepartamento, String tipoCaso,
                                               Long idCategoria, Usuario admin) {
        validar(admin, desde, hasta);

        List<Dashboard2Repository.FilaValorEtiqueta> filas = dashboard2Repository.casosPorTipo(
                desde, hasta, idDepartamento, vacio(tipoCaso), idCategoria);

        return aListaDeValores(filas);
    }

    // ---------- 3. Dispersion: casos del Tecnico contra sus dias promedio ----------
    @Transactional(readOnly = true)
    public List<DtoDispersionTecnico> casosPorTecnico(LocalDate desde, LocalDate hasta,
                                                      Long idDepartamento, String tipoCaso,
                                                      Long idCategoria, Usuario admin) {
        validar(admin, desde, hasta);

        List<Dashboard2Repository.FilaDispersion> filas = dashboard2Repository.casosPorTecnico(
                desde, hasta, idDepartamento, vacio(tipoCaso), idCategoria);

        List<DtoDispersionTecnico> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            Dashboard2Repository.FilaDispersion fila = filas.get(i);
            resultado.add(new DtoDispersionTecnico(
                    fila.getTecnico(),
                    fila.getCantidad(),
                    fila.getPromedio()));
        }
        return resultado;
    }

    // ---------- 4. Campana de Gauss: dias de resolucion ----------
    @Transactional(readOnly = true)
    public DtoCampanaGauss campanaDeGauss(LocalDate desde, LocalDate hasta,
                                          Long idDepartamento, String tipoCaso,
                                          Long idCategoria, Usuario admin) {
        validar(admin, desde, hasta);

        // Las barras salen de una consulta y la media y la desviacion de otra.
        // La curva normal no se calcula aqui: la pinta el componente con esos dos numeros.
        List<Dashboard2Repository.FilaValorEtiqueta> filas = dashboard2Repository.diasDeResolucionPorTramo(
                desde, hasta, idDepartamento, vacio(tipoCaso), idCategoria);

        Dashboard2Repository.FilaResumen resumen = dashboard2Repository.mediaYDesviacion(
                desde, hasta, idDepartamento, vacio(tipoCaso), idCategoria);

        double media = resumen == null || resumen.getMedia() == null ? 0 : resumen.getMedia();
        double desviacion = resumen == null || resumen.getDesviacion() == null ? 0 : resumen.getDesviacion();

        return new DtoCampanaGauss(aListaDeValores(filas), media, desviacion);
    }

    // ---------- Metodos de apoyo ----------

    // Las filas que devuelve la consulta se pasan a una lista de DTO
    private List<DtoValorEtiqueta> aListaDeValores(List<Dashboard2Repository.FilaValorEtiqueta> filas) {
        List<DtoValorEtiqueta> resultado = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            resultado.add(new DtoValorEtiqueta(
                    filas.get(i).getEtiqueta(),
                    filas.get(i).getValor()));
        }
        return resultado;
    }

    // Un filtro de texto vacio es lo mismo que no filtrar, y en la consulta eso es NULL
    private String vacio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    // getAuthorities() siempre devuelve ROLE_USER, asi que el rol se valida aqui
    private void validarAdmin(Usuario admin) {
        if (admin == null || admin.getIdRol() == null || !ROL_ADMIN.equals(admin.getIdRol().getRol())) {
            throw new NegocioException(CodigoError.ACCESO_DENEGADO);
        }
    }

    // AN02 # 30: el rango de fechas debe ser valido
    private void validar(Usuario admin, LocalDate desde, LocalDate hasta) {
        validarAdmin(admin);
        if (desde == null || hasta == null || hasta.isBefore(desde)
                || desde.plusDays(MAXIMO_DIAS_RANGO).isBefore(hasta)) {
            throw new NegocioException(CodigoError.RANGO_FECHAS_INVALIDO);
        }
    }
}
