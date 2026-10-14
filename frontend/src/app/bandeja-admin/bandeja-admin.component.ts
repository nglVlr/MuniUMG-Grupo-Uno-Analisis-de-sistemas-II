import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { SeguimientoCasoComponent } from '../seguimiento-caso/seguimiento-caso.component';
import { MiCuentaComponent } from '../mi-cuenta/mi-cuenta.component';
import { ReportesComponent } from '../reportes/reportes.component';
import { BitacoraComponent } from '../bitacora/bitacora.component';
import {
  AdminUsuarioService,
  CatalogosUsuario,
  GuardarUsuario,
  UsuarioAdmin
} from '../core/admin-usuario.service';
import {
  AdminCatalogoService,
  CatalogoElemento,
  GuardarCatalogo
} from '../core/admin-catalogo.service';

type VistaAdmin = 'menu' | 'usuarios' | 'formulario' | 'estado' | 'catalogos' | 'catalogoForm' | 'seguimiento' | 'cuenta' | 'reportes' | 'bitacora';

@Component({
  selector: 'app-bandeja-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, SeguimientoCasoComponent, MiCuentaComponent, ReportesComponent, BitacoraComponent],
  templateUrl: './bandeja-admin.component.html',
  styleUrl: './bandeja-admin.component.css'
})
export class BandejaAdminComponent {
  vista: VistaAdmin = 'menu';

  usuarios: UsuarioAdmin[] = [];
  usuariosOriginal: UsuarioAdmin[] = [];
  catalogos: CatalogosUsuario = { roles: [], departamentos: [], estados: [] };

  filtroNombre = '';
  filtroRol = '';
  filtroDepartamento = '';

  // Formulario de usuario (CU-10 paso 7)
  editando: UsuarioAdmin | null = null;
  formulario: GuardarUsuario = this.formularioVacio();

  // FA04 y FA08: cambio de estado y reasignación de casos
  usuarioEstado: UsuarioAdmin | null = null;
  idEstadoNuevo: number | null = null;
  mostrarReasignacion = false;
  cuiDestino = '';
  usuariosDestino: UsuarioAdmin[] = [];

  mensaje = '';
  mensajeExito = '';
  cargando = false;
  private temporizadorExito: ReturnType<typeof setTimeout> | null = null;

  // CU-11 Administración de Catálogos
  catalogoActual = 'departamentos';
  elementos: CatalogoElemento[] = [];
  tiposCasoDisponibles: string[] = [];
  elementoEditando: CatalogoElemento | null = null;
  formularioCatalogo: GuardarCatalogo = this.catalogoVacio();

  // El encabezado de la bandeja muestra quien tiene la sesión abierta
  nombreUsuario = '';

  constructor(
    private adminUsuarioService: AdminUsuarioService,
    private adminCatalogoService: AdminCatalogoService,
    private authService: AuthService,
    private router: Router
  ) {
    this.nombreUsuario = this.authService.obtenerNombreUsuario();
  }

  // ---------- CU-10 paso 3: el Admin entra a Mantenimiento de Usuarios ----------
  abrirUsuarios(): void {
    this.vista = 'usuarios';
    this.mensajeExito = '';
    this.cargarUsuarios();
    this.cargarCatalogosUsuario();
  }

  // CU-15: consulta de la bitacora de actividades
  abrirBitacora(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'bitacora';
  }

  // CU-14: reportes estadisticos y trazabilidad
  abrirReportes(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'reportes';
  }

  // CU-12: datos propios del usuario
  abrirMiCuenta(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'cuenta';
  }

  abrirSeguimiento(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.vista = 'seguimiento';
  }

  volverAlMenu(): void {
    this.vista = 'menu';
  }

  cerrarSesion(): void {
    this.authService.cerrarSesionConBitacora();
    this.router.navigate(['']);
  }

