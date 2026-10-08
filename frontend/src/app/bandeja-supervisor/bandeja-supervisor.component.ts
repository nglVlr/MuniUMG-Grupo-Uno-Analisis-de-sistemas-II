import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SeguimientoCasoComponent } from '../seguimiento-caso/seguimiento-caso.component';
import { MiCuentaComponent } from '../mi-cuenta/mi-cuenta.component';
import { ReportesComponent } from '../reportes/reportes.component';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  SupervisorService,
  CasoBandejaSupervisor,
  CasoDetalleSupervisor
} from '../core/supervisor.service';

type VistaSupervisor = 'menu' | 'lista' | 'detalle' | 'devolver' | 'regresar' | 'seguimiento' | 'cuenta' | 'reportes';

@Component({
  selector: 'app-bandeja-supervisor',
  standalone: true,
  imports: [CommonModule, FormsModule, SeguimientoCasoComponent, MiCuentaComponent, ReportesComponent],
  templateUrl: './bandeja-supervisor.component.html',
  styleUrl: './bandeja-supervisor.component.css'
})
export class BandejaSupervisorComponent {
  vista: VistaSupervisor = 'menu';

  casos: CasoBandejaSupervisor[] = [];
  casosOriginal: CasoBandejaSupervisor[] = [];
  categoriasDisponibles: string[] = [];
  tiposCaso = ['Queja', 'Reclamo', 'Denuncia', 'Sugerencia'];

  codigoBusqueda = '';
  filtroFecha = '';
  filtroTipo = '';
  filtroCategoria = '';

  casoSeleccionado: CasoDetalleSupervisor | null = null;
  observaciones = '';

  mensaje = '';
  mensajeExito = '';
  cargando = false;
  private temporizadorExito: ReturnType<typeof setTimeout> | null = null;

  evidenciaUrl: string | null = null;
  evidenciaEsImagen = false;

  // El encabezado de la bandeja muestra quien tiene la sesión abierta
  nombreUsuario = '';

