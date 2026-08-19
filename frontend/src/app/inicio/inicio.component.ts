import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.css'
})
export class InicioComponent {

  nombreUsuario: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario =
      localStorage.getItem('nombreUsuario') || 'Usuario';
  }

  cerrarSesion(): void {

    this.authService.cerrarSesion();

    this.router.navigate(['']);
  }
}