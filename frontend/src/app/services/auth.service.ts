import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginRequest {
  usuario: string;
  password: string;
}

export interface LoginResponse {
  jwtToken: string;
  correo: string;
  nombreUsuario: string;
  rol: number;
  cui: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly loginUrl = 'api/login';

  constructor(private http: HttpClient) {}

  iniciarSesion(
    usuario: string,
    password: string
  ): Observable<LoginResponse> {

    const credenciales: LoginRequest = {
      usuario,
      password
    };

    return this.http.post<LoginResponse>(
      this.loginUrl,
      credenciales
    );
  }

  guardarSesion(respuesta: LoginResponse): void {
    localStorage.setItem('jwtToken', respuesta.jwtToken);
    localStorage.setItem('correo', respuesta.correo);
    localStorage.setItem('nombreUsuario', respuesta.nombreUsuario);
    localStorage.setItem('rol', respuesta.rol.toString());
    localStorage.setItem('cui', respuesta.cui);
  }

  cerrarSesion(): void {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('correo');
    localStorage.removeItem('nombreUsuario');
    localStorage.removeItem('rol');
    localStorage.removeItem('cui');
  }

  obtenerToken(): string | null {
    return localStorage.getItem('jwtToken');
  }

  obtenerRol(): number | null {
    const rol = localStorage.getItem('rol');
    return rol ? Number(rol) : null;
  }

  estaAutenticado(): boolean {
    return this.obtenerToken() !== null;
  }
}