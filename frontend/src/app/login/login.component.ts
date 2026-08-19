import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit{

  usuario = '';
  password = '';

  mostrarPassword = false;

  error = '';
  cargando = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.cerrarSesion();
  }

  togglePassword(): void {
    this.mostrarPassword = !this.mostrarPassword;
  }

  iniciarSesion(): void {

    this.error = '';

    if (!this.usuario || !this.password) {
      this.error = 'Debes ingresar usuario y contraseña.';
      return;
    }

    this.cargando = true;

    this.authService
      .iniciarSesion(this.usuario, this.password)
      .subscribe({
        next: (respuesta) => {

          console.log('Login exitoso:', respuesta);

          this.authService.guardarSesion(respuesta);

          this.cargando = false;

          this.router.navigate(['/inicio']);
        },

        error: (error) => {

          console.error('Error de autenticación:', error);

          this.cargando = false;

          if (error.error?.mensaje) {
            this.error = error.error.mensaje;
          } else {
            this.error = 'No fue posible conectar con el servidor.';
          }
        }
      });
  }
}