import { Routes } from '@angular/router';

import { PortalComponent } from './portal/portal.component';
import { LoginComponent } from './login/login.component';
import { InicioComponent } from './inicio/inicio.component';
import { BandejaComponent } from './bandeja/bandeja.component';
import { BandejaAuxiliarComponent } from './bandeja-auxiliar/bandeja-auxiliar.component';
import { BandejaTecnicoComponent } from './bandeja-tecnico/bandeja-tecnico.component';
import { BandejaSupervisorComponent } from './bandeja-supervisor/bandeja-supervisor.component';
import { BandejaJefaturaComponent } from './bandeja-jefatura/bandeja-jefatura.component';
import { BandejaAdminComponent } from './bandeja-admin/bandeja-admin.component';
import { RecuperarPasswordComponent } from './recuperar-password/recuperar-password.component';
import { RestablecerPasswordComponent } from './reestablecer-password/restablecer-password.component';

import { authGuard } from './guards/auth.guard';
import { ciudadanoGuard } from './guards/ciudadano.guard';

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
  },
  {
    path: 'bandeja-auxiliar',
    component: BandejaAuxiliarComponent,
    canActivate: [authGuard]
  },
  {
    path: 'bandeja-tecnico',
    component: BandejaTecnicoComponent,
    canActivate: [authGuard]
  },
  {
    path: 'bandeja-supervisor',
    component: BandejaSupervisorComponent,
    canActivate: [authGuard]
  },
  {
    path: 'bandeja-jefatura',
    component: BandejaJefaturaComponent,
    canActivate: [authGuard]
  },
  {
    path: 'bandeja-admin',
    component: BandejaAdminComponent,
    canActivate: [authGuard]
  },
  {
    path: 'bandeja',
    component: BandejaComponent,
    canActivate: [ciudadanoGuard]
  }

];
