import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SeguimientoCasoComponent } from '../seguimiento-caso/seguimiento-caso.component';
import { MiCuentaComponent } from '../mi-cuenta/mi-cuenta.component';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  AuxiliarService,
  CasoBandeja,
  CasoDetalleAuxiliar,
  MotivoRechazo
} from '../core/auxiliar.service';

type VistaAuxiliar = 'menu' | 'lista' | 'detalle' | 'rechazo' | 'seguimiento' | 'cuenta';

@Component({
  selector: 'app-bandeja-auxiliar',
  standalone: true,
  imports: [CommonModule, FormsModule, SeguimientoCasoComponent, MiCuentaComponent],
  templateUrl: './bandeja-auxiliar.component.html',
  styleUrl: './bandeja-auxiliar.component.css'
})
export class BandejaAuxiliarComponent {
  vista: VistaAuxiliar = 'menu';

  casos: CasoBandeja[] = [];
  casosOriginal: CasoBandeja[] = [];
  categoriasDisponibles: string[] = [];
  tiposCaso = ['Queja', 'Reclamo', 'Denuncia', 'Sugerencia'];

  codigoBusqueda = '';
  filtroFecha = '';
  filtroTipo = '';
  filtroCategoria = '';

  casoSeleccionado: CasoDetalleAuxiliar | null = null;
  motivos: MotivoRechazo[] = [];

  idMotivoRechazo: number | null = null;
  comentarioRechazo = '';
  aplicaAmpliacion = false;

  mensaje = '';
  mensajeExito = '';
  cargando = false;
  private temporizadorExito: ReturnType<typeof setTimeout> | null = null;

  evidenciaUrl: string | null = null;
  evidenciaEsImagen = false;

  // El encabezado de la bandeja muestra quien tiene la sesión abierta
  nombreUsuario = '';

  constructor(
    private auxiliarService: AuxiliarService,
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  // ---------- El usuario selecciona el menú "Casos Asignados" ----------
  abrirCasosAsignados(): void {
    this.vista = 'lista';
    this.mensajeExito = '';
    this.cargarBandeja();
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

  // ---------- FA06: Cerrar sesión ----------
  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  cargarBandeja(): void {
    this.mensaje = '';
    this.cargando = true;

    this.auxiliarService.listarBandeja().subscribe({
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

        // FA01: no existen casos pendientes
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

  // ---------- FA04: Búsqueda de caso ----------
  buscarCaso(): void {
    const codigo = this.codigoBusqueda.trim().toLowerCase();

    if (!codigo) {
      this.casos = this.casosOriginal;
      return;
    }

    const resultado: CasoBandeja[] = [];
    for (let i = 0; i < this.casosOriginal.length; i++) {
      const caso = this.casosOriginal[i];
      if (caso.codigoSeguimiento.toLowerCase().includes(codigo)) {
        resultado.push(caso);
      }
    }
    this.casos = resultado;
  }

  // ---------- FA05: Filtro de casos ----------
  aplicarFiltros(): void {
    const resultado: CasoBandeja[] = [];

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

  // ---------- El usuario selecciona el caso con el botón "ver detalle" ----------
  verDetalle(id: number): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = true;

    this.auxiliarService.obtenerDetalle(id).subscribe({
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

  aceptarCaso(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.auxiliarService.aceptarCaso(this.casoSeleccionado.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.casoSeleccionado = resultado;
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  // ---------- FA08: Técnico no activo (incluido en el mensaje de error) ----------
  asignarTecnico(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.auxiliarService.asignarTecnico(this.casoSeleccionado.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No existe un Técnico activo en el departamento para asignar el caso.');
      }
    });
  }

  // ---------- FA02: Rechazo sin ampliación ----------
  mostrarFormularioRechazo(): void {
    this.mensaje = '';
    this.idMotivoRechazo = null;
    this.comentarioRechazo = '';
    this.aplicaAmpliacion = false;
    this.vista = 'rechazo';

    if (this.motivos.length === 0) {
      this.auxiliarService.listarMotivosRechazo().subscribe({
        next: (resultado) => {
          this.motivos = resultado;
        },
        error: (error) => {
          this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
        }
      });
    }
  }

  // ---------- FA09: Cancelar ----------
  cancelarRechazo(): void {
    this.vista = 'detalle';
  }

  // ---------- FA02 / FA03: confirmar el rechazo (con o sin ampliación) ----------
  confirmarRechazo(): void {
    if (!this.casoSeleccionado) {
      return;
    }

    this.mensaje = '';

    if (!this.idMotivoRechazo || !this.comentarioRechazo.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }
    if (this.comentarioRechazo.trim().length > 500) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;

    this.auxiliarService.rechazarCaso(this.casoSeleccionado.id, {
      idMotivoRechazo: this.idMotivoRechazo,
      comentario: this.comentarioRechazo.trim(),
      aplicaAmpliacion: this.aplicaAmpliacion
    }).subscribe({
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

  // ---------- FA07: Regresar (retorna al paso 4, la lista) ----------
  regresarALaLista(): void {
    this.casoSeleccionado = null;
    this.vista = 'lista';
    this.cargarBandeja();
  }

  // ---------- Evidencia (RN14): botón para ver el archivo adjunto ----------
  verEvidencia(): void {
    if (!this.casoSeleccionado) {
      return;
    }

    this.auxiliarService.verEvidencia(this.casoSeleccionado.id).subscribe({
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
