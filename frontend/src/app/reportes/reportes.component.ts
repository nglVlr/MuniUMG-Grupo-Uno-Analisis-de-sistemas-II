import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Conteo,
  FiltrosReporte,
  OpcionDepartamento,
  ReporteEstadistico,
  ReporteService,
  TrazabilidadCaso
} from '../core/reporte.service';

// CU-14 Reportería: se reutiliza en las bandejas de Supervisor, Jefatura y Admin.
@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.css'
})
export class ReportesComponent implements OnInit {
  pestana: 'estadisticas' | 'trazabilidad' = 'estadisticas';

  tiposCaso = ['Queja', 'Reclamo', 'Denuncia', 'Sugerencia'];
  estados = [
    'Registrado', 'En Revisión', 'En Atención', 'En Supervisión',
    'Pendiente de Aprobación de Rechazo', 'Pendiente de Firma',
    'Resuelto', 'Rechazado', 'Requiere Ampliación'
  ];

  // El catalogo de departamentos es administrable (CU-10), por eso se pide al backend
  departamentos: OpcionDepartamento[] = [];

  filtros: FiltrosReporte = {
    fechaInicio: '',
    fechaFin: '',
    idDepartamento: null,
    tipoCaso: '',
    estado: ''
  };

  reporte: ReporteEstadistico | null = null;

  codigoTrazabilidad = '';
  trazabilidad: TrazabilidadCaso | null = null;

  mensaje = '';
  cargando = false;

  constructor(private reporteService: ReporteService) {}

  ngOnInit(): void {
    // Por defecto, el ano en curso
    const hoy = new Date();
    this.filtros.fechaFin = this.comoTexto(hoy);
    this.filtros.fechaInicio = this.comoTexto(new Date(hoy.getFullYear(), 0, 1));

    // RN32: el Supervisor y el Jefe reciben unicamente su propio departamento
    this.reporteService.listarDepartamentos().subscribe({
      next: (resultado) => {
        this.departamentos = resultado;
        if (resultado.length === 1) {
          this.filtros.idDepartamento = resultado[0].id;
        }
      },
      error: () => (this.departamentos = [])
    });
  }

  cambiarPestana(pestana: 'estadisticas' | 'trazabilidad'): void {
    this.pestana = pestana;
    this.mensaje = '';
  }

  // ---------- CU-14 pasos 3 y 4: generar el reporte estadístico ----------
  generar(): void {
    this.mensaje = '';
    this.reporte = null;

    if (!this.filtros.fechaInicio || !this.filtros.fechaFin) {
      this.mensaje = 'El rango de fechas ingresado no es válido.';
      return;
    }

    this.cargando = true;
    this.reporteService.generarEstadisticas(this.filtros).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.reporte = resultado;
        if (resultado.totalCasos === 0) {
          this.mensaje = 'No existen registros para los filtros aplicados.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'El rango de fechas ingresado no es válido.');
      }
    });
  }

  // ---------- CU-14 paso 5: exportar ----------
  exportar(formato: 'pdf' | 'excel'): void {
    this.mensaje = '';
    this.reporteService.exportarEstadisticas(this.filtros, formato).subscribe({
      next: (archivo) => this.descargar(archivo, 'reporte-estadistico.' + this.extension(formato)),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'El rango de fechas ingresado no es válido.');
      }
    });
  }

  // ---------- CU-14: trazabilidad del caso ----------
  generarTrazabilidad(): void {
    this.mensaje = '';
    this.trazabilidad = null;

    if (!this.codigoTrazabilidad.trim()) {
      this.mensaje = 'Debe ingresar un código de seguimiento para continuar.';
      return;
    }

    this.cargando = true;
    this.reporteService.generarTrazabilidad(this.codigoTrazabilidad.trim()).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.trazabilidad = resultado;
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  exportarTrazabilidad(formato: 'pdf' | 'excel'): void {
    if (!this.trazabilidad) {
      return;
    }
    const nombre = 'trazabilidad-' + this.trazabilidad.codigoSeguimiento + '.' + this.extension(formato);
    this.reporteService.exportarTrazabilidad(this.trazabilidad.codigoSeguimiento, formato).subscribe({
      next: (archivo) => this.descargar(archivo, nombre),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- Apoyo para dibujar las tablas en pantalla ----------

  // Suma de la columna Cantidad, que se muestra en el pie de cada tabla
  total(filas: Conteo[]): number {
    let suma = 0;
    for (let i = 0; i < filas.length; i++) {
      suma = suma + filas[i].cantidad;
    }
    return suma;
  }

  // Cada estado del caso se pinta con su propio color en la etiqueta
  claseEstado(estado: string): string {
    switch (estado) {
      case 'Resuelto':
        return 'pill--resuelto';
      case 'Rechazado':
        return 'pill--rechazado';
      case 'Registrado':
        return 'pill--registrado';
      case 'En Revisión':
      case 'En Atención':
      case 'En Supervisión':
        return 'pill--proceso';
      case 'Pendiente de Aprobación de Rechazo':
      case 'Pendiente de Firma':
        return 'pill--pendiente';
      case 'Requiere Ampliación':
        return 'pill--ampliacion';
      default:
        return 'pill--registrado';
    }
  }

  // El backend entrega el libro de Excel como .xlsx
  private extension(formato: 'pdf' | 'excel'): string {
    return formato === 'excel' ? 'xlsx' : 'pdf';
  }

  private descargar(archivo: Blob, nombre: string): void {
    const url = window.URL.createObjectURL(archivo);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    window.URL.revokeObjectURL(url);
  }

  private comoTexto(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
