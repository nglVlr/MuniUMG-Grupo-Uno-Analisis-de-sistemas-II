import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

export interface RegistroBitacora {
  id: number;
  accion: string;
  usuario: string;
  rol: string;
  entidad: string;
  codigoSeguimiento: string;
  fecha: string;
  hora: string;
  ipOrigen: string;
  detalle: string;
  valorAnterior: string;
  valorNuevo: string;
}

export interface PaginaBitacora {
  registros: RegistroBitacora[];
  pagina: number;
  totalPaginas: number;
  totalRegistros: number;
}

export interface FiltrosBitacora {
  desde: string;
  hasta: string;
  usuario: string;
  accion: string;
  entidad: string;
  rol: string;
  pagina: number;
}

@Injectable({ providedIn: 'root' })
export class BitacoraService {
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

  private armarParametros(filtros: FiltrosBitacora): HttpParams {
    let params = new HttpParams()
      .set('desde', filtros.desde)
      .set('hasta', filtros.hasta)
      .set('pagina', String(filtros.pagina));

    if (filtros.usuario) {
      params = params.set('usuario', filtros.usuario);
    }
    if (filtros.accion) {
      params = params.set('accion', filtros.accion);
    }
    if (filtros.entidad) {
      params = params.set('entidad', filtros.entidad);
    }
    if (filtros.rol) {
      params = params.set('rol', filtros.rol);
    }
    return params;
  }

  consultar(filtros: FiltrosBitacora): Observable<PaginaBitacora> {
    return this.http.get<PaginaBitacora>(`${environment.apiUrl}/bitacora`, {
      headers: this.obtenerCabeceras(),
      params: this.armarParametros(filtros)
    });
  }

  listarAcciones(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/bitacora/acciones`, {
      headers: this.obtenerCabeceras()
    });
  }

  exportarCsv(filtros: FiltrosBitacora): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/bitacora/csv`, {
      headers: this.obtenerCabeceras(),
      params: this.armarParametros(filtros),
      responseType: 'blob'
    });
  }
}
