import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

export interface CasoBandejaTecnico {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  estado: string;
}

export interface CasoDetalleTecnico {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  descripcion: string;
  direccion: string;
  estado: string;
  tieneEvidencia: boolean;
}

export interface Actividad {
  codigoSeguimiento: string;
  codigoActividad: number;
  detalle: string;
  fecha: string;
}

export interface MensajeRespuesta {
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class TecnicoService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token del Técnico en cada petición.
  // Como todavía no hay un interceptor, lo agregamos a mano en cada llamada.
  private obtenerCabeceras(): HttpHeaders {
    const token = this.authService.obtenerToken();
    return new HttpHeaders({
      Authorization: 'Bearer ' + token
    });
  }

  listarCasosAsignados(): Observable<CasoBandejaTecnico[]> {
    return this.http.get<CasoBandejaTecnico[]>(`${environment.apiUrl}/tecnico/casos`, {
      headers: this.obtenerCabeceras()
    });
  }

  obtenerDetalle(id: number): Observable<CasoDetalleTecnico> {
    return this.http.get<CasoDetalleTecnico>(`${environment.apiUrl}/tecnico/casos/${id}`, {
      headers: this.obtenerCabeceras()
    });
  }

  listarActividades(id: number): Observable<Actividad[]> {
    return this.http.get<Actividad[]>(`${environment.apiUrl}/tecnico/casos/${id}/actividades`, {
      headers: this.obtenerCabeceras()
    });
  }

  registrarActividad(id: number, detalle: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/tecnico/casos/${id}/actividades`,
      { detalle },
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA02: Finalizar caso. El soporte en PDF es obligatorio, por eso va como multipart.
  finalizarAtencion(id: number, detalle: string, soporte: File | null): Observable<MensajeRespuesta> {
    const datos = new FormData();
    datos.append('detalle', detalle);
    if (soporte) {
      datos.append('soporte', soporte);
    }

    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/tecnico/casos/${id}/finalizar`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }

  // Pedimos el archivo como "blob" (datos binarios) para abrirlo en una pestaña nueva.
  verEvidencia(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/tecnico/casos/${id}/evidencia`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }
}
