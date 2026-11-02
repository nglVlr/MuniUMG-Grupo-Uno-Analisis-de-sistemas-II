import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgxEchartsDirective } from 'ngx-echarts';
import { AuthService } from '../services/auth.service';
import {
  CatalogosDashboard,
  CategoriaDashboard,
  DashboardService,
  FiltrosDashboard,
  IndicadorTarjeta,
  RespuestaDashboard,
  UsuarioDashboard
} from '../core/dashboard.service';
import {
  ESCALA_SECUENCIAL,
  NOMBRE_TEMA,
  PALETA_PASTEL,
  SEMAFORO_AMARILLO,
  SEMAFORO_ROJO,
  SEMAFORO_VERDE,
  TINTA,
  registrarTemaPastel
} from './tema-pastel';

// Un tipo de gráfica del catálogo. forma es la estructura de datos que acepta:
// categoria, series, temporal, propio o indicador. lienzo indica si se dibuja con
// ECharts, porque las tarjetas y la tabla son HTML.
interface TipoGrafica {
  id: string;
  nombre: string;
  forma: string;
  lienzo: boolean;
}

interface ConfiguracionDashboard {
  // Versión del significado de las casillas. Antes una casilla marcada quería decir
  // "mostrar esta gráfica"; ahora quiere decir "agregar esta gráfica debajo de la
  // elegida". Una configuración guardada con otra versión se descarta, porque sus
  // marcas significaban otra cosa.
  version: number;
  graficas: { [id: string]: boolean };
  filtros: { [id: string]: boolean };
  // La gráfica que el Admin eligió la última vez
  tipoGrafica?: string;
}

// CU-19 Dashboard de gráficas: pantalla aparte, exclusiva del Admin.
@Component({
  selector: 'app-dashboard-graficas',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxEchartsDirective],
  templateUrl: './dashboard-graficas.component.html',
  styleUrl: './dashboard-graficas.component.css'
})
export class DashboardGraficasComponent implements OnInit {

  readonly tema = NOMBRE_TEMA;
  readonly CLAVE_CONFIGURACION = 'dashboardConfiguracion';
  readonly VERSION_CONFIGURACION = 2;

  // ---------- Catálogo de métricas ----------
  readonly metricas = [
    { id: 'casos', nombre: 'Conteo de casos' },
    { id: 'casosResueltos', nombre: 'Casos resueltos' },
    { id: 'porcentajeResolucion', nombre: 'Porcentaje de resolución' },
    { id: 'tiempoPromedioResolucion', nombre: 'Tiempo promedio de resolución' },
    { id: 'accionesBitacora', nombre: 'Acciones de bitácora' },
    { id: 'inicioSesionExitoso', nombre: 'Inicios de sesión exitosos' },
    { id: 'inicioSesionFallido', nombre: 'Inicios de sesión fallidos' },
    { id: 'erroresNotificacion', nombre: 'Errores de envío de notificaciones' }
  ];

  readonly agrupaciones = [
    { id: 'estado', nombre: 'Estado' },
    { id: 'tipo', nombre: 'Tipo de caso' },
    { id: 'departamento', nombre: 'Departamento' },
    { id: 'categoria', nombre: 'Categoría' },
    { id: 'tecnico', nombre: 'Técnico' },
    { id: 'cargo', nombre: 'Cargo' },
    { id: 'usuario', nombre: 'Usuario' },
    { id: 'ciudadano', nombre: 'Ciudadano' },
    { id: 'motivoRechazo', nombre: 'Motivo de rechazo' },
    { id: 'dia', nombre: 'Día' },
    { id: 'semana', nombre: 'Semana' },
    { id: 'mes', nombre: 'Mes' },
    { id: 'diaSemana', nombre: 'Día de la semana' },
    { id: 'hora', nombre: 'Hora' },
    { id: 'accion', nombre: 'Acción' }
  ];

  // Agrupaciones que acepta cada métrica
  readonly agrupacionesCaso = ['estado', 'tipo', 'departamento', 'categoria', 'tecnico',
    'cargo', 'ciudadano', 'motivoRechazo', 'dia', 'semana', 'mes', 'diaSemana', 'hora'];
  readonly agrupacionesAcciones = ['accion', 'usuario', 'cargo', 'departamento', 'estado',
    'dia', 'semana', 'mes', 'diaSemana', 'hora'];
  readonly agrupacionesSesion = ['usuario', 'cargo', 'departamento',
    'dia', 'semana', 'mes', 'diaSemana', 'hora'];

  // ---------- Catálogo de gráficas ----------
  readonly graficas: TipoGrafica[] = [
    { id: 'barras', nombre: 'Barras verticales', forma: 'categoria', lienzo: true },
    { id: 'barrasHorizontal', nombre: 'Barras horizontales', forma: 'categoria', lienzo: true },
    { id: 'barrasAgrupadas', nombre: 'Barras agrupadas', forma: 'series', lienzo: true },
    { id: 'barrasApiladas', nombre: 'Barras apiladas', forma: 'series', lienzo: true },
    { id: 'barrasApiladas100', nombre: 'Barras apiladas al 100%', forma: 'series', lienzo: true },
    { id: 'lollipop', nombre: 'Lollipop', forma: 'categoria', lienzo: true },
    { id: 'radar', nombre: 'Radar', forma: 'categoria', lienzo: true },
    { id: 'pareto', nombre: 'Pareto de motivos de rechazo', forma: 'propio', lienzo: true },
    { id: 'linea', nombre: 'Línea', forma: 'temporal', lienzo: true },
    { id: 'lineasMultiples', nombre: 'Líneas múltiples', forma: 'temporal', lienzo: true },
    { id: 'area', nombre: 'Área', forma: 'temporal', lienzo: true },
    { id: 'areaApilada', nombre: 'Área apilada', forma: 'temporal', lienzo: true },
    { id: 'escalonada', nombre: 'Escalonada de casos abiertos', forma: 'propio', lienzo: true },
    { id: 'combinada', nombre: 'Barras más línea por mes', forma: 'propio', lienzo: true },
    { id: 'cascada', nombre: 'Cascada', forma: 'propio', lienzo: true },
    { id: 'calendario', nombre: 'Calendario', forma: 'propio', lienzo: true },
    { id: 'pastel', nombre: 'Pastel', forma: 'categoria', lienzo: true },
    { id: 'dona', nombre: 'Dona', forma: 'categoria', lienzo: true },
    { id: 'semidona', nombre: 'Semidona', forma: 'categoria', lienzo: true },
    { id: 'treemap', nombre: 'Treemap de departamento y categoría', forma: 'propio', lienzo: true },
    { id: 'sunburst', nombre: 'Sunburst de departamento, tipo y estado', forma: 'propio', lienzo: true },
    { id: 'waffle', nombre: 'Waffle', forma: 'categoria', lienzo: true },
    { id: 'histograma', nombre: 'Histograma de días de resolución', forma: 'propio', lienzo: true },
    { id: 'campanaGauss', nombre: 'Campana de Gauss de días de resolución', forma: 'propio', lienzo: true },
    { id: 'boxplot', nombre: 'Boxplot de tiempos por Técnico', forma: 'propio', lienzo: true },
    { id: 'dispersion', nombre: 'Dispersión de carga y tiempo', forma: 'propio', lienzo: true },
    { id: 'burbujas', nombre: 'Burbujas de carga y tiempo', forma: 'propio', lienzo: true },
    { id: 'calorDiaHora', nombre: 'Mapa de calor día de semana por hora', forma: 'propio', lienzo: true },
    { id: 'calorCategoriaMes', nombre: 'Mapa de calor categoría por mes', forma: 'propio', lienzo: true },
    { id: 'embudo', nombre: 'Embudo de Registrado a Resuelto', forma: 'propio', lienzo: true },
    { id: 'sankey', nombre: 'Sankey de transiciones de estado', forma: 'propio', lienzo: true },
    { id: 'trazabilidad', nombre: 'Línea de tiempo de un caso', forma: 'propio', lienzo: true },
    { id: 'arbol', nombre: 'Árbol de departamento, roles y usuarios', forma: 'propio', lienzo: true },
    { id: 'gauge', nombre: 'Gauge de porcentaje de resolución', forma: 'indicador', lienzo: true },
    { id: 'bullet', nombre: 'Bullet de porcentaje de resolución', forma: 'indicador', lienzo: true },
    { id: 'termometro', nombre: 'Termómetro de porcentaje de resolución', forma: 'indicador', lienzo: true },
    { id: 'tarjetas', nombre: 'Tarjetas con variación', forma: 'indicador', lienzo: false },
    { id: 'sparklines', nombre: 'Sparklines en las tarjetas', forma: 'indicador', lienzo: false },
    { id: 'tablaTecnicos', nombre: 'Tabla de Técnicos con semáforo', forma: 'propio', lienzo: false }
  ];

