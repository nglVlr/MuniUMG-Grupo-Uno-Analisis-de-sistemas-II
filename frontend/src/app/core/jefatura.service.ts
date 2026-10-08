import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { CasoBandejaSupervisor, CasoDetalleSupervisor, MensajeRespuesta } from './supervisor.service';

@Injectable({ providedIn: 'root' })
export class JefaturaService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token de la Jefatura en cada petición.
  // Como todavía no hay un interceptor, lo agregamos a mano en cada llamada.
  private obtenerCabeceras(): HttpHeaders {
    const token = this.authService.obtenerToken();
    return new HttpHeaders({
      Authorization: 'Bearer ' + token
    });
  }

  listarBandeja(): Observable<CasoBandejaSupervisor[]> {
    return this.http.get<CasoBandejaSupervisor[]>(`${environment.apiUrl}/jefatura/casos`, {
      headers: this.obtenerCabeceras()
    });
  }

  obtenerDetalle(id: number): Observable<CasoDetalleSupervisor> {
    return this.http.get<CasoDetalleSupervisor>(`${environment.apiUrl}/jefatura/casos/${id}`, {
      headers: this.obtenerCabeceras()
    });
  }

  firmarOficio(id: number): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/jefatura/casos/${id}/firmar`,
      {},
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA02: Devolver a Supervisión
  devolverASupervision(id: number, observaciones: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/jefatura/casos/${id}/devolver`,
      { observaciones },
      { headers: this.obtenerCabeceras() }
    );
  }

  verEvidencia(id: number): Observable<Blob> {
    return this.archivo(id, 'evidencia');
  }

  verSoporteAtencion(id: number): Observable<Blob> {
    return this.archivo(id, 'soporte-atencion');
  }

  verReporteAtencion(id: number): Observable<Blob> {
    return this.archivo(id, 'reporte-atencion');
  }

  verReporteSupervision(id: number): Observable<Blob> {
    return this.archivo(id, 'reporte-supervision');
  }

  verOficio(id: number): Observable<Blob> {
    return this.archivo(id, 'oficio');
  }

  // Pedimos el archivo como "blob" (datos binarios) para abrirlo en una pestaña nueva.
  private archivo(id: number, recurso: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/jefatura/casos/${id}/${recurso}`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }
}
