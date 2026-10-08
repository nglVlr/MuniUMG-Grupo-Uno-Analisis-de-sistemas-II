import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { Actividad } from './tecnico.service';

export interface HistorialCaso {
  accion: string;
  usuario: string;
  fecha: string;
  hora: string;
  ipOrigen: string;
  detalle: string;
}

export interface Expediente {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  departamento: string;
  descripcion: string;
  direccion: string;
  estado: string;
  nombreCiudadano: string;
  nombreTecnico: string;
  motivoRechazo: string;
  comentarioRechazo: string;
  observacionesSupervision: string;
  observacionesJefatura: string;
  tieneEvidencia: boolean;
  tieneSoporteAtencion: boolean;
  tieneInformeInicial: boolean;
  tieneReporteAtencion: boolean;
  tieneReporteSupervision: boolean;
  tieneOficio: boolean;
  actividades: Actividad[];
  historial: HistorialCaso[];
}

@Injectable({ providedIn: 'root' })
export class SeguimientoService {
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

  buscarPorCodigo(codigo: string): Observable<Expediente> {
    return this.http.get<Expediente>(`${environment.apiUrl}/seguimiento/casos`, {
      headers: this.obtenerCabeceras(),
      params: { codigo }
    });
  }

  obtenerExpediente(id: number): Observable<Expediente> {
    return this.http.get<Expediente>(`${environment.apiUrl}/seguimiento/casos/${id}`, {
      headers: this.obtenerCabeceras()
    });
  }

  // Pedimos el documento como "blob" (datos binarios) para abrirlo en una pestaña nueva.
  verDocumento(id: number, tipo: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/seguimiento/casos/${id}/documentos/${tipo}`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }
}
