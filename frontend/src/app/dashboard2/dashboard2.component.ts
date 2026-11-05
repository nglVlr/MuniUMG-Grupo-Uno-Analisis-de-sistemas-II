import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgxEchartsDirective } from 'ngx-echarts';
import { AuthService } from '../services/auth.service';
import {
  CampanaGauss,
  Categoria2,
  Dashboard2Service,
  DispersionTecnico,
  Filtros2,
  ValorEtiqueta
} from '../core/dashboard2.service';

// Lo que llena los selectores de departamento
interface Opcion {
  id: number;
  nombre: string;
}

// Dashboard 2: cuatro graficas con cuatro filtros. Pantalla del Admin.
@Component({
  selector: 'app-dashboard2',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxEchartsDirective],
  templateUrl: './dashboard2.component.html',
  styleUrl: './dashboard2.component.css'
})
export class Dashboard2Component implements OnInit {

  // Los colores pastel, en un solo lugar
  readonly colores = ['#A8D8EA', '#FFD6A5', '#CAFFBF', '#FFADAD', '#BDB2FF',
    '#FDFFB6', '#9BF6FF', '#FFC6FF', '#D0F4DE', '#E4C1F9'];
  readonly tinta = '#2F3640';

  // Los cuatro filtros. PARA AGREGAR UNO: agregarlo aqui, en el HTML,
  // en dashboard2.service.ts y en la consulta del repositorio.
  filtros: Filtros2 = {
    desde: '',
    hasta: '',
    idDepartamento: null,
    tipoCaso: '',
    idCategoria: null
  };

  // Lo que llena los selectores
  departamentos: Opcion[] = [];
  tiposCaso: string[] = [];
  categorias: Categoria2[] = [];

  // Lo que devuelve cada consulta
  datosBarras: ValorEtiqueta[] = [];
  datosPastel: ValorEtiqueta[] = [];
  datosDispersion: DispersionTecnico[] = [];
  datosCampana: CampanaGauss = { tramos: [], media: 0, desviacion: 0 };

  // Las opciones que recibe cada grafica de ECharts
  opcionBarras: any = {};
  opcionPastel: any = {};
  opcionDispersion: any = {};
  opcionCampana: any = {};

  nombreUsuario = '';
  mensaje = '';
  cargando = false;

  constructor(
    private dashboard2Service: Dashboard2Service,
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  ngOnInit(): void {
    // Por defecto, los ultimos 12 meses
    const hoy = new Date();
    this.filtros.hasta = this.comoTexto(hoy);
    this.filtros.desde = this.comoTexto(
      new Date(hoy.getFullYear() - 1, hoy.getMonth(), hoy.getDate()));

    this.cargarFiltros();
    this.consultar();
  }

  volverAlMenu(): void {
    this.router.navigate(['/bandeja-admin']);
  }

  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  // La categoria depende del departamento y del tipo de caso: solo se muestran
  // las categorias de ese departamento que ademas admiten ese tipo de caso.
  categoriasFiltradas(): Categoria2[] {
    const resultado: Categoria2[] = [];

    for (let i = 0; i < this.categorias.length; i++) {
      const categoria = this.categorias[i];

      if (this.filtros.idDepartamento
          && categoria.idDepartamento !== Number(this.filtros.idDepartamento)) {
        continue;
      }
      if (this.filtros.tipoCaso
          && categoria.tiposCaso.indexOf(this.filtros.tipoCaso) < 0) {
        continue;
      }
      resultado.push(categoria);
    }
    return resultado;
  }

  // Al cambiar el departamento o el tipo, la categoria elegida puede ya no estar
  // en la lista, asi que se limpia antes de consultar
  cambiarDepartamento(): void {
    this.filtros.idCategoria = null;
    this.consultar();
  }

  cambiarTipoCaso(): void {
    this.filtros.idCategoria = null;
    this.consultar();
  }

  limpiarFiltros(): void {
    this.filtros.idDepartamento = null;
    this.filtros.tipoCaso = '';
    this.filtros.idCategoria = null;
    this.consultar();
  }

  // Llena los selectores de departamento, tipo de caso y categoria
  private cargarFiltros(): void {
    this.dashboard2Service.filtros().subscribe({
      next: (resultado) => {
        this.departamentos = resultado.departamentos;
        this.tiposCaso = resultado.tiposCaso;
        this.categorias = resultado.categorias;
      },
      error: () => {
        this.mensaje = 'No tiene permisos para acceder a este recurso';
      }
    });
  }

  // ---------- Las cuatro consultas ----------
  consultar(): void {
    this.mensaje = '';
    this.cargando = true;

    this.dashboard2Service.barras(this.filtros).subscribe({
      next: (datos) => {
        this.cargando = false;
        this.datosBarras = datos;
        this.opcionBarras = this.armarBarras();
        if (datos.length === 0) {
          this.mensaje = 'No existen registros para los filtros aplicados.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.datosBarras = [];
        this.opcionBarras = {};
        this.mensaje = this.textoError(error);
      }
    });

    this.dashboard2Service.pastel(this.filtros).subscribe({
      next: (datos) => {
        this.datosPastel = datos;
        this.opcionPastel = this.armarPastel();
      },
      error: () => {
        this.datosPastel = [];
        this.opcionPastel = {};
      }
    });

    this.dashboard2Service.dispersion(this.filtros).subscribe({
      next: (datos) => {
        this.datosDispersion = datos;
        this.opcionDispersion = this.armarDispersion();
      },
      error: () => {
        this.datosDispersion = [];
        this.opcionDispersion = {};
      }
    });

    this.dashboard2Service.campana(this.filtros).subscribe({
      next: (datos) => {
        this.datosCampana = datos;
        this.opcionCampana = this.armarCampana();
      },
      error: () => {
        this.datosCampana = { tramos: [], media: 0, desviacion: 0 };
        this.opcionCampana = {};
      }
    });
  }

  // ---------- 1. Barras verticales ----------
  private armarBarras(): any {
    const etiquetas: string[] = [];
    const valores: number[] = [];

    for (let i = 0; i < this.datosBarras.length; i++) {
      etiquetas.push(this.datosBarras[i].etiqueta);
      valores.push(this.datosBarras[i].valor);
    }

    return {
      color: this.colores,
      textStyle: { color: this.tinta },
      tooltip: { trigger: 'axis' },
      grid: { top: 30, bottom: 90, left: 60, right: 20 },
      xAxis: { type: 'category', data: etiquetas, axisLabel: { color: this.tinta, rotate: 35 } },
      yAxis: { type: 'value', name: 'Casos', axisLabel: { color: this.tinta } },
      series: [{
        type: 'bar',
        data: valores,
        barMaxWidth: 40,
        itemStyle: { borderColor: this.tinta, borderWidth: 1 },
        label: { show: true, position: 'top', color: this.tinta }
      }]
    };
  }

  // ---------- 2. Pastel ----------
  private armarPastel(): any {
    const partes: any[] = [];

    for (let i = 0; i < this.datosPastel.length; i++) {
      partes.push({
        name: this.datosPastel[i].etiqueta,
        value: this.datosPastel[i].valor
      });
    }

    return {
      color: this.colores,
      textStyle: { color: this.tinta },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '65%',
        center: ['50%', '45%'],
        data: partes,
        itemStyle: { borderColor: this.tinta, borderWidth: 1 },
        label: { color: this.tinta, formatter: '{b}: {c}' }
      }]
    };
  }

