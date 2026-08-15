import { Routes } from '@angular/router';
import { PortalComponent } from './portal/portal.component';
import { LoginComponent } from './login/login.component';

// CU-00 Portal de Inicio -> CU-02 Registro de Caso / CU-01 Ingreso al Sistema
export const routes: Routes = [
  { path: '', component: PortalComponent },
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: '', pathMatch: 'full' }
];
