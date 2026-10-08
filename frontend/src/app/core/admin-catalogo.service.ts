import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { MensajeRespuesta } from './admin-usuario.service';

export interface CatalogoElemento {
  id: number;
  nombre: string;
  activo: boolean;
  idDepartamento: number | null;
  departamento: string | null;
  tiposCaso: string[] | null;
  enUso: boolean;
}

export interface GuardarCatalogo {
  nombre: string;
  activo: boolean;
  idDepartamento: number | null;
  tiposCaso: string[];
}

@Injectable({ providedIn: 'root' })
export class AdminCatalogoService {
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

  listar(catalogo: string): Observable<CatalogoElemento[]> {
    return this.http.get<CatalogoElemento[]>(`${environment.apiUrl}/admin/catalogos/${catalogo}`, {
      headers: this.obtenerCabeceras()
    });
  }

  listarTiposCaso(): Observable<string[]> {
    return this.http.get<string[]>(`${environment.apiUrl}/admin/catalogos/tipos-caso`, {
      headers: this.obtenerCabeceras()
    });
  }

  crear(catalogo: string, datos: GuardarCatalogo): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(
      `${environment.apiUrl}/admin/catalogos/${catalogo}`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }

  editar(catalogo: string, id: number, datos: GuardarCatalogo): Observable<MensajeRespuesta> {
    return this.http.put<MensajeRespuesta>(
      `${environment.apiUrl}/admin/catalogos/${catalogo}/${id}`,
      datos,
      { headers: this.obtenerCabeceras() }
    );
  }

  // FA01: el backend rechaza el borrado si el elemento está en uso
  eliminar(catalogo: string, id: number): Observable<MensajeRespuesta> {
    return this.http.delete<MensajeRespuesta>(
      `${environment.apiUrl}/admin/catalogos/${catalogo}/${id}`,
      { headers: this.obtenerCabeceras() }
    );
  }
}
