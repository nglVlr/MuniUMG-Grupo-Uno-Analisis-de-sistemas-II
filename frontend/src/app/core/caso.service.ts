import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegistroCasoRequest {
  tipoDocumento: 'DPI' | 'NIT' | 'IUSI';
  numeroDocumento: string;
  tipoCaso: 'QUEJA' | 'DENUNCIA' | 'SUGERENCIA';
  idCategoria: number;
  direccionProblema: string;
  descripcion: string;
}

@Injectable({ providedIn: 'root' })
export class CasoService {
  constructor(private http: HttpClient) {}

  // CU-02 paso 4/5: busca al ciudadano por su documento en la base precargada
  buscarCiudadano(tipoDocumento: string, numeroDocumento: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/casos/ciudadano`, { params: { tipoDocumento, numeroDocumento } });
  }

  registrar(datos: RegistroCasoRequest): Observable<any> {
    return this.http.post(`${environment.apiUrl}/casos`, datos);
  }

  // CU-03: consulta publica de estado
  consultarPorCodigo(codigo: string): Observable<any> {
    return this.http.get(`${environment.apiUrl}/casos/seguimiento/${codigo}`);
  }
}
