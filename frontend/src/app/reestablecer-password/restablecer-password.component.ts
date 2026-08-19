import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-restablecer-password',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink
  ],
  templateUrl: './restablecer-password.component.html',
  styleUrls: ['./restablecer-password.component.css']
})
export class RestablecerPasswordComponent implements OnInit {

  token = '';

  password = '';
  confirmarPassword = '';

  mostrarPassword = false;
  mostrarConfirmarPassword = false;

  mensaje = '';
  error = '';

  cargando = false;
  actualizado = false;

  private readonly urlRestablecer = '/api/password/restablecer';

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {

    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.error = 'El enlace de recuperación no contiene un token válido.';
    }
  }

  togglePassword(): void {
    this.mostrarPassword = !this.mostrarPassword;
  }

  toggleConfirmarPassword(): void {
    this.mostrarConfirmarPassword = !this.mostrarConfirmarPassword;
  }

  restablecerPassword(): void {

    this.mensaje = '';
    this.error = '';

    if (!this.token) {
      this.error = 'El enlace de recuperación no es válido.';
      return;
    }

    if (!this.password || !this.confirmarPassword) {
      this.error = 'Debes ingresar y confirmar tu nueva contraseña.';
      return;
    }

    if (this.password !== this.confirmarPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.cargando = true;

    this.http.post(
      this.urlRestablecer,
      {
        token: this.token,
        password: this.password,
        confirmarPassword: this.confirmarPassword
      },
      {
        responseType: 'text'
      }
    ).subscribe({

      next: (respuesta: string) => {

        console.log('Contraseña actualizada:', respuesta);

        this.cargando = false;
        this.actualizado = true;
        this.mensaje = respuesta;

      },

      error: (error) => {

        console.error('Error al restablecer contraseña:', error);

        this.cargando = false;

        let respuestaError: any = error.error;

        // Cuando Angular recibe el body como texto,
        // convertimos el JSON string a objeto.
        if (typeof respuestaError === 'string') {
            try {
            respuestaError = JSON.parse(respuestaError);
            } catch {
            respuestaError = null;
            }
        }

        const detalles = respuestaError?.detalles;

        if (Array.isArray(detalles) && detalles.length > 0) {

            this.error = detalles
            .map(
                (detalle: { campo: string; motivo: string }) =>
                detalle.motivo
            )
            .join(' ');

        } else if (respuestaError?.mensaje) {

            this.error = respuestaError.mensaje;

        } else {

            this.error = 'No fue posible actualizar la contraseña.';
        }
        }

    });
  }
}