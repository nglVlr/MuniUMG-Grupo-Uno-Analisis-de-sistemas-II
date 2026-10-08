import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SeguimientoCasoComponent } from '../seguimiento-caso/seguimiento-caso.component';
import { MiCuentaComponent } from '../mi-cuenta/mi-cuenta.component';
import { ReportesComponent } from '../reportes/reportes.component';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { JefaturaService } from '../core/jefatura.service';
import { CasoBandejaSupervisor, CasoDetalleSupervisor } from '../core/supervisor.service';

type VistaJefatura = 'menu' | 'lista' | 'detalle' | 'devolver' | 'seguimiento' | 'cuenta' | 'reportes';

@Component({
  selector: 'app-bandeja-jefatura',
  standalone: true,
  imports: [CommonModule, FormsModule, SeguimientoCasoComponent, MiCuentaComponent, ReportesComponent],
  templateUrl: './bandeja-jefatura.component.html',
  styleUrl: './bandeja-jefatura.component.css'
})
export class BandejaJefaturaComponent {
  vista: VistaJefatura = 'menu';

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
    private jefaturaService: JefaturaService,
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  // ---------- CU-07 paso 3: el usuario selecciona el menú "Casos Pendientes de Firma" ----------
  abrirCasosPendientesFirma(): void {
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

  // ---------- CU-07 FA04: Cerrar sesión ----------
  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  cargarCasos(): void {
    this.mensaje = '';
    this.cargando = true;

    this.jefaturaService.listarBandeja().subscribe({
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

        // FA01: no existen casos pendientes de firma
        if (this.casos.length === 0) {
          this.mensaje = 'No existen casos pendientes de firma en este momento.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No existen casos pendientes de firma en este momento.');
      }
    });
  }

  // ---------- CU-07 FA03: Búsqueda de caso ----------
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

  // ---------- CU-07 FA03: Filtro de casos (fecha, tipo, categoría) ----------
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

  // ---------- CU-07 pasos 6 y 7: ver detalle del caso ----------
  verDetalle(id: number): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = true;

    this.jefaturaService.obtenerDetalle(id).subscribe({
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

  // ---------- CU-07 paso 9: Firmar Oficio ----------
  firmarOficio(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.jefaturaService.firmarOficio(this.casoSeleccionado.id).subscribe({
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

  // ---------- CU-07 FA02: abre el formulario de devolución a Supervisión ----------
  abrirDevolucion(): void {
    this.mensaje = '';
    this.observaciones = '';
    this.vista = 'devolver';
  }

  confirmarDevolucion(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';

    if (!this.observaciones.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;

    this.jefaturaService.devolverASupervision(
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

  // ---------- Archivos del expediente que revisa la Jefatura ----------
  verEvidencia(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.jefaturaService.verEvidencia(this.casoSeleccionado.id).subscribe({
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
    this.jefaturaService.verSoporteAtencion(this.casoSeleccionado.id).subscribe({
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
    this.jefaturaService.verReporteAtencion(this.casoSeleccionado.id).subscribe({
      next: (archivo) => this.abrirArchivo(archivo),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  verReporteSupervision(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.jefaturaService.verReporteSupervision(this.casoSeleccionado.id).subscribe({
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
