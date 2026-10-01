import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AuxiliarService,
  CasoBandeja,
  CasoDetalleAuxiliar,
  MotivoRechazo
} from '../core/auxiliar.service';

type VistaAuxiliar = 'lista' | 'detalle' | 'rechazo';

@Component({
  selector: 'app-bandeja-auxiliar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './bandeja-auxiliar.component.html',
  styleUrl: './bandeja-auxiliar.component.css'
})
export class BandejaAuxiliarComponent implements OnInit {
  vista: VistaAuxiliar = 'lista';

  casos: CasoBandeja[] = [];
  casoSeleccionado: CasoDetalleAuxiliar | null = null;
  motivos: MotivoRechazo[] = [];

  idMotivoRechazo: number | null = null;
  comentarioRechazo = '';
  aplicaAmpliacion = false;

  mensaje = '';
  mensajeExito = '';
  cargando = false;

  constructor(private auxiliarService: AuxiliarService) {}

  ngOnInit(): void {
    this.cargarBandeja();
  }

  cargarBandeja(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = true;

    this.auxiliarService.listarBandeja().subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.casos = resultado;

        if (this.casos.length === 0) {
          this.mensaje = 'No existen casos pendientes de revisión en este momento.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'El servicio no está disponible en este momento. Intente nuevamente más tarde.');
      }
    });
  }

  verDetalle(id: number): void {
    this.mensaje = '';
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

  asignarTecnico(): void {
    if (!this.casoSeleccionado) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.auxiliarService.asignarTecnico(this.casoSeleccionado.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mensajeExito = resultado.mensaje;
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No existe un Técnico activo en el departamento para asignar el caso.');
      }
    });
  }

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
          this.mensaje = this.mensajeError(error, 'El servicio no está disponible en este momento. Intente nuevamente más tarde.');
        }
      });
    }
  }

  cancelarRechazo(): void {
    this.vista = 'detalle';
  }

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
        this.mensajeExito = resultado.mensaje;
        this.regresarALaLista();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  regresarALaLista(): void {
    this.casoSeleccionado = null;
    this.vista = 'lista';
    this.cargarBandeja();
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}