  // Los que quedan en el perfil "Solo barras"
  readonly perfilSoloBarras = ['barras', 'barrasHorizontal', 'barrasAgrupadas',
    'barrasApiladas', 'barrasApiladas100', 'lollipop', 'tarjetas', 'sparklines'];

  // ---------- Catálogo de filtros ----------
  readonly listaFiltros = [
    { id: 'rango', nombre: 'Rango de fechas' },
    { id: 'departamento', nombre: 'Departamento' },
    { id: 'tipoCaso', nombre: 'Tipo de caso' },
    { id: 'categoria', nombre: 'Categoría' },
    { id: 'estado', nombre: 'Estado actual' },
    { id: 'tecnico', nombre: 'Técnico' },
    { id: 'motivoRechazo', nombre: 'Motivo de rechazo' },
    { id: 'cargo', nombre: 'Cargo' },
    { id: 'usuario', nombre: 'Usuario' },
    { id: 'ciudadano', nombre: 'Ciudadano por DPI' }
  ];

  // Las gráficas de actividad del sistema no pertenecen a un caso
  readonly filtrosDeSistema = ['rango', 'departamento', 'cargo', 'usuario'];

  metrica = 'casos';
  agrupacion = 'estado';
  tipoGrafica = 'barras';

  filtros: FiltrosDashboard = {
    modoFecha: 'registro',
    desde: '',
    hasta: '',
    idDepartamento: null,
    tipoCaso: '',
    idCategoria: null,
    estado: '',
    cuiTecnico: '',
    idMotivoRechazo: null,
    cargo: '',
    cuiUsuario: '',
    dpiCiudadano: '',
    codigoSeguimiento: ''
  };

  catalogos: CatalogosDashboard = {
    departamentos: [], categorias: [], tiposCaso: [], estados: [],
    tecnicos: [], usuarios: [], motivosRechazo: [], cargos: []
  };

  respuesta: RespuestaDashboard | null = null;
  configuracion: ConfiguracionDashboard = this.configuracionPorDefecto();
  mostrarConfiguracion = false;

  // Las opciones de cada gráfica se arman una sola vez por consulta. Si la plantilla
  // llamara al método en cada ligadura, Angular las volvería a armar en cada ciclo
  // de detección de cambios y ECharts redibujaría todos los lienzos cada vez.
  opcionesCalculadas: { [id: string]: any } = {};
  chispas: { [clave: string]: any } = {};

