import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { CasoService } from '../core/caso.service';
import { CiudadanoSesionService } from '../core/ciudadano-sesion.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit{

  tipoUsuario = '';
  usuario = '';
  password = '';

  mostrarPassword = false;

  error = '';
  aviso = '';
  cargando = false;
  codigoEnviado = false;
  private dpiCodigoEnviado = '';

  constructor(
    private authService: AuthService,
    private casoService: CasoService,
    private ciudadanoSesion: CiudadanoSesionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.cerrarSesion();
  }

  togglePassword(): void {
    this.mostrarPassword = !this.mostrarPassword;
  }

  alCambiarTipo(): void {
    this.error = '';
    this.aviso = '';
    this.password = '';
    this.mostrarPassword = false;
    this.dpiCodigoEnviado = '';
    this.codigoEnviado = false;
  }

  alSalirUsuario(): void {
    if (this.tipoUsuario !== 'Ciudadano') {
      return;
    }
    this.enviarCodigoCiudadano(this.usuario.trim());
  }

  acceder(): void {
    this.error = '';

    if (this.tipoUsuario === 'Ciudadano') {
      const dpi = this.usuario.trim();
      const codigo = this.password.trim();
      if (!/^\d{13}$/.test(dpi)) {
        this.error = 'El número de identificación ingresado no tiene un formato válido.';
        return;
      }
      if (!codigo) {
        this.enviarCodigoCiudadano(dpi, true);
        return;
      }
      this.cargando = true;
      this.casoService.confirmarVerificacion(dpi, codigo).subscribe({
        next: (ciudadano) => {
          this.ciudadanoSesion.guardar(ciudadano);
          this.cargando = false;
          this.router.navigate(['/bandeja']);
        },
        error: (error) => this.mostrarFalloIngreso(error)
      });
      return;
    }

    this.cargando = true;

    this.authService.iniciarSesion(this.usuario.trim(), this.password).subscribe({
      next: (respuesta) => {
        this.authService.guardarSesion(respuesta);
        this.cargando = false;

        // Rol 2 = Auxiliar (según la tabla "rol"): lo mandamos directo a su
        // propia bandeja en vez de la pantalla de inicio genérica.
        if (respuesta.rol === 1) {
          this.router.navigate(['/bandeja-admin']);
        } else if (respuesta.rol === 2) {
          this.router.navigate(['/bandeja-auxiliar']);
        } else if (respuesta.rol === 3) {
          this.router.navigate(['/bandeja-tecnico']);
        } else if (respuesta.rol === 4) {
          this.router.navigate(['/bandeja-supervisor']);
        } else if (respuesta.rol === 5) {
          this.router.navigate(['/bandeja-jefatura']);
        } else {
          this.router.navigate(['/inicio']);
        }
      },
      error: (error) => this.mostrarFalloIngreso(error)
    });
  }

  private enviarCodigoCiudadano(dpi: string, forzar = false): void {
    this.error = '';
    if (!/^\d{13}$/.test(dpi)) {
      this.dpiCodigoEnviado = '';
      this.codigoEnviado = false;
      this.error = 'El número de identificación ingresado no tiene un formato válido.';
      return;
    }
    if (!forzar && dpi === this.dpiCodigoEnviado) {
      return;
    }
    this.cargando = true;
    this.aviso = '';
    this.casoService.solicitarVerificacion(dpi).subscribe({
      next: () => {
        this.cargando = false;
        this.dpiCodigoEnviado = dpi;
        this.codigoEnviado = true;
        this.aviso = 'Se envió el código de verificación al correo asociado al DPI, debe ingresarlo en el campo contraseña para ingresar al sistema.';
      },
      error: (error) => {
        this.cargando = false;
        this.dpiCodigoEnviado = '';
        this.codigoEnviado = false;
        this.error = error?.error?.mensaje ?? 'El número de identificación ingresado no tiene un formato válido.';
      }
    });
  }

  private mostrarFalloIngreso(error: { status?: number; error?: { mensaje?: string } }): void {
    this.cargando = false;
    if (!error?.status) {
      this.error = 'No fue posible conectar con el servidor.';
      return;
    }
    const mensaje = error.error?.mensaje ?? '';
    if (mensaje === 'El usuario se encuentra inactivo'
        || mensaje === 'El usuario se encuentra bloqueado, contacte al administrador') {
      this.error = mensaje;
      return;
    }
    this.error = 'Usuario o contraseña incorrectos.';
  }
}
