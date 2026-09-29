import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CiudadanoSesionService } from '../core/ciudadano-sesion.service';

export const ciudadanoGuard: CanActivateFn = () => {
  const sesion = inject(CiudadanoSesionService);
  const router = inject(Router);

  if (sesion.haySesion()) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