  nombreUsuario = '';
  mensaje = '';
  cargando = false;

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService,
    private router: Router
  ) {
    registrarTemaPastel();
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  ngOnInit(): void {
    // Por defecto, los últimos 12 meses
    const hoy = new Date();
    this.filtros.hasta = this.comoTexto(hoy);
    this.filtros.desde = this.comoTexto(
      new Date(hoy.getFullYear() - 1, hoy.getMonth(), hoy.getDate()));

    this.leerConfiguracion();
    this.cargarCatalogos();
    this.consultar();
  }

  volverAlMenu(): void {
    this.router.navigate(['/bandeja-admin']);
  }

  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  private cargarCatalogos(): void {
    this.dashboardService.catalogos().subscribe({
      next: (resultado) => {
        this.catalogos = resultado;
      },
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No tiene permisos para acceder a este recurso');
      }
    });
  }

  // ---------- Consulta ----------
  consultar(): void {
    this.mensaje = '';
    this.cargando = true;
    this.limpiarFiltrosQueNoAplican();

    this.dashboardService
      .consultar(this.metrica, this.agrupacion, this.filtros, this.filtrosAEnviar())
      .subscribe({
        next: (resultado) => {
          this.cargando = false;
          this.respuesta = resultado;

          // AN02 # 31: no existen registros para los filtros aplicados
          if (resultado.sinResultados) {
            this.mensaje = 'No existen registros para los filtros aplicados.';
          }
          // El selector nunca deja una gráfica incompatible seleccionada
          if (!this.disponible(this.tipoGrafica)) {
            this.tipoGrafica = this.primeraDisponible();
          }
          this.recalcularOpciones();
        },
        error: (error) => {
          this.cargando = false;
          this.respuesta = null;
          this.opcionesCalculadas = {};
          this.chispas = {};
          this.mensaje = this.mensajeError(error, 'El rango de fechas ingresado no es válido.');
        }
      });
  }

  // El Admin elige qué gráfica quiere ver con los filtros que tiene puestos,
  // y la elección se recuerda para la próxima vez que entre
  cambiarTipoGrafica(): void {
    this.guardarConfiguracion();
    this.recalcularOpciones();
  }

  cambiarMetrica(): void {
    // Si la agrupación no aplica a la métrica nueva, se mueve a la primera que sí
    if (this.agrupacionesPermitidas().indexOf(this.agrupacion) < 0) {
      this.agrupacion = this.agrupacionesPermitidas()[0];
    }
    this.consultar();
  }

  limpiarFiltros(): void {
    this.filtros.modoFecha = 'registro';
    this.filtros.idDepartamento = null;
    this.filtros.tipoCaso = '';
    this.filtros.idCategoria = null;
    this.filtros.estado = '';
    this.filtros.cuiTecnico = '';
    this.filtros.idMotivoRechazo = null;
    this.filtros.cargo = '';
    this.filtros.cuiUsuario = '';
    this.filtros.dpiCiudadano = '';
    this.filtros.codigoSeguimiento = '';
    this.consultar();
  }

  agrupacionesPermitidas(): string[] {
    if (this.metrica === 'accionesBitacora') {
      return this.agrupacionesAcciones;
    }
    if (this.esMetricaSistema()) {
      return this.agrupacionesSesion;
    }
    return this.agrupacionesCaso;
  }

  agrupacionPermitida(id: string): boolean {
    return this.agrupacionesPermitidas().indexOf(id) >= 0;
  }

  esMetricaSistema(): boolean {
    return this.metrica === 'accionesBitacora' || this.metrica === 'inicioSesionExitoso'
        || this.metrica === 'inicioSesionFallido' || this.metrica === 'erroresNotificacion';
  }

  // ---------- Filtros: visibilidad y aplicabilidad ----------
  filtroVisible(id: string): boolean {
    return this.configuracion.filtros[id] !== false;
  }

  // Los filtros de caso no aplican a las gráficas de actividad del sistema
  filtroAplica(id: string): boolean {
    if (!this.esMetricaSistema()) {
      return true;
    }
    return this.filtrosDeSistema.indexOf(id) >= 0;
  }

  // Un filtro oculto se limpia y no se envía al backend
  private filtrosAEnviar(): string[] {
    const ids: string[] = [];
    for (let i = 0; i < this.listaFiltros.length; i++) {
      const id = this.listaFiltros[i].id;
      if (this.filtroVisible(id) && this.filtroAplica(id)) {
        ids.push(id);
      }
    }
    return ids;
  }

  private limpiarFiltrosQueNoAplican(): void {
    for (let i = 0; i < this.listaFiltros.length; i++) {
      const id = this.listaFiltros[i].id;
      if (this.filtroVisible(id) && this.filtroAplica(id)) {
        continue;
      }
      if (id === 'departamento') { this.filtros.idDepartamento = null; }
      if (id === 'tipoCaso') { this.filtros.tipoCaso = ''; }
      if (id === 'categoria') { this.filtros.idCategoria = null; }
      if (id === 'estado') { this.filtros.estado = ''; }
      if (id === 'tecnico') { this.filtros.cuiTecnico = ''; }
      if (id === 'motivoRechazo') { this.filtros.idMotivoRechazo = null; }
      if (id === 'cargo') { this.filtros.cargo = ''; }
      if (id === 'usuario') { this.filtros.cuiUsuario = ''; }
      if (id === 'ciudadano') { this.filtros.dpiCiudadano = ''; }
    }
  }

  // La categoría depende del departamento y del tipo de caso
  categoriasFiltradas(): CategoriaDashboard[] {
    const resultado: CategoriaDashboard[] = [];
    for (let i = 0; i < this.catalogos.categorias.length; i++) {
      const categoria = this.catalogos.categorias[i];
      if (this.filtros.idDepartamento
          && categoria.idDepartamento !== Number(this.filtros.idDepartamento)) {
        continue;
      }
      if (this.filtros.tipoCaso && categoria.tiposCaso.indexOf(this.filtros.tipoCaso) < 0) {
        continue;
      }
      resultado.push(categoria);
    }
    return resultado;
  }

  // El Técnico depende del departamento
  tecnicosFiltrados(): UsuarioDashboard[] {
    const resultado: UsuarioDashboard[] = [];
    for (let i = 0; i < this.catalogos.tecnicos.length; i++) {
      const tecnico = this.catalogos.tecnicos[i];
      if (this.filtros.idDepartamento
          && tecnico.idDepartamento !== Number(this.filtros.idDepartamento)) {
        continue;
      }
      resultado.push(tecnico);
    }
    return resultado;
  }

  cambiarDepartamento(): void {
    // La categoría y el Técnico elegidos pueden ya no pertenecer al departamento
    this.filtros.idCategoria = null;
    this.filtros.cuiTecnico = '';
    this.consultar();
  }

  cambiarTipoCaso(): void {
    this.filtros.idCategoria = null;
    this.consultar();
  }

  // ---------- Configuración de la pantalla ----------
  // Las casillas del catálogo solo deciden qué gráficas se agregan debajo de la que
  // el Admin eligió. La gráfica del selector siempre se dibuja, aunque su casilla
  // esté apagada, porque es la que pidió ver.
  private configuracionTodo(): ConfiguracionDashboard {
    const configuracion: ConfiguracionDashboard = {
      version: this.VERSION_CONFIGURACION,
      graficas: {},
      filtros: {}
    };
    for (let i = 0; i < this.graficas.length; i++) {
      configuracion.graficas[this.graficas[i].id] = true;
    }
    for (let i = 0; i < this.listaFiltros.length; i++) {
      configuracion.filtros[this.listaFiltros[i].id] = true;
    }
    return configuracion;
  }

  // Al entrar solo se ven las tarjetas y la gráfica elegida: el catálogo arranca vacío
  private configuracionPorDefecto(): ConfiguracionDashboard {
    const configuracion = this.configuracionTodo();
    for (let i = 0; i < this.graficas.length; i++) {
      const id = this.graficas[i].id;
      configuracion.graficas[id] = id === 'tarjetas' || id === 'sparklines';
    }
    return configuracion;
  }

  private leerConfiguracion(): void {
    const guardado = localStorage.getItem(this.CLAVE_CONFIGURACION);
    if (!guardado) {
      return;
    }
    try {
      const leido = JSON.parse(guardado) as ConfiguracionDashboard;

      // Lo guardado por una versión anterior de la pantalla ya no significa lo mismo
      if (leido.version !== this.VERSION_CONFIGURACION) {
        this.configuracion = this.configuracionPorDefecto();
        this.guardarConfiguracion();
        return;
      }

      const base = this.configuracionPorDefecto();
      for (let i = 0; i < this.graficas.length; i++) {
        const id = this.graficas[i].id;
        if (leido.graficas && leido.graficas[id] !== undefined) {
          base.graficas[id] = leido.graficas[id];
        }
      }
      for (let i = 0; i < this.listaFiltros.length; i++) {
        const id = this.listaFiltros[i].id;
        if (leido.filtros && leido.filtros[id] === false) {
          base.filtros[id] = false;
        }
      }
      this.configuracion = base;
      if (leido.tipoGrafica) {
        this.tipoGrafica = leido.tipoGrafica;
      }
    } catch {
      this.configuracion = this.configuracionPorDefecto();
    }
  }

  private guardarConfiguracion(): void {
    this.configuracion.tipoGrafica = this.tipoGrafica;
    localStorage.setItem(this.CLAVE_CONFIGURACION, JSON.stringify(this.configuracion));
  }

  alternarGrafica(id: string): void {
    this.configuracion.graficas[id] = !this.configuracion.graficas[id];
    this.guardarConfiguracion();
    this.recalcularOpciones();
  }

  alternarFiltro(id: string): void {
    if (id === 'rango') {
      // El rango de fechas es obligatorio para la consulta
      return;
    }
    this.configuracion.filtros[id] = !this.configuracion.filtros[id];
    this.guardarConfiguracion();
    this.consultar();
  }

  aplicarPerfil(perfil: string): void {
    if (perfil === 'elegida') {
      this.configuracion = this.configuracionPorDefecto();
    } else {
      this.configuracion = this.configuracionTodo();
      if (perfil === 'barras') {
        for (let i = 0; i < this.graficas.length; i++) {
          const id = this.graficas[i].id;
          this.configuracion.graficas[id] = this.perfilSoloBarras.indexOf(id) >= 0;
        }
      }
    }
    this.guardarConfiguracion();
    this.consultar();
  }

  restaurarConfiguracion(): void {
    localStorage.removeItem(this.CLAVE_CONFIGURACION);
    this.configuracion = this.configuracionPorDefecto();
    this.consultar();
  }

  graficaVisible(id: string): boolean {
    return this.configuracion.graficas[id] !== false;
  }

  // Se dibuja solo lo que está visible y además es compatible con los datos
  mostrarGrafica(id: string): boolean {
    return this.graficaVisible(id) && this.disponible(id);
  }

  // En el catálogo no se repite la gráfica elegida: ya se ve grande arriba
  mostrarEnCatalogo(id: string): boolean {
    return id !== this.tipoGrafica && this.mostrarGrafica(id);
  }

  private primeraDisponible(): string {
    for (let i = 0; i < this.graficas.length; i++) {
      const grafica = this.graficas[i];
      if (grafica.lienzo && this.disponible(grafica.id)) {
        return grafica.id;
      }
    }
    return 'barras';
  }

  // Cuántos paneles del catálogo quedaron encendidos y además se pueden dibujar.
  // Las tarjetas y las sparklines no cuentan: viven arriba, no en el catálogo.
  graficasDelCatalogo(): number {
    let total = 0;
    for (let i = 0; i < this.graficas.length; i++) {
      const grafica = this.graficas[i];
      if (!grafica.lienzo && grafica.id !== 'tablaTecnicos') {
        continue;
      }
      if (this.mostrarEnCatalogo(grafica.id)) {
        total = total + 1;
      }
    }
    return total;
  }

  graficasDelSelector(): TipoGrafica[] {
    const resultado: TipoGrafica[] = [];
    for (let i = 0; i < this.graficas.length; i++) {
      if (this.graficas[i].lienzo) {
        resultado.push(this.graficas[i]);
      }
    }
    return resultado;
  }

  nombreGrafica(id: string): string {
    for (let i = 0; i < this.graficas.length; i++) {
      if (this.graficas[i].id === id) {
        return this.graficas[i].nombre;
      }
    }
    return id;
  }

  // ---------- Compatibilidad de cada gráfica con el resultado ----------
  disponible(id: string): boolean {
    const datos = this.respuesta;
    if (!datos) {
      return false;
    }
    // Las métricas de porcentaje y de tiempo promedio no se pueden sumar,
    // así que no se ofrecen en gráficas de proporción ni apiladas
    const aditiva = datos.formato === 'entero';
    const hayCategorias = datos.categorias.length > 0;
    const haySeries = datos.seriesDesglosadas.length > 0;
    const esTemporal = datos.forma === 'temporal';

    if (id === 'barras' || id === 'barrasHorizontal' || id === 'lollipop') {
      return hayCategorias;
    }
    if (id === 'radar') {
      return datos.categorias.length >= 3;
    }
    if (id === 'pastel' || id === 'dona' || id === 'semidona' || id === 'waffle') {
      return hayCategorias && aditiva;
    }
    if (id === 'barrasAgrupadas' || id === 'barrasApiladas' || id === 'barrasApiladas100') {
      return haySeries && aditiva;
    }
    if (id === 'linea' || id === 'area') {
      return hayCategorias && esTemporal;
    }
    if (id === 'lineasMultiples' || id === 'areaApilada') {
      return haySeries && aditiva && esTemporal;
    }
    if (id === 'pareto') {
      return datos.pareto.length > 0;
    }
    if (id === 'escalonada') {
      return datos.acumuladoAbiertos.length > 0;
    }
    if (id === 'combinada') {
      return datos.mesesCombinada.length > 0;
    }
    if (id === 'cascada') {
      return datos.totalCasos > 0;
    }
    if (id === 'calendario') {
      return datos.calendario.length > 0;
    }
    if (id === 'treemap') {
      return datos.treemap.length > 0;
    }
    if (id === 'sunburst') {
      return datos.sunburst.length > 0;
    }
    if (id === 'arbol') {
      return datos.arbol.length > 0;
    }
    if (id === 'histograma') {
      return this.sumaConteos(datos.histograma) > 0;
    }
    if (id === 'campanaGauss') {
      return datos.distribucionDias.length > 1 && datos.desviacionDias > 0;
    }
    if (id === 'boxplot') {
      return datos.boxplot.length > 0;
    }
    if (id === 'dispersion' || id === 'burbujas') {
      return datos.dispersion.length > 0;
    }
    if (id === 'calorDiaHora') {
      return datos.calorDiaHora && datos.calorDiaHora.celdas.length > 0;
    }
    if (id === 'calorCategoriaMes') {
      return datos.calorCategoriaMes && datos.calorCategoriaMes.celdas.length > 0;
    }
    if (id === 'embudo') {
      return this.sumaConteos(datos.embudo) > 0;
    }
    if (id === 'sankey') {
      return datos.sankey.length > 0;
    }
    if (id === 'trazabilidad') {
      return datos.trazabilidad.length > 0;
    }
    if (id === 'tablaTecnicos') {
      return datos.tablaTecnicos.length > 0;
    }
    // gauge, bullet, termometro, tarjetas y sparklines
    return datos.indicadores.length > 0;
  }

  // Motivo por el que una gráfica no está disponible, para el selector
  razonNoDisponible(id: string): string {
    const datos = this.respuesta;
    if (!datos) {
      return 'sin datos';
    }
    if (datos.formato !== 'entero'
        && (id === 'pastel' || id === 'dona' || id === 'semidona' || id === 'waffle'
            || id === 'barrasApiladas' || id === 'barrasApiladas100'
            || id === 'barrasAgrupadas' || id === 'lineasMultiples' || id === 'areaApilada')) {
      return 'no aplica a esta métrica';
    }
    if (datos.forma !== 'temporal'
        && (id === 'linea' || id === 'area' || id === 'lineasMultiples' || id === 'areaApilada')) {
      return 'requiere una agrupación de tiempo';
    }
    return 'sin datos';
  }

  private sumaConteos(conteos: { cantidad: number }[]): number {
    let total = 0;
    for (let i = 0; i < conteos.length; i++) {
      total = total + conteos[i].cantidad;
    }
    return total;
  }

  // ---------- Indicadores ----------
  indicadores(): IndicadorTarjeta[] {
    return this.respuesta ? this.respuesta.indicadores : [];
  }

  valorIndicador(clave: string): number {
    const tarjetas = this.indicadores();
    for (let i = 0; i < tarjetas.length; i++) {
      if (tarjetas[i].clave === clave) {
        return tarjetas[i].valor;
      }
    }
    return 0;
  }

  textoValor(valor: number, formato: string): string {
    if (formato === 'porcentaje') {
      return valor + ' %';
    }
    if (formato === 'dias') {
      return valor + ' días';
    }
    return String(valor);
  }

  colorSemaforo(semaforo: string): string {
    if (semaforo === 'verde') {
      return SEMAFORO_VERDE;
    }
    if (semaforo === 'rojo') {
      return SEMAFORO_ROJO;
    }
    return SEMAFORO_AMARILLO;
  }

  // ---------- Opciones de ECharts ----------
  // Solo se arman las gráficas que están visibles y que aceptan la forma de los datos
  private recalcularOpciones(): void {
    const calculadas: { [id: string]: any } = {};
    for (let i = 0; i < this.graficas.length; i++) {
      const grafica = this.graficas[i];
      if (grafica.lienzo && this.mostrarGrafica(grafica.id)) {
        calculadas[grafica.id] = this.opcion(grafica.id);
      }
    }
    // La gráfica elegida se dibuja aunque su casilla del catálogo esté apagada
    if (!calculadas[this.tipoGrafica] && this.disponible(this.tipoGrafica)) {
      calculadas[this.tipoGrafica] = this.opcion(this.tipoGrafica);
    }
    this.opcionesCalculadas = calculadas;

    const chispas: { [clave: string]: any } = {};
    if (this.graficaVisible('sparklines')) {
      const tarjetas = this.indicadores();
      for (let i = 0; i < tarjetas.length; i++) {
        if (tarjetas[i].chispa.length > 0) {
          chispas[tarjetas[i].clave] = this.opcionChispa(tarjetas[i]);
        }
      }
    }
    this.chispas = chispas;
  }

  opcion(id: string): any {
    const datos = this.respuesta;
    if (!datos) {
      return {};
    }
    if (id === 'barras') { return this.opcionBarras(false); }
    if (id === 'barrasHorizontal') { return this.opcionBarras(true); }
    if (id === 'barrasAgrupadas') { return this.opcionSeries('', false); }
    if (id === 'barrasApiladas') { return this.opcionSeries('total', false); }
    if (id === 'barrasApiladas100') { return this.opcionSeries('total', true); }
    if (id === 'lollipop') { return this.opcionLollipop(); }
    if (id === 'radar') { return this.opcionRadar(); }
    if (id === 'pareto') { return this.opcionPareto(); }
    if (id === 'linea') { return this.opcionLinea(false, false); }
    if (id === 'area') { return this.opcionLinea(true, false); }
    if (id === 'lineasMultiples') { return this.opcionLineasMultiples(false); }
    if (id === 'areaApilada') { return this.opcionLineasMultiples(true); }
    if (id === 'escalonada') { return this.opcionEscalonada(); }
    if (id === 'combinada') { return this.opcionCombinada(); }
    if (id === 'cascada') { return this.opcionCascada(); }
    if (id === 'calendario') { return this.opcionCalendario(); }
    if (id === 'pastel') { return this.opcionCircular('0%', false); }
    if (id === 'dona') { return this.opcionCircular('45%', false); }
    if (id === 'semidona') { return this.opcionCircular('45%', true); }
    if (id === 'treemap') { return this.opcionTreemap(); }
    if (id === 'sunburst') { return this.opcionSunburst(); }
    if (id === 'waffle') { return this.opcionWaffle(); }
    if (id === 'histograma') { return this.opcionHistograma(); }
    if (id === 'campanaGauss') { return this.opcionCampanaGauss(); }
    if (id === 'boxplot') { return this.opcionBoxplot(); }
    if (id === 'dispersion') { return this.opcionDispersion(false); }
    if (id === 'burbujas') { return this.opcionDispersion(true); }
    if (id === 'calorDiaHora') { return this.opcionCalor(datos.calorDiaHora); }
    if (id === 'calorCategoriaMes') { return this.opcionCalor(datos.calorCategoriaMes); }
    if (id === 'embudo') { return this.opcionEmbudo(); }
    if (id === 'sankey') { return this.opcionSankey(); }
    if (id === 'trazabilidad') { return this.opcionTrazabilidad(); }
    if (id === 'arbol') { return this.opcionArbol(); }
    if (id === 'gauge') { return this.opcionGauge(); }
    if (id === 'bullet') { return this.opcionBullet(); }
    if (id === 'termometro') { return this.opcionTermometro(); }
    return {};
  }

  // Sparkline de una tarjeta de indicador
  opcionChispa(tarjeta: IndicadorTarjeta): any {
    const posiciones: number[] = [];
    for (let i = 0; i < tarjeta.chispa.length; i++) {
      posiciones.push(i);
    }
    return {
      grid: { top: 4, bottom: 4, left: 4, right: 4 },
      xAxis: { type: 'category', show: false, data: posiciones },
      yAxis: { type: 'value', show: false },
      series: [{
        type: 'line',
        data: tarjeta.chispa,
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 3 },
        areaStyle: { opacity: 0.35 }
      }]
    };
  }

  private base(): any {
    return {
      tooltip: { trigger: 'axis' },
      grid: { top: 40, bottom: 70, left: 60, right: 24, containLabel: true }
    };
  }

  private etiquetasEje(): any {
    return { color: TINTA, rotate: this.respuesta!.categorias.length > 7 ? 35 : 0, hideOverlap: true };
  }

  private opcionBarras(horizontal: boolean): any {
    const datos = this.respuesta!;
    const categoria = { type: 'category', data: datos.categorias, axisLabel: this.etiquetasEje() };
    const valor = { type: 'value', name: datos.series[0].nombre };
    return {
      ...this.base(),
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: horizontal ? valor : categoria,
      yAxis: horizontal ? { type: 'category', data: datos.categorias } : valor,
      series: [{
        type: 'bar',
        name: datos.series[0].nombre,
        data: datos.series[0].valores,
        barMaxWidth: 34,
        label: { show: datos.categorias.length <= 12, position: horizontal ? 'right' : 'top', color: TINTA }
      }]
    };
  }

  private opcionSeries(apilar: string, porcentaje: boolean): any {
    const datos = this.respuesta!;
    const series: any[] = [];

    for (let i = 0; i < datos.seriesDesglosadas.length; i++) {
      const serie = datos.seriesDesglosadas[i];
      let valores = serie.valores;

      if (porcentaje) {
        valores = [];
        for (let j = 0; j < serie.valores.length; j++) {
          const total = this.totalColumna(j);
          valores.push(total === 0 ? 0 : Math.round(serie.valores[j] * 1000 / total) / 10);
        }
      }
      series.push({
        type: 'bar',
        name: serie.nombre,
        stack: apilar ? apilar : undefined,
        data: valores,
        barMaxWidth: 40
      });
    }

    return {
      ...this.base(),
      legend: { bottom: 0, type: 'scroll' },
      xAxis: { type: 'category', data: datos.categorias, axisLabel: this.etiquetasEje() },
      yAxis: { type: 'value', max: porcentaje ? 100 : undefined, name: porcentaje ? '%' : '' },
      series
    };
  }

  private totalColumna(columna: number): number {
    const datos = this.respuesta!;
    let total = 0;
    for (let i = 0; i < datos.seriesDesglosadas.length; i++) {
      total = total + datos.seriesDesglosadas[i].valores[columna];
    }
    return total;
  }

  private opcionLollipop(): any {
    const datos = this.respuesta!;
    return {
      ...this.base(),
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: datos.categorias },
      series: [
        {
          type: 'bar',
          name: datos.series[0].nombre,
          data: datos.series[0].valores,
          barWidth: 3,
          itemStyle: { color: PALETA_PASTEL[4], borderWidth: 0 }
        },
        {
          type: 'scatter',
          name: datos.series[0].nombre,
          symbolSize: 16,
          data: datos.series[0].valores
        }
      ]
    };
  }

  private opcionRadar(): any {
    const datos = this.respuesta!;
    const indicadores: any[] = [];
    let maximo = 0;
    for (let i = 0; i < datos.series[0].valores.length; i++) {
      if (datos.series[0].valores[i] > maximo) {
        maximo = datos.series[0].valores[i];
      }
    }
    for (let i = 0; i < datos.categorias.length && i < 12; i++) {
      indicadores.push({ name: datos.categorias[i], max: maximo === 0 ? 1 : maximo });
    }
    return {
      tooltip: {},
      radar: {
        indicator: indicadores,
        axisName: { color: TINTA },
        splitLine: { lineStyle: { color: '#e5e0d5' } }
      },
      series: [{
        type: 'radar',
        name: datos.series[0].nombre,
        data: [{
          value: datos.series[0].valores.slice(0, 12),
          name: datos.series[0].nombre,
          areaStyle: { opacity: 0.35 }
        }]
      }]
    };
  }

  private opcionPareto(): any {
    const datos = this.respuesta!;
    const etiquetas: string[] = [];
    const cantidades: number[] = [];
    const acumulados: number[] = [];
    for (let i = 0; i < datos.pareto.length; i++) {
      etiquetas.push(datos.pareto[i].etiqueta);
      cantidades.push(datos.pareto[i].cantidad);
      acumulados.push(datos.pareto[i].acumulado);
    }
    return {
      ...this.base(),
      legend: { bottom: 0 },
      xAxis: { type: 'category', data: etiquetas, axisLabel: { color: TINTA, rotate: 35, hideOverlap: true } },
      yAxis: [
        { type: 'value', name: 'Casos' },
        { type: 'value', name: '% acumulado', max: 100, splitLine: { show: false } }
      ],
      series: [
        { type: 'bar', name: 'Casos rechazados', data: cantidades, barMaxWidth: 34 },
        { type: 'line', name: '% acumulado', yAxisIndex: 1, data: acumulados }
      ]
    };
  }

  private opcionLinea(area: boolean, escalonada: boolean): any {
    const datos = this.respuesta!;
    return {
      ...this.base(),
      xAxis: { type: 'category', boundaryGap: false, data: datos.categorias, axisLabel: this.etiquetasEje() },
      yAxis: { type: 'value', name: datos.series[0].nombre },
      series: [{
        type: 'line',
        name: datos.series[0].nombre,
        data: datos.series[0].valores,
        step: escalonada ? 'end' : undefined,
        areaStyle: area ? { opacity: 0.35 } : undefined,
        smooth: !escalonada
      }]
    };
  }

  private opcionLineasMultiples(apilar: boolean): any {
    const datos = this.respuesta!;
    const series: any[] = [];
    for (let i = 0; i < datos.seriesDesglosadas.length; i++) {
      series.push({
        type: 'line',
        name: datos.seriesDesglosadas[i].nombre,
        data: datos.seriesDesglosadas[i].valores,
        stack: apilar ? 'total' : undefined,
        areaStyle: apilar ? { opacity: 0.35 } : undefined,
        smooth: true
      });
    }
    return {
      ...this.base(),
      legend: { bottom: 0, type: 'scroll' },
      xAxis: { type: 'category', boundaryGap: false, data: datos.categorias, axisLabel: this.etiquetasEje() },
      yAxis: { type: 'value' },
      series
    };
  }

  private opcionEscalonada(): any {
    const datos = this.respuesta!;
    const etiquetas: string[] = [];
    const valores: number[] = [];
    for (let i = 0; i < datos.acumuladoAbiertos.length; i++) {
      etiquetas.push(datos.acumuladoAbiertos[i].etiqueta);
      valores.push(datos.acumuladoAbiertos[i].cantidad);
    }
    return {
      ...this.base(),
      xAxis: { type: 'category', boundaryGap: false, data: etiquetas, axisLabel: this.etiquetasEje() },
      yAxis: { type: 'value', name: 'Casos abiertos' },
      series: [{ type: 'line', step: 'end', name: 'Casos abiertos acumulados', data: valores }]
    };
  }

  private opcionCombinada(): any {
    const datos = this.respuesta!;
    return {
      ...this.base(),
      legend: { bottom: 0 },
      xAxis: { type: 'category', data: datos.mesesCombinada, axisLabel: this.etiquetasEje() },
      yAxis: [
        { type: 'value', name: 'Recibidos' },
        { type: 'value', name: 'Días promedio', splitLine: { show: false } }
      ],
      series: [
        { type: 'bar', name: 'Casos recibidos', data: datos.recibidosPorMes, barMaxWidth: 34 },
        { type: 'line', name: 'Tiempo promedio', yAxisIndex: 1, data: datos.tiempoPorMes }
      ]
    };
  }

  private opcionCascada(): any {
    const datos = this.respuesta!;
    const etiquetas: string[] = [];
    const base: number[] = [];
    const sube: number[] = [];
    const baja: number[] = [];
    let acumulado = 0;

    for (let i = 0; i < datos.cascada.length; i++) {
      const paso = datos.cascada[i];
      etiquetas.push(paso.etiqueta);

      // El primero y el último son totales, van desde cero
      const esTotal = i === 0 || i === datos.cascada.length - 1;
      if (esTotal) {
        base.push(0);
        sube.push(Math.abs(paso.cantidad));
        baja.push(0);
        acumulado = Math.abs(paso.cantidad);
      } else if (paso.cantidad < 0) {
        acumulado = acumulado + paso.cantidad;
        base.push(acumulado);
        sube.push(0);
        baja.push(Math.abs(paso.cantidad));
      } else {
        base.push(acumulado);
        sube.push(paso.cantidad);
        baja.push(0);
        acumulado = acumulado + paso.cantidad;
      }
    }

    return {
      ...this.base(),
      legend: { bottom: 0, data: ['Suma', 'Resta'] },
      xAxis: { type: 'category', data: etiquetas, axisLabel: this.etiquetasEje() },
      yAxis: { type: 'value', name: 'Casos' },
      series: [
        {
          type: 'bar', stack: 'cascada', name: 'base', silent: true,
          itemStyle: { color: 'transparent', borderWidth: 0 },
          emphasis: { itemStyle: { color: 'transparent' } },
          tooltip: { show: false },
          data: base
        },
        { type: 'bar', stack: 'cascada', name: 'Suma', data: sube, barMaxWidth: 46,
          label: { show: true, position: 'top', color: TINTA } },
        { type: 'bar', stack: 'cascada', name: 'Resta', data: baja, barMaxWidth: 46,
          itemStyle: { color: PALETA_PASTEL[3] },
          label: { show: true, position: 'top', color: TINTA } }
      ]
    };
  }

  private opcionCalendario(): any {
    const datos = this.respuesta!;
    const celdas: any[] = [];
    let maximo = 1;
    for (let i = 0; i < datos.calendario.length; i++) {
      celdas.push([datos.calendario[i].fecha, datos.calendario[i].valor]);
      if (datos.calendario[i].valor > maximo) {
        maximo = datos.calendario[i].valor;
      }
    }
    return {
      tooltip: { formatter: (p: any) => p.value[0] + ': ' + p.value[1] },
      visualMap: {
        min: 0, max: maximo, orient: 'horizontal', left: 'center', bottom: 0,
        inRange: { color: ESCALA_SECUENCIAL }
      },
      calendar: {
        top: 50, left: 50, right: 20, cellSize: ['auto', 14],
        range: [this.filtros.desde, this.filtros.hasta],
        itemStyle: { borderColor: '#e5e0d5', borderWidth: 1 },
        dayLabel: { color: TINTA, nameMap: ['D', 'L', 'M', 'M', 'J', 'V', 'S'] },
        monthLabel: { color: TINTA },
        yearLabel: { show: false }
      },
      series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: celdas }]
    };
  }

  private opcionCircular(radioInterno: string, media: boolean): any {
    const datos = this.respuesta!;
    const partes: any[] = [];
    for (let i = 0; i < datos.categorias.length; i++) {
      partes.push({ name: datos.categorias[i], value: datos.series[0].valores[i] });
    }
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: [radioInterno, '70%'],
        center: ['50%', media ? '68%' : '45%'],
        startAngle: media ? 180 : 90,
        endAngle: media ? 360 : undefined,
        data: partes,
        label: { color: TINTA, formatter: '{b}: {c}' }
      }]
    };
  }

  private opcionTreemap(): any {
    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      series: [{
        type: 'treemap',
        roam: false,
        data: this.aNodos(this.respuesta!.treemap),
        // Sin esto ECharts tiñe los hijos cambiando la saturación y se pierde el pastel
        levels: [
          { itemStyle: { borderWidth: 2, gapWidth: 2, borderColor: TINTA } },
          { color: PALETA_PASTEL, colorMappingBy: 'index',
            itemStyle: { gapWidth: 1, borderColor: TINTA } }
        ],
        label: { color: TINTA },
        upperLabel: { show: true, height: 22, color: TINTA }
      }]
    };
  }

  private opcionSunburst(): any {
    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      series: [{
        type: 'sunburst',
        radius: [0, '92%'],
        data: this.aNodos(this.respuesta!.sunburst),
        label: { color: TINTA },
        // El anillo de estados puede traer decenas de sectores finos: ahi la etiqueta
        // solo estorba y el estado se lee en el tooltip
        levels: [
          {},
          { label: { rotate: 'tangential' } },
          { label: { rotate: 'tangential' } },
          { label: { show: false } }
        ]
      }]
    };
  }

  private aNodos(nodos: any[]): any[] {
    const resultado: any[] = [];
    for (let i = 0; i < nodos.length; i++) {
      const nodo: any = { name: nodos[i].nombre, value: nodos[i].valor };
      if (nodos[i].hijos && nodos[i].hijos.length > 0) {
        nodo.children = this.aNodos(nodos[i].hijos);
      }
      resultado.push(nodo);
    }
    return resultado;
  }

  // Waffle: 100 cuadros repartidos según la proporción de cada categoría
  private opcionWaffle(): any {
    const datos = this.respuesta!;
    let total = 0;
    for (let i = 0; i < datos.series[0].valores.length; i++) {
      total = total + datos.series[0].valores[i];
    }
    const series: any[] = [];
    let cuadro = 0;

    for (let i = 0; i < datos.categorias.length; i++) {
      const cuantos = total === 0 ? 0 : Math.round(datos.series[0].valores[i] * 100 / total);
      const puntos: number[][] = [];
      for (let j = 0; j < cuantos && cuadro < 100; j++) {
        puntos.push([cuadro % 10, Math.floor(cuadro / 10)]);
        cuadro = cuadro + 1;
      }
      series.push({
        type: 'scatter',
        name: datos.categorias[i],
        symbol: 'rect',
        symbolSize: 18,
        data: puntos,
        itemStyle: { borderColor: TINTA, borderWidth: 1 }
      });
    }
    return {
      tooltip: { trigger: 'item', formatter: '{a}' },
      legend: { bottom: 0, type: 'scroll' },
      grid: { top: 30, bottom: 60, left: 30, right: 30 },
      xAxis: { type: 'value', min: -0.5, max: 9.5, show: false },
      yAxis: { type: 'value', min: -0.5, max: 9.5, show: false, inverse: true },
      series
    };
  }

  private opcionHistograma(): any {
    const datos = this.respuesta!;
    const etiquetas: string[] = [];
    const valores: number[] = [];
    for (let i = 0; i < datos.histograma.length; i++) {
      etiquetas.push(datos.histograma[i].etiqueta);
      valores.push(datos.histograma[i].cantidad);
    }
    return {
      ...this.base(),
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'category', data: etiquetas, axisLabel: { color: TINTA, rotate: 20 } },
      yAxis: { type: 'value', name: 'Casos resueltos' },
      series: [{
        type: 'bar',
        name: 'Casos resueltos',
        data: valores,
        barCategoryGap: '2%',
        label: { show: true, position: 'top', color: TINTA }
      }]
    };
  }

  // Campana de Gauss: las barras son el reparto real de los días de resolución y la
  // línea es la curva normal que les corresponde con esa media y esa desviación.
  private opcionCampanaGauss(): any {
    const datos = this.respuesta!;
    const centros: number[] = [];
    const conteos: number[] = [];
    let total = 0;

    for (let i = 0; i < datos.distribucionDias.length; i++) {
      centros.push(Number(datos.distribucionDias[i].etiqueta));
      conteos.push(datos.distribucionDias[i].cantidad);
      total = total + datos.distribucionDias[i].cantidad;
    }

    const ancho = centros.length > 1 ? centros[1] - centros[0] : 2;
    const media = datos.mediaDias;
    const desviacion = datos.desviacionDias;

    // La curva se escala a la cantidad de casos para que quede sobre las barras
    const curva: number[] = [];
    for (let i = 0; i < centros.length; i++) {
      const z = (centros[i] - media) / desviacion;
      const densidad = Math.exp(-0.5 * z * z) / (desviacion * Math.sqrt(2 * Math.PI));
      curva.push(Math.round(densidad * total * ancho * 100) / 100);
    }

    const etiquetas: string[] = [];
    for (let i = 0; i < centros.length; i++) {
      etiquetas.push(String(centros[i]));
    }

    return {
      ...this.base(),
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      title: {
        text: 'Media ' + media + ' días, desviación ' + desviacion + ' días',
        left: 'center',
        top: 4
      },
      xAxis: {
        type: 'category',
        data: etiquetas,
        name: 'Días de resolución',
        nameLocation: 'middle',
        nameGap: 32,
        axisLabel: { color: TINTA, hideOverlap: true }
      },
      yAxis: { type: 'value', name: 'Casos resueltos' },
      series: [
        { type: 'bar', name: 'Casos resueltos', data: conteos, barCategoryGap: '2%' },
        {
          type: 'line',
          name: 'Curva normal',
          data: curva,
          smooth: true,
          symbol: 'none',
          itemStyle: { color: PALETA_PASTEL[3] },
          lineStyle: { width: 3, color: PALETA_PASTEL[3] }
        }
      ]
    };
  }

  private opcionBoxplot(): any {
    const datos = this.respuesta!;
    const nombres: string[] = [];
    const cajas: number[][] = [];
    for (let i = 0; i < datos.boxplot.length; i++) {
      const caja = datos.boxplot[i];
      nombres.push(caja.tecnico);
      cajas.push([caja.minimo, caja.cuartilUno, caja.mediana, caja.cuartilTres, caja.maximo]);
    }
    return {
      ...this.base(),
      tooltip: { trigger: 'item' },
      xAxis: { type: 'category', data: nombres, axisLabel: { color: TINTA, rotate: 35, hideOverlap: true } },
      yAxis: { type: 'value', name: 'Días de resolución' },
      series: [{ type: 'boxplot', name: 'Días de resolución', data: cajas }]
    };
  }

  private opcionDispersion(burbujas: boolean): any {
    const datos = this.respuesta!;
    const puntos: any[] = [];
    for (let i = 0; i < datos.dispersion.length; i++) {
      const punto = datos.dispersion[i];
      puntos.push({ name: punto.etiqueta, value: [punto.x, punto.y, punto.tamano] });
    }
    return {
      ...this.base(),
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => p.data.name + '<br/>Casos: ' + p.data.value[0]
          + '<br/>Días promedio: ' + p.data.value[1] + '<br/>Resueltos: ' + p.data.value[2]
      },
      xAxis: { type: 'value', name: 'Casos a cargo' },
      yAxis: { type: 'value', name: 'Días promedio' },
      series: [{
        type: 'scatter',
        name: 'Técnicos',
        data: puntos,
        symbolSize: burbujas
          ? (valor: number[]) => 10 + Math.sqrt(valor[2]) * 6
          : 16,
        label: { show: !burbujas, position: 'right', color: TINTA, formatter: (p: any) => p.data.name },
        // Los nombres de los Tecnicos se enciman cuando varios tienen carga parecida
        labelLayout: { hideOverlap: true }
      }]
    };
  }

  private opcionCalor(mapa: any): any {
    const celdas: any[] = [];
    let maximo = 1;
    for (let i = 0; i < mapa.celdas.length; i++) {
      const celda = mapa.celdas[i];
      celdas.push([celda.columna, celda.fila, celda.valor]);
      if (celda.valor > maximo) {
        maximo = celda.valor;
      }
    }
    return {
      tooltip: { position: 'top' },
      grid: { top: 30, bottom: 80, left: 10, right: 20, containLabel: true },
      xAxis: { type: 'category', data: mapa.columnas, splitArea: { show: true },
               axisLabel: { color: TINTA, rotate: mapa.columnas.length > 14 ? 45 : 0 } },
      yAxis: { type: 'category', data: mapa.filas, splitArea: { show: true },
               axisLabel: { color: TINTA } },
      visualMap: {
        min: 0, max: maximo, calculable: true, orient: 'horizontal',
        left: 'center', bottom: 0, inRange: { color: ESCALA_SECUENCIAL }
      },
      series: [{
        type: 'heatmap',
        data: celdas,
        itemStyle: { borderColor: '#e5e0d5', borderWidth: 1 }
      }]
    };
  }

  private opcionEmbudo(): any {
    const datos = this.respuesta!;
    const partes: any[] = [];
    for (let i = 0; i < datos.embudo.length; i++) {
      partes.push({ name: datos.embudo[i].etiqueta, value: datos.embudo[i].cantidad });
    }
    return {
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'funnel',
        top: 20, bottom: 60, left: '10%', width: '80%',
        sort: 'none',
        gap: 3,
        label: { show: true, position: 'inside', color: TINTA, formatter: '{b}: {c}' },
        data: partes
      }]
    };
  }

  private opcionSankey(): any {
    const datos = this.respuesta!;
    const nombres: string[] = [];
    const enlaces: any[] = [];
    for (let i = 0; i < datos.sankey.length; i++) {
      const enlace = datos.sankey[i];
      if (nombres.indexOf(enlace.origen) < 0) { nombres.push(enlace.origen); }
      if (nombres.indexOf(enlace.destino) < 0) { nombres.push(enlace.destino); }
      enlaces.push({ source: enlace.origen, target: enlace.destino, value: enlace.valor });
    }
    const nodos: any[] = [];
    for (let i = 0; i < nombres.length; i++) {
      nodos.push({ name: nombres[i] });
    }
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'sankey',
        top: 20, bottom: 20, left: 20, right: 140,
        data: nodos,
        links: enlaces,
        // El backend nombra con su vuelta los estados a los que el caso regresa por una
        // devolución o una ampliación, así el flujo siempre avanza y nunca se cicla
        label: { color: TINTA },
        lineStyle: { color: 'gradient', opacity: 0.45, curveness: 0.5 }
      }]
    };
  }

  private opcionTrazabilidad(): any {
    const datos = this.respuesta!;
    const puntos: any[] = [];
    const etiquetas: string[] = [];
    for (let i = 0; i < datos.trazabilidad.length; i++) {
      const linea = datos.trazabilidad[i];
      etiquetas.push(linea.accion);
      puntos.push({
        name: linea.accion,
        value: [linea.fecha + 'T' + linea.hora, i],
        detalle: linea.detalle,
        usuario: linea.usuario
      });
    }
    return {
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => p.data.name + '<br/>' + p.data.value[0].replace('T', ' ')
          + '<br/>Usuario: ' + p.data.usuario
      },
      grid: { top: 30, bottom: 60, left: 30, right: 40, containLabel: true },
      xAxis: { type: 'time', name: 'Fecha y hora', axisLabel: { color: TINTA } },
      yAxis: { type: 'category', data: etiquetas, inverse: true, axisLabel: { color: TINTA } },
      series: [{
        type: 'line',
        name: 'Trazabilidad del caso ' + datos.casoTrazabilidad,
        data: puntos,
        symbolSize: 14,
        lineStyle: { width: 3, type: 'dashed' }
      }]
    };
  }

  private opcionArbol(): any {
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'tree',
        data: [{ name: 'Municipalidad', children: this.aNodos(this.respuesta!.arbol) }],
        top: 20, bottom: 20, left: 90, right: 160,
        symbolSize: 10,
        orient: 'LR',
        label: { position: 'left', verticalAlign: 'middle', align: 'right', color: TINTA },
        leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left', color: TINTA } },
        expandAndCollapse: true,
        initialTreeDepth: 2
      }]
    };
  }

  private opcionGauge(): any {
    const porcentaje = this.valorIndicador('porcentajeResolucion');
    return {
      series: [{
        type: 'gauge',
        min: 0,
        max: 100,
        progress: { show: true, width: 16 },
        axisLine: {
          lineStyle: {
            width: 16,
            // Semáforo: rojo abajo, amarillo al medio y verde arriba
            color: [[0.5, SEMAFORO_ROJO], [0.8, SEMAFORO_AMARILLO], [1, SEMAFORO_VERDE]]
          }
        },
        pointer: { itemStyle: { color: TINTA } },
        axisLabel: { color: TINTA, distance: 22 },
        detail: { valueAnimation: true, formatter: '{value} %', color: TINTA, fontSize: 24 },
        title: { show: true, offsetCenter: [0, '72%'], color: TINTA },
        data: [{ value: porcentaje, name: 'Resolución' }]
      }]
    };
  }

  private opcionBullet(): any {
    const porcentaje = this.valorIndicador('porcentajeResolucion');
    return {
      tooltip: { trigger: 'axis' },
      grid: { top: 40, bottom: 40, left: 90, right: 40 },
      xAxis: { type: 'value', max: 100, name: '%' },
      yAxis: { type: 'category', data: ['Resolución'] },
      series: [
        {
          type: 'bar', name: 'Meta 80%', barWidth: 44, silent: true,
          itemStyle: { color: SEMAFORO_AMARILLO, borderWidth: 0 },
          data: [80], tooltip: { show: false }
        },
        {
          type: 'bar', name: 'Porcentaje de resolución', barWidth: 18, barGap: '-68%',
          itemStyle: { color: SEMAFORO_VERDE },
          data: [porcentaje],
          label: { show: true, position: 'right', color: TINTA, formatter: '{c} %' }
        }
      ]
    };
  }

  private opcionTermometro(): any {
    const porcentaje = this.valorIndicador('porcentajeResolucion');
    return {
      tooltip: { trigger: 'axis' },
      grid: { top: 30, bottom: 40, left: 60, right: 40 },
      xAxis: { type: 'category', data: ['Resolución'] },
      yAxis: { type: 'value', max: 100, name: '%' },
      series: [
        {
          type: 'bar', name: 'Pendiente', stack: 'termometro', barWidth: 46,
          itemStyle: { color: '#EEF7FB' },
          data: [100 - porcentaje], tooltip: { show: false }, silent: true
        },
        {
          type: 'bar', name: 'Resuelto', stack: 'termometro', barWidth: 46,
          itemStyle: { color: porcentaje >= 80 ? SEMAFORO_VERDE
                              : porcentaje >= 50 ? SEMAFORO_AMARILLO : SEMAFORO_ROJO },
          data: [porcentaje],
          label: { show: true, position: 'inside', color: TINTA, formatter: '{c} %' }
        }
      ]
    };
  }

  // ---------- Apoyo ----------
  private comoTexto(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
