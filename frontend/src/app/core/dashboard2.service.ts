import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';

// Dashboard 2: una llamada por cada gráfica.

// Una barra o un pedazo del pastel
export interface ValorEtiqueta {
  etiqueta: string;
  valor: number;
}

// Un punto de la dispersión
export interface DispersionTecnico {
  tecnico: string;
  cantidadCasos: number;
  diasPromedio: number;
}

// La campana de Gauss
export interface CampanaGauss {
  tramos: ValorEtiqueta[];
  media: number;
  desviacion: number;
}

// Una categoría del selector, con el departamento y los tipos a los que pertenece
export interface Categoria2 {
  id: number;
  nombre: string;
  idDepartamento: number;
  tiposCaso: string[];
}

// Lo que llena los selectores de la pantalla
export interface Filtros2Catalogo {
  departamentos: { id: number; nombre: string }[];
  tiposCaso: string[];
  categorias: Categoria2[];
}

// Los cuatro filtros de la pantalla
export interface Filtros2 {
  desde: string;
  hasta: string;
  idDepartamento: number | null;
  tipoCaso: string;
  idCategoria: number | null;
}

@Injectable({ providedIn: 'root' })
export class Dashboard2Service {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // El backend exige el token del Admin en cada petición
  private cabeceras(): HttpHeaders {
    return new HttpHeaders({
      Authorization: 'Bearer ' + this.authService.obtenerToken()
    });
  }

  // Los cuatro filtros viajan igual en las cuatro llamadas.
  // PARA AGREGAR UN FILTRO: agregarlo a Filtros2 y copiar aquí una de las tres
  // líneas de abajo con el nombre nuevo.
  private parametros(filtros: Filtros2): HttpParams {
    let params = new HttpParams()
      .set('desde', filtros.desde)
      .set('hasta', filtros.hasta);

    if (filtros.idDepartamento) {
      params = params.set('idDepartamento', String(filtros.idDepartamento));
    }
    if (filtros.tipoCaso) {
      params = params.set('tipoCaso', filtros.tipoCaso);
    }
    if (filtros.idCategoria) {
      params = params.set('idCategoria', String(filtros.idCategoria));
    }
    return params;
  }

  // Llena los selectores de departamento, tipo de caso y categoría
  filtros(): Observable<Filtros2Catalogo> {
    return this.http.get<Filtros2Catalogo>(`${environment.apiUrl}/dashboard2/filtros`, {
      headers: this.cabeceras()
    });
  }

  barras(filtros: Filtros2): Observable<ValorEtiqueta[]> {
    return this.http.get<ValorEtiqueta[]>(`${environment.apiUrl}/dashboard2/barras`, {
      headers: this.cabeceras(),
      params: this.parametros(filtros)
    });
  }

  pastel(filtros: Filtros2): Observable<ValorEtiqueta[]> {
    return this.http.get<ValorEtiqueta[]>(`${environment.apiUrl}/dashboard2/pastel`, {
      headers: this.cabeceras(),
      params: this.parametros(filtros)
    });
  }

  dispersion(filtros: Filtros2): Observable<DispersionTecnico[]> {
    return this.http.get<DispersionTecnico[]>(`${environment.apiUrl}/dashboard2/dispersion`, {
      headers: this.cabeceras(),
      params: this.parametros(filtros)
    });
  }

  campana(filtros: Filtros2): Observable<CampanaGauss> {
    return this.http.get<CampanaGauss>(`${environment.apiUrl}/dashboard2/campana`, {
      headers: this.cabeceras(),
      params: this.parametros(filtros)
    });
  }
}
