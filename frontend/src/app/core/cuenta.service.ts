import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { MensajeRespuesta } from './admin-usuario.service';

export interface MiCuenta {
  cui: string;
  nombre: string;
  correo: string;
  rol: string;
  departamento: string;
}

export interface CambioPassword {
  passwordActual: string;
  passwordNueva: string;
  passwordConfirmacion: string;
}

@Injectable({ providedIn: 'root' })
export class CuentaService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token del usuario en cada petición.
  // Como todavía no hay un interceptor, lo agregamos a mano en cada llamada.
  private obtenerCabeceras(): HttpHeaders {
    const token = this.authService.obtenerToken();
    return new HttpHeaders({
      Authorization: 'Bearer ' + token
    });
  }

  obtenerMiCuenta(): Observable<MiCuenta> {
    return this.http.get<MiCuenta>(`${environment.apiUrl}/cuenta`, {
      headers: this.obtenerCabeceras()
    });
  }

  actualizarDatos(nombre: string): Observable<MensajeRespuesta> {
    return this.http.put<MensajeRespuesta>(
      `${environment.apiUrl}/cuenta`,
      { nombre },
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA01: cambio de contraseña
  cambiarPassword(datos: CambioPassword): Observable<MensajeRespuesta> {
    return this.http.put<MensajeRespuesta>(
      `${environment.apiUrl}/cuenta/password`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }
}
