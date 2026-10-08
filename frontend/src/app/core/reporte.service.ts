import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { HistorialCaso } from './seguimiento.service';

export interface Conteo {
  etiqueta: string;
  cantidad: number;
}

export interface ReporteEstadistico {
  fechaInicio: string;
  fechaFin: string;
  totalCasos: number;
  casosResueltos: number;
  diasPromedioAtencion: number;
  porEstado: Conteo[];
  porTipoCaso: Conteo[];
  porDepartamento: Conteo[];
  porCategoria: Conteo[];
}

export interface TrazabilidadCaso {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  departamento: string;
  estado: string;
  nombreCiudadano: string;
  nombreTecnico: string;
  historial: HistorialCaso[];
}

export interface FiltrosReporte {
  fechaInicio: string;
  fechaFin: string;
  idDepartamento: number | null;
  tipoCaso: string;
  estado: string;
}

@Injectable({ providedIn: 'root' })
export class ReporteService {
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

  private armarParametros(filtros: FiltrosReporte): HttpParams {
    let params = new HttpParams()
      .set('fechaInicio', filtros.fechaInicio)
      .set('fechaFin', filtros.fechaFin);

    if (filtros.idDepartamento) {
      params = params.set('idDepartamento', String(filtros.idDepartamento));
    }
    if (filtros.tipoCaso) {
      params = params.set('tipoCaso', filtros.tipoCaso);
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    return params;
  }

  generarEstadisticas(filtros: FiltrosReporte): Observable<ReporteEstadistico> {
    return this.http.get<ReporteEstadistico>(`${environment.apiUrl}/reportes/estadisticas`, {
      headers: this.obtenerCabeceras(),
      params: this.armarParametros(filtros)
    });
  }

  // Pedimos el archivo como "blob" (datos binarios) para abrirlo o descargarlo.
  // El formato es "pdf" o "excel", tal como lo expone el backend.
  exportarEstadisticas(filtros: FiltrosReporte, formato: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/reportes/estadisticas/${formato}`, {
      headers: this.obtenerCabeceras(),
      params: this.armarParametros(filtros),
      responseType: 'blob'
    });
  }

  generarTrazabilidad(codigoSeguimiento: string): Observable<TrazabilidadCaso> {
    return this.http.get<TrazabilidadCaso>(`${environment.apiUrl}/reportes/trazabilidad`, {
      headers: this.obtenerCabeceras(),
      params: { codigoSeguimiento }
    });
  }

  exportarTrazabilidad(codigoSeguimiento: string, formato: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/reportes/trazabilidad/${formato}`, {
      headers: this.obtenerCabeceras(),
      params: { codigoSeguimiento },
      responseType: 'blob'
    });
  }
}
