import { Routes } from '@angular/router';

import { PortalComponent } from './portal/portal.component';
import { LoginComponent } from './login/login.component';
import { InicioComponent } from './inicio/inicio.component';
import { RecuperarPasswordComponent } from './recuperar-password/recuperar-password.component';
import { RestablecerPasswordComponent } from './reestablecer-password/restablecer-password.component';

import { authGuard } from './guards/auth.guard';

export const routes: Routes = [

  {
    path: '',
    component: PortalComponent
  },

  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'recuperar-password',
    component: RecuperarPasswordComponent
  },
  {
    path: 'restablecer-password',
    component: RestablecerPasswordComponent
  },
  {
    path: 'inicio',
    component: InicioComponent,
    canActivate: [authGuard]
  }

];