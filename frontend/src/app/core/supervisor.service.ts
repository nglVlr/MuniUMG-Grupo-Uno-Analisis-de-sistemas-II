import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { Actividad } from './tecnico.service';

export interface CasoBandejaSupervisor {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  estado: string;
}

export interface CasoDetalleSupervisor {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  descripcion: string;
  direccion: string;
  estado: string;
  nombreTecnico: string;
  motivoRechazo: string;
  comentarioRechazo: string;
  tieneEvidencia: boolean;
  tieneSoporteAtencion: boolean;
  tieneReporteAtencion: boolean;
  tieneReporteSupervision: boolean;
  actividades: Actividad[];
}

export interface MensajeRespuesta {
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class SupervisorService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token del Supervisor en cada petición.
  // Como todavía no hay un interceptor, lo agregamos a mano en cada llamada.
  private obtenerCabeceras(): HttpHeaders {
    const token = this.authService.obtenerToken();
    return new HttpHeaders({
      Authorization: 'Bearer ' + token
    });
  }

  listarBandeja(): Observable<CasoBandejaSupervisor[]> {
    return this.http.get<CasoBandejaSupervisor[]>(`${environment.apiUrl}/supervisor/casos`, {
      headers: this.obtenerCabeceras()
    });
  }

  obtenerDetalle(id: number): Observable<CasoDetalleSupervisor> {
    return this.http.get<CasoDetalleSupervisor>(`${environment.apiUrl}/supervisor/casos/${id}`, {
      headers: this.obtenerCabeceras()
    });
  }

  aprobarReporte(id: number): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/supervisor/casos/${id}/aprobar`,
      {},
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA02: Devolver al Técnico
  devolverAlTecnico(id: number, observaciones: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/supervisor/casos/${id}/devolver`,
      { observaciones },
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA03: Aprobar Rechazo
  aprobarRechazo(id: number): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/supervisor/casos/${id}/aprobar-rechazo`,
      {},
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA04: Regresar a Revisión
  regresarARevision(id: number, observaciones: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/supervisor/casos/${id}/regresar-revision`,
      { observaciones },
      { headers: this.obtenerCabeceras() }
    );
  }

  verEvidencia(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/supervisor/casos/${id}/evidencia`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }

  verSoporteAtencion(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/supervisor/casos/${id}/soporte-atencion`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }

  verReporteAtencion(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/supervisor/casos/${id}/reporte-atencion`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }
}
