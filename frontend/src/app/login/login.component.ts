import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/** CU-01 Ingreso al Sistema (placeholder simple, sin cifrado de contraseña). */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  username = '';
  password = '';
  mensaje = '';

  ingresar(): void {
    // TODO: llamar a POST /api/auth/login cuando se implemente el modulo de autenticacion
    this.mensaje = 'Funcionalidad de ingreso pendiente de conectar al backend.';
  }
}