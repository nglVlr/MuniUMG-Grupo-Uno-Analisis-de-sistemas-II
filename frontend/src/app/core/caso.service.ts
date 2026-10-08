import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TipoCaso } from './categoria.service';

export interface CiudadanoVerificado {
  dpi: string;
  nombre: string;
  telefono: string;
  correo: string;
  tokenSesion: string;
}

export interface RegistroCasoRespuesta {
  numeroCaso: number;
  codigoSeguimiento: string;
  mensaje: string;
}

export interface MensajeRespuesta {
  mensaje: string;
}

// CU-18: caso del ciudadano que requiere ampliacion
export interface CasoEnAmpliacion {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  estado: string;
}

export interface CasoAmpliacionDetalle {
  id: number;
  codigoSeguimiento: string;
  fechaIngreso: string;
  tipoCaso: string;
  categoria: string;
  descripcion: string;
  direccion: string;
  estado: string;
  motivoRechazo: string;
  instruccionesAmpliacion: string;
  informacionAmpliacion: string;
}

export interface ConsultaCasoRespuesta {
  codigoSeguimiento: string;
  estado: string;
  fechaRegistro: string;
  departamentoResponsable: string;
  descripcion: string;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class CasoService {
  constructor(private http: HttpClient) {}

  solicitarVerificacion(dpi: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(`${environment.apiUrl}/casos/verificacion`, { dpi });
  }

  confirmarVerificacion(dpi: string, codigo: string): Observable<CiudadanoVerificado> {
    return this.http.post<CiudadanoVerificado>(`${environment.apiUrl}/casos/verificacion/confirmar`, { dpi, codigo });
  }

  cerrarSesionCiudadano(tokenSesion: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(`${environment.apiUrl}/casos/verificacion/cerrar`, { tokenSesion });
  }

  registrar(
    tokenSesion: string,
    tipoCaso: TipoCaso,
    idCategoria: number,
    direccionProblema: string,
    descripcion: string,
    evidencia?: File
  ): Observable<RegistroCasoRespuesta> {
    const datos = new FormData();
    datos.append('tokenSesion', tokenSesion);
    datos.append('tipoCaso', tipoCaso);
    datos.append('idCategoria', String(idCategoria));
    datos.append('direccionProblema', direccionProblema);
    datos.append('descripcion', descripcion);
    if (evidencia) {
      datos.append('evidencia', evidencia, evidencia.name);
    }
    return this.http.post<RegistroCasoRespuesta>(`${environment.apiUrl}/casos`, datos);
  }
  
  consultarEstado(codigoSeguimiento: string): Observable<ConsultaCasoRespuesta> {
    return this.http.get<ConsultaCasoRespuesta>(`${environment.apiUrl}/casos/consulta`, {
      params: { codigoSeguimiento }
    });
  }


  // ---------- CU-18 Ampliacion de casos ----------
  listarCasosEnAmpliacion(tokenSesion: string): Observable<CasoEnAmpliacion[]> {
    return this.http.get<CasoEnAmpliacion[]>(`${environment.apiUrl}/casos/ampliacion`, {
      params: { tokenSesion }
    });
  }

  obtenerCasoEnAmpliacion(id: number, tokenSesion: string): Observable<CasoAmpliacionDetalle> {
    return this.http.get<CasoAmpliacionDetalle>(`${environment.apiUrl}/casos/ampliacion/${id}`, {
      params: { tokenSesion }
    });
  }

  reenviarCaso(id: number, tokenSesion: string, informacion: string, evidencia: File | null): Observable<MensajeRespuesta> {
    const datos = new FormData();
    if (evidencia) {
      datos.append('evidencia', evidencia);
    }
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/casos/ampliacion/${id}`,
      datos,
      { params: { tokenSesion, informacion } }
    );
  }
}