  constructor(
    private supervisorService: SupervisorService,
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  // ---------- CU-06 paso 3: el usuario selecciona el menú "Casos en Supervisión" ----------
  abrirCasosEnSupervision(): void {
    this.vista = 'lista';
    this.mensajeExito = '';
    this.cargarCasos();
  }

  volverAlMenu(): void {
    this.vista = 'menu';
  }

  // CU-14: reportes estadisticos y trazabilidad
  abrirReportes(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'reportes';
  }

  // CU-12: datos propios del usuario
  abrirMiCuenta(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'cuenta';
  }

  // Seguimiento del caso: consulta de solo lectura del expediente
  abrirSeguimiento(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'seguimiento';
  }

  // ---------- CU-06 FA05: Cerrar sesión ----------
  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  cargarCasos(): void {
    this.mensaje = '';
    this.cargando = true;

    this.supervisorService.listarBandeja().subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.casos = resultado;
        this.casosOriginal = resultado;
        this.limpiarFiltros();

        // Armamos la lista de categorías que aparecen en los casos, con un for normal
        this.categoriasDisponibles = [];
        for (let i = 0; i < resultado.length; i++) {
          const categoria = resultado[i].categoria;
          if (!this.categoriasDisponibles.includes(categoria)) {
            this.categoriasDisponibles.push(categoria);
          }
        }

        // FA01: no existen casos pendientes en la bandeja de supervisión
        if (this.casos.length === 0) {
          this.mensaje = 'No existen casos pendientes en la bandeja de supervisión.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No existen casos pendientes en la bandeja de supervisión.');
      }
    });
  }

  // ---------- CU-06 FA06: Búsqueda de caso ----------
  buscarCaso(): void {
    const codigo = this.codigoBusqueda.trim().toLowerCase();

    if (!codigo) {
      this.casos = this.casosOriginal;
      return;
    }

    const resultado: CasoBandejaSupervisor[] = [];
    for (let i = 0; i < this.casosOriginal.length; i++) {
      const caso = this.casosOriginal[i];
      if (caso.codigoSeguimiento.toLowerCase().includes(codigo)) {
        resultado.push(caso);
      }
    }
    this.casos = resultado;
  }

  // ---------- CU-06 FA07: Filtro de casos (fecha, tipo, categoría) ----------
  aplicarFiltros(): void {
    const resultado: CasoBandejaSupervisor[] = [];

    for (let i = 0; i < this.casosOriginal.length; i++) {
      const caso = this.casosOriginal[i];
      let cumpleFiltros = true;

      if (this.filtroFecha && !caso.fechaIngreso.startsWith(this.filtroFecha)) {
        cumpleFiltros = false;
      }
      if (this.filtroTipo && caso.tipoCaso !== this.filtroTipo) {
        cumpleFiltros = false;
      }
      if (this.filtroCategoria && caso.categoria !== this.filtroCategoria) {
        cumpleFiltros = false;
      }

      if (cumpleFiltros) {
        resultado.push(caso);
      }
    }

    this.casos = resultado;
  }

  limpiarFiltros(): void {
    this.codigoBusqueda = '';
    this.filtroFecha = '';
    this.filtroTipo = '';
    this.filtroCategoria = '';
    this.casos = this.casosOriginal;
  }

  // ---------- CU-06 pasos 6 y 7: ver detalle del caso ----------
  verDetalle(id: number): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = true;

    this.supervisorService.obtenerDetalle(id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.casoSeleccionado = resultado;
        this.vista = 'detalle';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- CU-06 paso 9: Aprobar el reporte de atención ----------
  aprobarReporte(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.supervisorService.aprobarReporte(this.casoSeleccionado.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- CU-06 FA03: Aprobar el rechazo registrado por el Auxiliar ----------
  aprobarRechazo(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.supervisorService.aprobarRechazo(this.casoSeleccionado.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- CU-06 FA02: abre el formulario de devolución al Técnico ----------
  abrirDevolucion(): void {
    this.mensaje = '';
    this.observaciones = '';
    this.vista = 'devolver';
  }

  // ---------- CU-06 FA04: abre el formulario para regresar a revisión ----------
  abrirRegresoARevision(): void {
    this.mensaje = '';
    this.observaciones = '';
    this.vista = 'regresar';
  }

  confirmarDevolucion(): void {
    if (!this.casoSeleccionado || !this.validarObservaciones()) {
      return;
    }
    this.cargando = true;

    this.supervisorService.devolverAlTecnico(
      this.casoSeleccionado.id,
      this.observaciones.trim()
    ).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  confirmarRegresoARevision(): void {
    if (!this.casoSeleccionado || !this.validarObservaciones()) {
      return;
    }
    this.cargando = true;

    this.supervisorService.regresarARevision(
      this.casoSeleccionado.id,
      this.observaciones.trim()
    ).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  // ---------- Cancelar vuelve al detalle del caso ----------
  cancelarFormulario(): void {
    this.mensaje = '';
    this.vista = 'detalle';
  }

  regresarALaLista(): void {
    this.casoSeleccionado = null;
    this.observaciones = '';
    this.vista = 'lista';
    this.cargarCasos();
  }

  // ---------- Archivos del expediente que revisa el Supervisor ----------
  verEvidencia(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.supervisorService.verEvidencia(this.casoSeleccionado.id).subscribe({
      next: (archivo) => this.abrirArchivo(archivo),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  verSoporteAtencion(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.supervisorService.verSoporteAtencion(this.casoSeleccionado.id).subscribe({
      next: (archivo) => this.abrirArchivo(archivo),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  verReporteAtencion(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.supervisorService.verReporteAtencion(this.casoSeleccionado.id).subscribe({
      next: (archivo) => this.abrirArchivo(archivo),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- Cierra el modal de la evidencia (botón "X") ----------
  cerrarEvidencia(): void {
    if (this.evidenciaUrl) {
      window.URL.revokeObjectURL(this.evidenciaUrl);
    }
    this.evidenciaUrl = null;
    this.evidenciaEsImagen = false;
  }

  private abrirArchivo(archivo: Blob): void {
    const url = window.URL.createObjectURL(archivo);
    if (archivo.type.startsWith('image/')) {
      this.evidenciaUrl = url;
      this.evidenciaEsImagen = true;
    } else {
      window.open(url, '_blank');
    }
  }

  private validarObservaciones(): boolean {
    if (!this.observaciones.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return false;
    }
    return true;
  }

  // Muestra el mensaje de éxito y lo oculta solo a los 10 segundos
  private mostrarMensajeExito(texto: string): void {
    this.mensajeExito = texto;
    if (this.temporizadorExito) {
      clearTimeout(this.temporizadorExito);
    }
    this.temporizadorExito = setTimeout(() => {
      this.mensajeExito = '';
    }, 10000);
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
