import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SeguimientoService, Expediente } from '../core/seguimiento.service';

// Seguimiento del caso: opción de solo lectura que se reutiliza en las cuatro
// bandejas (Auxiliar, Técnico, Supervisor y Jefatura).
@Component({
  selector: 'app-seguimiento-caso',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './seguimiento-caso.component.html',
  styleUrl: './seguimiento-caso.component.css'
})
export class SeguimientoCasoComponent {
  codigoBusqueda = '';
  expediente: Expediente | null = null;

  mensaje = '';
  cargando = false;

  constructor(private seguimientoService: SeguimientoService) {}

  buscar(): void {
    this.mensaje = '';
    this.expediente = null;

    const codigo = this.codigoBusqueda.trim();
    if (!codigo) {
      this.mensaje = 'Debe ingresar un código de seguimiento para continuar.';
      return;
    }

    this.cargando = true;

    this.seguimientoService.buscarPorCodigo(codigo).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.expediente = resultado;
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  limpiar(): void {
    this.codigoBusqueda = '';
    this.expediente = null;
    this.mensaje = '';
  }

  verDocumento(tipo: string): void {
    if (!this.expediente) {
      return;
    }

    this.seguimientoService.verDocumento(this.expediente.id, tipo).subscribe({
      next: (archivo) => {
        const url = window.URL.createObjectURL(archivo);
        window.open(url, '_blank');
      },
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No se encontró un caso con el código o número ingresado.');
      }
    });
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
