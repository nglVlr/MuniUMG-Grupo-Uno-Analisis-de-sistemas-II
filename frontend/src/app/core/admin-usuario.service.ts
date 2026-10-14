import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

export interface UsuarioAdmin {
  cui: string;
  nombre: string;
  correo: string;
  idRol: number;
  rol: string;
  idDepartamento: number;
  departamento: string;
  idEstado: number;
  estado: string;
  casosActivos: number;
}

export interface GuardarUsuario {
  cui: string;
  nombre: string;
  correo: string;
  password: string;
  idRol: number | null;
  idDepartamento: number | null;
  idEstado: number | null;
}

export interface OpcionCatalogo {
  id: number;
  nombre: string;
}

export interface CatalogosUsuario {
  roles: OpcionCatalogo[];
  departamentos: OpcionCatalogo[];
  estados: OpcionCatalogo[];
}

export interface MensajeRespuesta {
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class AdminUsuarioService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token del Admin en cada petición.
  // Como todavía no hay un interceptor, lo agregamos a mano en cada llamada.
  private obtenerCabeceras(): HttpHeaders {
    const token = this.authService.obtenerToken();
    return new HttpHeaders({
      Authorization: 'Bearer ' + token
    });
  }

  listarUsuarios(): Observable<UsuarioAdmin[]> {
    return this.http.get<UsuarioAdmin[]>(`${environment.apiUrl}/admin/usuarios`, {
      headers: this.obtenerCabeceras()
    });
  }

  obtenerCatalogos(): Observable<CatalogosUsuario> {
    return this.http.get<CatalogosUsuario>(`${environment.apiUrl}/admin/usuarios/catalogos`, {
      headers: this.obtenerCabeceras()
    });
  }

  crearUsuario(datos: GuardarUsuario): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/admin/usuarios`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }

  editarUsuario(cui: string, datos: GuardarUsuario): Observable<MensajeRespuesta> {
    return this.http.put<MensajeRespuesta>(
      `${environment.apiUrl}/admin/usuarios/${cui}`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA04: cambio del estado de disponibilidad
  // FA08: cuiDestino solo se envía cuando hay casos pendientes que reasignar
  cambiarEstado(cui: string, idEstado: number, cuiDestino = ''): Observable<MensajeRespuesta> {
    return this.http.put<MensajeRespuesta>(
      `${environment.apiUrl}/admin/usuarios/${cui}/estado`,
      { idEstado, cuiDestino },
      { headers: this.obtenerCabeceras() }
    );
  }
}
