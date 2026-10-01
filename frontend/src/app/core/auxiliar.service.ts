import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

export interface CasoBandeja {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  estado: string;
}

export interface CasoDetalleAuxiliar {
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

export interface MotivoRechazo {
  id: number;
  motivo: string;
}

export interface RechazoAuxiliar {
  idMotivoRechazo: number;
  comentario: string;
  aplicaAmpliacion: boolean;
}

export interface MensajeRespuesta {
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class AuxiliarService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token del Auxiliar en cada petición.
  // Como todavía no hay un interceptor, lo agregamos a mano en cada llamada.
  private obtenerCabeceras(): HttpHeaders {
    const token = this.authService.obtenerToken();
    return new HttpHeaders({
      Authorization: 'Bearer ' + token
    });
  }

  listarBandeja(): Observable<CasoBandeja[]> {
    return this.http.get<CasoBandeja[]>(`${environment.apiUrl}/auxiliar/casos`, {
      headers: this.obtenerCabeceras()
    });
  }

  obtenerDetalle(id: number): Observable<CasoDetalleAuxiliar> {
    return this.http.get<CasoDetalleAuxiliar>(`${environment.apiUrl}/auxiliar/casos/${id}`, {
      headers: this.obtenerCabeceras()
    });
  }

  listarMotivosRechazo(): Observable<MotivoRechazo[]> {
    return this.http.get<MotivoRechazo[]>(`${environment.apiUrl}/auxiliar/motivos-rechazo`, {
      headers: this.obtenerCabeceras()
    });
  }

  aceptarCaso(id: number): Observable<CasoDetalleAuxiliar> {
    return this.http.post<CasoDetalleAuxiliar>(
      `${environment.apiUrl}/auxiliar/casos/${id}/aceptar`,
      {},
      { headers: this.obtenerCabeceras() }
    );
  }

  asignarTecnico(id: number): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/auxiliar/casos/${id}/asignar-tecnico`,
      {},
      { headers: this.obtenerCabeceras() }
    );
  }

  rechazarCaso(id: number, datos: RechazoAuxiliar): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/auxiliar/casos/${id}/rechazar`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }
   verEvidencia(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/auxiliar/casos/${id}/evidencia`, {
      headers: this.obtenerCabeceras(),
      responseType: 'blob'
    });
  }
}