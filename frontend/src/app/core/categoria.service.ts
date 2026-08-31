import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type TipoCaso = 'Queja' | 'Reclamo' | 'Denuncia' | 'Sugerencia';

export interface Categoria {
  id: number;
  nombre: string;
  departamentoResponsable: string;
}

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  constructor(private http: HttpClient) {}

  listar(tipoCaso: TipoCaso): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${environment.apiUrl}/categorias`, { params: { tipoCaso } });
  }
}
