import { Injectable } from '@angular/core';
import { CiudadanoVerificado } from './caso.service';

const CLAVE = 'ciudadanoSesion';

@Injectable({ providedIn: 'root' })
export class CiudadanoSesionService {
  guardar(ciudadano: CiudadanoVerificado): void {
    sessionStorage.setItem(CLAVE, JSON.stringify(ciudadano));
  }

  obtener(): CiudadanoVerificado | null {
    const texto = sessionStorage.getItem(CLAVE);
    if (!texto) {
      return null;
    }
    try {
      return JSON.parse(texto) as CiudadanoVerificado;
    } catch {
      return null;
    }
  }

  limpiar(): void {
    sessionStorage.removeItem(CLAVE);
  }

  haySesion(): boolean {
    return this.obtener()?.tokenSesion != null && this.obtener()!.tokenSesion !== '';
  }
}
