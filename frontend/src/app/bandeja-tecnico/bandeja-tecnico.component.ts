import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SeguimientoCasoComponent } from '../seguimiento-caso/seguimiento-caso.component';
import { MiCuentaComponent } from '../mi-cuenta/mi-cuenta.component';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  TecnicoService,
  Actividad,
  CasoBandejaTecnico,
  CasoDetalleTecnico
} from '../core/tecnico.service';

type VistaTecnico = 'menu' | 'lista' | 'detalle' | 'actividades' | 'nueva' | 'finalizar' | 'seguimiento' | 'cuenta';

@Component({
  selector: 'app-bandeja-tecnico',
  standalone: true,
  imports: [CommonModule, FormsModule, SeguimientoCasoComponent, MiCuentaComponent],
  templateUrl: './bandeja-tecnico.component.html',
  styleUrl: './bandeja-tecnico.component.css'
})
export class BandejaTecnicoComponent {
  vista: VistaTecnico = 'menu';

  // RN20: la fecha de la actividad es la del sistema
  hoy = new Date();

  casos: CasoBandejaTecnico[] = [];
  casosOriginal: CasoBandejaTecnico[] = [];
  categoriasDisponibles: string[] = [];
  tiposCaso = ['Queja', 'Reclamo', 'Denuncia', 'Sugerencia'];

  codigoBusqueda = '';
  filtroFecha = '';
  filtroTipo = '';
  filtroCategoria = '';

  casoSeleccionado: CasoDetalleTecnico | null = null;
  actividades: Actividad[] = [];
  mensajeActividades = '';

  detalleActividad = '';
  soporte: File | null = null;

  mensaje = '';
  mensajeExito = '';
  cargando = false;
  private temporizadorExito: ReturnType<typeof setTimeout> | null = null;

  evidenciaUrl: string | null = null;
  evidenciaEsImagen = false;

  // El encabezado de la bandeja muestra quien tiene la sesión abierta
  nombreUsuario = '';

  constructor(
    private tecnicoService: TecnicoService,
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  // ---------- CU-05 paso 3: el usuario selecciona el menú "Casos Asignados" ----------
  abrirCasosAsignados(): void {
    this.vista = 'lista';
    this.mensajeExito = '';
    this.cargarCasos();
  }

  volverAlMenu(): void {
    this.vista = 'menu';
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

  // ---------- CU-05 FA03: Cerrar sesión ----------
  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  cargarCasos(): void {
    this.mensaje = '';
    this.cargando = true;

    this.tecnicoService.listarCasosAsignados().subscribe({
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

        // FA01: no existen casos asignados
        if (this.casos.length === 0) {
          this.mensaje = 'No existen casos pendientes en este momento.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No existen casos pendientes en este momento.');
      }
    });
  }

  // ---------- CU-05 FA04: Búsqueda de caso ----------
  buscarCaso(): void {
    const codigo = this.codigoBusqueda.trim().toLowerCase();

    if (!codigo) {
      this.casos = this.casosOriginal;
      return;
    }

    const resultado: CasoBandejaTecnico[] = [];
    for (let i = 0; i < this.casosOriginal.length; i++) {
      const caso = this.casosOriginal[i];
      if (caso.codigoSeguimiento.toLowerCase().includes(codigo)) {
        resultado.push(caso);
      }
    }
    this.casos = resultado;
  }

  // ---------- CU-05 FA05: Filtro de casos (fecha, tipo, categoría) ----------
  aplicarFiltros(): void {
    const resultado: CasoBandejaTecnico[] = [];

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

  // ---------- CU-05 paso 6: el usuario selecciona el caso con "ver detalle" ----------
  verDetalle(id: number): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = true;

    this.tecnicoService.obtenerDetalle(id).subscribe({
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

  // ---------- CU-05 pasos 9 y 10: Registrar Actividad ----------
  abrirActividades(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.mensajeActividades = '';
    this.vista = 'actividades';
    this.cargando = true;

    this.tecnicoService.listarActividades(this.casoSeleccionado.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.actividades = resultado;

        // FA06: caso sin actividades. No impide usar el botón "Agregar".
        if (this.actividades.length === 0) {
          this.mensajeActividades = 'Caso sin actividades registradas.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- CU-05 pasos 12 y 13: Agregar abre el formulario nueva actividad ----------
  abrirNuevaActividad(): void {
    this.mensaje = '';
    this.detalleActividad = '';
    this.vista = 'nueva';
  }

  // ---------- CU-05 pasos 15 al 19: Guardar la actividad ----------
  guardarActividad(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';

    if (!this.detalleActividad.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;

    this.tecnicoService.registrarActividad(this.casoSeleccionado.id, this.detalleActividad.trim()).subscribe({
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

  // ---------- CU-05 FA02: Finalizar caso ----------
  abrirFinalizar(): void {
    this.mensaje = '';
    this.detalleActividad = '';
    this.soporte = null;
    this.vista = 'finalizar';
  }

  seleccionarSoporte(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.soporte = entrada.files && entrada.files.length > 0 ? entrada.files[0] : null;
  }

  confirmarFinalizar(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';

    if (!this.detalleActividad.trim() || !this.soporte) {
      this.mensaje = 'El reporte de atención no contiene toda la información obligatoria.';
      return;
    }

    this.cargando = true;

    this.tecnicoService.finalizarAtencion(
      this.casoSeleccionado.id,
      this.detalleActividad.trim(),
      this.soporte
    ).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'El reporte de atención no contiene toda la información obligatoria.');
      }
    });
  }

  // ---------- CU-05 FA08: Cancelar vuelve al detalle del caso (paso 7) ----------
  cancelarFormulario(): void {
    this.mensaje = '';
    this.vista = 'detalle';
  }

  // ---------- CU-05 FA07: Regresar vuelve a la lista de casos (paso 4) ----------
  regresarALaLista(): void {
    this.casoSeleccionado = null;
    this.actividades = [];
    this.mensajeActividades = '';
    this.vista = 'lista';
    this.cargarCasos();
  }

  // ---------- Evidencia del ciudadano: botón para ver el archivo adjunto ----------
  verEvidencia(): void {
    if (!this.casoSeleccionado) {
      return;
    }

    this.tecnicoService.verEvidencia(this.casoSeleccionado.id).subscribe({
      next: (archivo) => {
        const url = window.URL.createObjectURL(archivo);
        if (archivo.type.startsWith('image/')) {
          this.evidenciaUrl = url;
          this.evidenciaEsImagen = true;
        } else {
          window.open(url, '_blank');
        }
      },
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
