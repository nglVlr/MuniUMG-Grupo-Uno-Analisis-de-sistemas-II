import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-recuperar-password',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink
  ],
  templateUrl: './recuperar-password.component.html',
  styleUrls: ['./recuperar-password.component.css']
})
export class RecuperarPasswordComponent {

  correo = '';

  mensaje = '';
  error = '';

  cargando = false;
  enviado = false;

  private readonly urlRecuperacion = '/api/password/olvide';

  constructor(private http: HttpClient) {}

  recuperarPassword(): void {

    this.mensaje = '';
    this.error = '';

    if (!this.correo.trim()) {
      this.error = 'Debes ingresar tu correo electrónico.';
      return;
    }

    this.cargando = true;

    this.http.post(
        this.urlRecuperacion,
        {
            correo: this.correo.trim()
        },
        {
            responseType: 'text'
        }
    ).subscribe({

      next: (respuesta: string) => {

        console.log('Solicitud de recuperación exitosa:', respuesta);

        this.cargando = false;
        this.enviado = true;

        this.mensaje = respuesta;
      },

      error: (error) => {

        console.error('Error al solicitar recuperación:', error);

        this.cargando = false;

        if (error.error?.mensaje) {
          this.error = error.error.mensaje;
        } else {
          this.error = 'No fue posible procesar la solicitud.';
        }
      }

    });
  }
}