import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CuentaService, MiCuenta } from '../core/cuenta.service';

// CU-12 Administrar Cuenta: se reutiliza en las cinco bandejas.
@Component({
  selector: 'app-mi-cuenta',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mi-cuenta.component.html',
  styleUrl: './mi-cuenta.component.css'
})
export class MiCuentaComponent implements OnInit {
  cuenta: MiCuenta | null = null;
  nombre = '';

  // FA01: cambio de contraseña
  mostrarPassword = false;
  passwordActual = '';
  passwordNueva = '';
  passwordConfirmacion = '';

  mensaje = '';
  mensajeExito = '';
  cargando = false;
  private temporizadorExito: ReturnType<typeof setTimeout> | null = null;

  constructor(private cuentaService: CuentaService) {}

  ngOnInit(): void {
    this.cargarCuenta();
  }

  // ---------- CU-12 paso 2: el sistema muestra los datos del usuario ----------
  cargarCuenta(): void {
    this.mensaje = '';
    this.cargando = true;

    this.cuentaService.obtenerMiCuenta().subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.cuenta = resultado;
        this.nombre = resultado.nombre;
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No tiene permisos para acceder a este recurso');
      }
    });
  }

  // ---------- CU-12 pasos 4 al 8: Guardar ----------
  guardarDatos(): void {
    this.mensaje = '';

    if (!this.nombre.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;

    this.cuentaService.actualizarDatos(this.nombre.trim()).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.cargarCuenta();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Los datos ingresados no son válidos, o las contraseñas no coinciden.');
      }
    });
  }

  // ---------- CU-12 FA01: abrir el cambio de contraseña ----------
  abrirCambioPassword(): void {
    this.mensaje = '';
    this.passwordActual = '';
    this.passwordNueva = '';
    this.passwordConfirmacion = '';
    this.mostrarPassword = true;
  }

  cancelarCambioPassword(): void {
    this.mensaje = '';
    this.mostrarPassword = false;
  }

  guardarPassword(): void {
    this.mensaje = '';

    if (!this.passwordActual || !this.passwordNueva || !this.passwordConfirmacion) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }
    // FA02: deben coincidir y cumplir RN08 (alfanumérica, mínimo 6 caracteres)
    if (this.passwordNueva !== this.passwordConfirmacion
        || !/^[A-Za-z0-9]{6,}$/.test(this.passwordNueva)) {
      this.mensaje = 'Los datos ingresados no son válidos, o las contraseñas no coinciden.';
      return;
    }

    this.cargando = true;

    this.cuentaService.cambiarPassword({
      passwordActual: this.passwordActual,
      passwordNueva: this.passwordNueva,
      passwordConfirmacion: this.passwordConfirmacion
    }).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.cancelarCambioPassword();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Los datos ingresados no son válidos, o las contraseñas no coinciden.');
      }
    });
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
