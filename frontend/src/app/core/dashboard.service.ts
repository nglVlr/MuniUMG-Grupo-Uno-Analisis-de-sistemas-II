import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { HistorialCaso } from './seguimiento.service';

// CU-19 Dashboard de gráficas: un solo endpoint devuelve todas las series ya listas.

export interface OpcionCatalogo {
  id: number;
  nombre: string;
}

export interface CategoriaDashboard {
  id: number;
  nombre: string;
  idDepartamento: number;
  tiposCaso: string[];
}

export interface UsuarioDashboard {
  cui: string;
  nombre: string;
  rol: string;
  idDepartamento: number | null;
}

export interface CatalogosDashboard {
  departamentos: OpcionCatalogo[];
  categorias: CategoriaDashboard[];
  tiposCaso: string[];
  estados: string[];
  tecnicos: UsuarioDashboard[];
  usuarios: UsuarioDashboard[];
  motivosRechazo: OpcionCatalogo[];
  cargos: string[];
}

export interface Serie {
  nombre: string;
  valores: number[];
}

export interface IndicadorTarjeta {
  clave: string;
  titulo: string;
  valor: number;
  formato: string;
  variacion: number;
  chispa: number[];
}

export interface Conteo {
  etiqueta: string;
  cantidad: number;
}

export interface NodoJerarquia {
  nombre: string;
  valor: number;
  hijos: NodoJerarquia[];
}

export interface EnlaceFlujo {
  origen: string;
  destino: string;
  valor: number;
}

export interface CeldaCalor {
  fila: number;
  columna: number;
  valor: number;
}

export interface MapaCalor {
  filas: string[];
  columnas: string[];
  celdas: CeldaCalor[];
}

export interface CajaTecnico {
  tecnico: string;
  minimo: number;
  cuartilUno: number;
  mediana: number;
  cuartilTres: number;
  maximo: number;
}

export interface PuntoPareto {
  etiqueta: string;
  cantidad: number;
  acumulado: number;
}

export interface PuntoDispersion {
  etiqueta: string;
  x: number;
  y: number;
  tamano: number;
}

export interface PuntoCalendario {
  fecha: string;
  valor: number;
}

export interface FilaTecnico {
  tecnico: string;
  departamento: string;
  enAtencion: number;
  resueltos: number;
  tiempoPromedio: number;
  semaforo: string;
}

export interface RespuestaDashboard {
  metrica: string;
  agrupacion: string;
  forma: string;
  formato: string;
  nombreDesglose: string;
  sinResultados: boolean;
  totalCasos: number;
  categorias: string[];
  series: Serie[];
  seriesDesglosadas: Serie[];
  indicadores: IndicadorTarjeta[];
  embudo: Conteo[];
  sankey: EnlaceFlujo[];
  treemap: NodoJerarquia[];
  sunburst: NodoJerarquia[];
  arbol: NodoJerarquia[];
  histograma: Conteo[];
  distribucionDias: Conteo[];
  mediaDias: number;
  desviacionDias: number;
  boxplot: CajaTecnico[];
  dispersion: PuntoDispersion[];
  calorDiaHora: MapaCalor;
  calorCategoriaMes: MapaCalor;
  calendario: PuntoCalendario[];
  pareto: PuntoPareto[];
  cascada: Conteo[];
  acumuladoAbiertos: Conteo[];
  mesesCombinada: string[];
  recibidosPorMes: number[];
  tiempoPorMes: number[];
  tablaTecnicos: FilaTecnico[];
  rankingCiudadanos: Conteo[];
  rechazadosCiudadanos: Conteo[];
  rechazoTramiteCiudadanos: Conteo[];
  casoTrazabilidad: string;
  trazabilidad: HistorialCaso[];
}

// Los 10 filtros de la pantalla
export interface FiltrosDashboard {
  modoFecha: string;
  desde: string;
  hasta: string;
  idDepartamento: number | null;
  tipoCaso: string;
  idCategoria: number | null;
  estado: string;
  cuiTecnico: string;
  idMotivoRechazo: number | null;
  cargo: string;
  cuiUsuario: string;
  dpiCiudadano: string;
  codigoSeguimiento: string;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
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

  catalogos(): Observable<CatalogosDashboard> {
    return this.http.get<CatalogosDashboard>(`${environment.apiUrl}/dashboard/catalogos`, {
      headers: this.obtenerCabeceras()
    });
  }

  // Solo se envían los filtros que la pantalla tiene visibles y con valor:
  // un filtro oculto se limpia y no viaja al backend
  consultar(metrica: string, agrupacion: string, filtros: FiltrosDashboard,
            enviar: string[]): Observable<RespuestaDashboard> {
    let params = new HttpParams()
      .set('metrica', metrica)
      .set('agrupacion', agrupacion)
      .set('modoFecha', filtros.modoFecha)
      .set('desde', filtros.desde)
      .set('hasta', filtros.hasta);

    if (enviar.indexOf('departamento') >= 0 && filtros.idDepartamento) {
      params = params.set('idDepartamento', String(filtros.idDepartamento));
    }
    if (enviar.indexOf('tipoCaso') >= 0 && filtros.tipoCaso) {
      params = params.set('tipoCaso', filtros.tipoCaso);
    }
    if (enviar.indexOf('categoria') >= 0 && filtros.idCategoria) {
      params = params.set('idCategoria', String(filtros.idCategoria));
    }
    if (enviar.indexOf('estado') >= 0 && filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (enviar.indexOf('tecnico') >= 0 && filtros.cuiTecnico) {
      params = params.set('cuiTecnico', filtros.cuiTecnico);
    }
    if (enviar.indexOf('motivoRechazo') >= 0 && filtros.idMotivoRechazo) {
      params = params.set('idMotivoRechazo', String(filtros.idMotivoRechazo));
    }
    if (enviar.indexOf('cargo') >= 0 && filtros.cargo) {
      params = params.set('cargo', filtros.cargo);
    }
    if (enviar.indexOf('usuario') >= 0 && filtros.cuiUsuario) {
      params = params.set('cuiUsuario', filtros.cuiUsuario);
    }
    if (enviar.indexOf('ciudadano') >= 0 && filtros.dpiCiudadano) {
      params = params.set('dpiCiudadano', filtros.dpiCiudadano);
    }
    if (filtros.codigoSeguimiento) {
      params = params.set('codigoSeguimiento', filtros.codigoSeguimiento);
    }

    return this.http.get<RespuestaDashboard>(`${environment.apiUrl}/dashboard`, {
      headers: this.obtenerCabeceras(),
      params
    });
  }
}