  cargarUsuarios(): void {
    this.mensaje = '';
    this.cargando = true;

    this.adminUsuarioService.listarUsuarios().subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.usuarios = resultado;
        this.usuariosOriginal = resultado;
        this.aplicarFiltros();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No tiene permisos para acceder a este recurso');
      }
    });
  }

  private cargarCatalogosUsuario(): void {
    this.adminUsuarioService.obtenerCatalogos().subscribe({
      next: (resultado) => {
        this.catalogos = resultado;
      },
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No tiene permisos para acceder a este recurso');
      }
    });
  }

  // ---------- CU-10 FA02: filtros por nombre, rol o departamento ----------
  aplicarFiltros(): void {
    const nombre = this.filtroNombre.trim().toLowerCase();
    const resultado: UsuarioAdmin[] = [];

    for (let i = 0; i < this.usuariosOriginal.length; i++) {
      const usuario = this.usuariosOriginal[i];
      let cumpleFiltros = true;

      if (nombre && !usuario.nombre.toLowerCase().includes(nombre)
          && !usuario.correo.toLowerCase().includes(nombre)) {
        cumpleFiltros = false;
      }
      if (this.filtroRol && usuario.rol !== this.filtroRol) {
        cumpleFiltros = false;
      }
      if (this.filtroDepartamento && usuario.departamento !== this.filtroDepartamento) {
        cumpleFiltros = false;
      }

      if (cumpleFiltros) {
        resultado.push(usuario);
      }
    }

    this.usuarios = resultado;
  }

  limpiarFiltros(): void {
    this.filtroNombre = '';
    this.filtroRol = '';
    this.filtroDepartamento = '';
    this.usuarios = this.usuariosOriginal;
  }

  // ---------- CU-10 paso 6: Nuevo Usuario ----------
  abrirNuevoUsuario(): void {
    this.mensaje = '';
    this.editando = null;
    this.formulario = this.formularioVacio();
    this.vista = 'formulario';
  }

  // ---------- CU-10 FA03: seleccionar un usuario existente ----------
  abrirEdicion(usuario: UsuarioAdmin): void {
    this.mensaje = '';
    this.editando = usuario;
    this.formulario = {
      cui: usuario.cui,
      nombre: usuario.nombre,
      correo: usuario.correo,
      password: '',
      idRol: usuario.idRol,
      idDepartamento: usuario.idDepartamento,
      idEstado: usuario.idEstado
    };
    this.vista = 'formulario';
  }

  // ---------- CU-10 pasos 10 al 14: Guardar ----------
  guardarUsuario(): void {
    this.mensaje = '';

    // FA06: campos obligatorios incompletos
    if (!this.formulario.nombre.trim() || !this.formulario.correo.trim()
        || !this.formulario.idRol || !this.formulario.idDepartamento || !this.formulario.idEstado) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }
    if (!this.editando && !this.formulario.cui.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;

    const peticion = this.editando
      ? this.adminUsuarioService.editarUsuario(this.editando.cui, this.formulario)
      : this.adminUsuarioService.crearUsuario(this.formulario);

    peticion.subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.vista = 'usuarios';
        this.cargarUsuarios();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  // ---------- CU-10 FA04: cambiar el estado de disponibilidad ----------
  abrirCambioEstado(usuario: UsuarioAdmin): void {
    this.mensaje = '';
    this.usuarioEstado = usuario;
    this.idEstadoNuevo = usuario.idEstado;
    this.mostrarReasignacion = false;
    this.cuiDestino = '';
    this.prepararUsuariosDestino(usuario);
    this.vista = 'estado';
  }

  // cuiDestino llega con valor cuando el Admin ya eligió a quién pasarle los casos
  confirmarCambioEstado(cuiDestino = ''): void {
    if (!this.usuarioEstado || !this.idEstadoNuevo) {
      return;
    }
    this.mensaje = '';
    this.cargando = true;

    this.adminUsuarioService.cambiarEstado(this.usuarioEstado.cui, this.idEstadoNuevo, cuiDestino).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarReasignacion = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.vista = 'usuarios';
        this.cargarUsuarios();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Los datos ingresados no son válidos, o las contraseñas no coinciden.');

        // FA08: el usuario tiene casos pendientes, hay que reasignarlos primero
        if (this.mensaje.startsWith('El usuario tiene casos activos')) {
          this.mostrarReasignacion = true;
        }
      }
    });
  }

  // ---------- CU-10 FA08: reasignar los casos pendientes y continuar ----------
  confirmarReasignacion(): void {
    if (!this.usuarioEstado) {
      return;
    }
    this.mensaje = '';

    if (!this.cuiDestino) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    // El backend traslada los casos y cambia el estado en una sola operación
    this.confirmarCambioEstado(this.cuiDestino);
  }

  cancelarFormulario(): void {
    this.mensaje = '';
    this.vista = 'usuarios';
  }

  // Solo el Técnico tiene casos a su nombre, y se pasan a otro Técnico activo de su
  // mismo departamento, porque el caso se atiende dentro del departamento (CU-04).
  // Las bandejas del Auxiliar, del Supervisor y del Jefe son compartidas, así que esos
  // usuarios nunca llegan a tener casos pendientes que reasignar.
  private prepararUsuariosDestino(usuario: UsuarioAdmin): void {
    const resultado: UsuarioAdmin[] = [];

    for (let i = 0; i < this.usuariosOriginal.length; i++) {
      const candidato = this.usuariosOriginal[i];
      if (candidato.rol === usuario.rol
          && candidato.departamento === usuario.departamento
          && candidato.estado === 'Activo'
          && candidato.cui !== usuario.cui) {
        resultado.push(candidato);
      }
    }
    this.usuariosDestino = resultado;
  }

  // ---------- CU-11 pasos 1 al 4: catálogos y sus elementos ----------
  abrirCatalogos(): void {
    this.vista = 'catalogos';
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargarCatalogo();
    this.cargarTiposCaso();
    this.cargarCatalogosUsuario();
  }

  seleccionarCatalogo(catalogo: string): void {
    this.catalogoActual = catalogo;
    this.cargarCatalogo();
  }

  cargarCatalogo(): void {
    this.mensaje = '';
    this.cargando = true;

    this.adminCatalogoService.listar(this.catalogoActual).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.elementos = resultado;
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'No tiene permisos para acceder a este recurso');
      }
    });
  }

  private cargarTiposCaso(): void {
    this.adminCatalogoService.listarTiposCaso().subscribe({
      next: (resultado) => {
        this.tiposCasoDisponibles = resultado;
      },
      error: () => {
        this.tiposCasoDisponibles = [];
      }
    });
  }

  // ---------- CU-11 paso 5: agregar o editar un elemento ----------
  abrirNuevoElemento(): void {
    this.mensaje = '';
    this.elementoEditando = null;
    this.formularioCatalogo = this.catalogoVacio();
    this.vista = 'catalogoForm';
  }

  abrirEdicionElemento(elemento: CatalogoElemento): void {
    this.mensaje = '';
    this.elementoEditando = elemento;
    this.formularioCatalogo = {
      nombre: elemento.nombre,
      activo: elemento.activo,
      idDepartamento: elemento.idDepartamento,
      tiposCaso: elemento.tiposCaso ? elemento.tiposCaso.slice() : []
    };
    this.vista = 'catalogoForm';
  }

  // FA02: la categoría elige a qué tipos de caso aplica
  alternarTipoCaso(tipo: string): void {
    const posicion = this.formularioCatalogo.tiposCaso.indexOf(tipo);
    if (posicion >= 0) {
      this.formularioCatalogo.tiposCaso.splice(posicion, 1);
    } else {
      this.formularioCatalogo.tiposCaso.push(tipo);
    }
  }

  tieneTipoCaso(tipo: string): boolean {
    return this.formularioCatalogo.tiposCaso.indexOf(tipo) >= 0;
  }

  guardarElemento(): void {
    this.mensaje = '';

    if (!this.formularioCatalogo.nombre.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }
    // FA02: el departamento responsable es obligatorio en las categorías
    if (this.catalogoActual === 'categorias' && !this.formularioCatalogo.idDepartamento) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;

    const peticion = this.elementoEditando
      ? this.adminCatalogoService.editar(this.catalogoActual, this.elementoEditando.id, this.formularioCatalogo)
      : this.adminCatalogoService.crear(this.catalogoActual, this.formularioCatalogo);

    peticion.subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.vista = 'catalogos';
        this.cargarCatalogo();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  // ---------- CU-11 FA01: eliminar solo si el elemento no está en uso ----------
  eliminarElemento(elemento: CatalogoElemento): void {
    this.mensaje = '';
    this.cargando = true;

    this.adminCatalogoService.eliminar(this.catalogoActual, elemento.id).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mostrarMensajeExito(resultado.mensaje);
        this.cargarCatalogo();
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'El elemento no puede eliminarse porque se encuentra en uso; puede inactivarlo.');
      }
    });
  }

  cancelarCatalogo(): void {
    this.mensaje = '';
    this.vista = 'catalogos';
  }

  private catalogoVacio(): GuardarCatalogo {
    return { nombre: '', activo: true, idDepartamento: null, tiposCaso: [] };
  }

  private formularioVacio(): GuardarUsuario {
    return {
      cui: '',
      nombre: '',
      correo: '',
      password: '',
      idRol: null,
      idDepartamento: null,
      idEstado: null
    };
  }

  // Muestra el mensaje de éxito y lo oculta solo a los 10 segundos
  private mostrarMensajeExito(texto: string): void {
    this.mensajeExito = texto;
    if (this.temporizadorExito) {
      clearTimeout(this.temporizadorExito);
    }
    this.temporizadorExito = setTimeout(() => {
      this.mensajeExito = '';
    }, 10000);
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