  // ---------- 3. Dispersion ----------
  private armarDispersion(): any {
    const puntos: any[] = [];

    for (let i = 0; i < this.datosDispersion.length; i++) {
      const fila = this.datosDispersion[i];
      puntos.push({
        name: fila.tecnico,
        value: [fila.cantidadCasos, fila.diasPromedio]
      });
    }

    return {
      color: this.colores,
      textStyle: { color: this.tinta },
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => p.data.name + '<br/>Casos: ' + p.data.value[0]
          + '<br/>Días promedio: ' + p.data.value[1]
      },
      grid: { top: 30, bottom: 60, left: 60, right: 40 },
      xAxis: {
        type: 'value',
        name: 'Casos a cargo',
        nameLocation: 'middle',
        nameGap: 28,
        axisLabel: { color: this.tinta }
      },
      yAxis: { type: 'value', name: 'Días promedio', axisLabel: { color: this.tinta } },
      series: [{
        type: 'scatter',
        symbolSize: 18,
        data: puntos,
        itemStyle: { borderColor: this.tinta, borderWidth: 1 }
      }]
    };
  }

  // ---------- 4. Campana de Gauss ----------
  private armarCampana(): any {
    const etiquetas: string[] = [];
    const valores: number[] = [];
    let total = 0;

    for (let i = 0; i < this.datosCampana.tramos.length; i++) {
      etiquetas.push(this.datosCampana.tramos[i].etiqueta);
      valores.push(this.datosCampana.tramos[i].valor);
      total = total + this.datosCampana.tramos[i].valor;
    }

    // La curva normal: para cada tramo se calcula su altura con la formula de Gauss
    const media = this.datosCampana.media;
    const desviacion = this.datosCampana.desviacion;
    const curva: number[] = [];

    for (let i = 0; i < etiquetas.length; i++) {
      if (desviacion === 0) {
        curva.push(0);
        continue;
      }
      const dias = Number(etiquetas[i]);
      const z = (dias - media) / desviacion;
      const altura = Math.exp(-0.5 * z * z) / (desviacion * Math.sqrt(2 * Math.PI));
      // Se multiplica por el total y por el ancho del tramo (2 días) para que
      // la curva quede a la misma altura que las barras
      curva.push(Math.round(altura * total * 2 * 100) / 100);
    }

    return {
      color: this.colores,
      textStyle: { color: this.tinta },
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      grid: { top: 40, bottom: 70, left: 60, right: 20 },
      xAxis: {
        type: 'category',
        data: etiquetas,
        name: 'Días de resolución',
        nameLocation: 'middle',
        nameGap: 30,
        axisLabel: { color: this.tinta }
      },
      yAxis: { type: 'value', name: 'Casos resueltos', axisLabel: { color: this.tinta } },
      series: [
        {
          type: 'bar',
          name: 'Casos resueltos',
          data: valores,
          barCategoryGap: '2%',
          itemStyle: { borderColor: this.tinta, borderWidth: 1 }
        },
        {
          type: 'line',
          name: 'Curva normal',
          data: curva,
          smooth: true,
          symbol: 'none',
          lineStyle: { width: 3, color: '#FFADAD' },
          itemStyle: { color: '#FFADAD' }
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

  private textoError(error: { error?: { mensaje?: string } }): string {
    return error?.error?.mensaje ?? 'El rango de fechas ingresado no es válido.';
  }
}
