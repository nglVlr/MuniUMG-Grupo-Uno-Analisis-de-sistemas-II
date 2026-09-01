import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CategoriaService, Categoria, TipoCaso } from '../core/categoria.service';
import { CasoService, CiudadanoVerificado } from '../core/caso.service';

type VistaPortal = 'inicio' | 'identificacion' | 'codigo' | 'formulario' | 'exito';

@Component({
  selector: 'app-portal',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './portal.component.html',
  styleUrl: './portal.component.css'
})
export class PortalComponent {
  vista: VistaPortal = 'inicio';
  dpi = '';
  codigoVerificacion = '';
  mensaje = '';
  mensajeExito = '';
  cargando = false;

  ciudadano: CiudadanoVerificado | null = null;
  tiposCaso: TipoCaso[] = ['Queja', 'Reclamo', 'Denuncia', 'Sugerencia'];
  tipoCaso: TipoCaso | '' = '';
  categorias: Categoria[] = [];
  idCategoria: number | null = null;
  departamentoResponsable = '';
  direccionProblema = '';
  descripcion = '';
  evidencia: File | null = null;
  codigoSeguimiento = '';

  constructor(
    private categoriaService: CategoriaService,
    private casoService: CasoService
  ) {}

  ingresarQueja(): void {
    this.reiniciarFormulario();
    this.vista = 'identificacion';
    this.desplazarARegistro();
  }

  consultarEstadoCaso(): void {
    // CU-03: stub
  }

  modificarQueja(): void {
    // CU-18: stub
  }

  volverAlPortal(): void {
    this.reiniciarFormulario();
    this.vista = 'inicio';
  }

  enviarDpi(): void {
    this.mensaje = '';
    const dpi = this.dpi.trim();
    if (!/^\d{13}$/.test(dpi)) {
      this.mensaje = 'El número de identificación ingresado no tiene un formato válido.';
      return;
    }
    this.cargando = true;
    this.casoService.solicitarVerificacion(dpi).subscribe({
      next: () => {
        this.cargando = false;
        this.vista = 'codigo';
        this.mensaje = '';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'El número de identificación ingresado no tiene un formato válido.');
      }
    });
  }

  enviarCodigo(): void {
    this.mensaje = '';
    if (!this.codigoVerificacion.trim()) {
      this.mensaje = 'El código es inválido, intente de nuevo';
      return;
    }
    this.cargando = true;
    this.casoService.confirmarVerificacion(this.dpi.trim(), this.codigoVerificacion.trim()).subscribe({
      next: (ciudadano) => {
        this.cargando = false;
        this.ciudadano = ciudadano;
        this.vista = 'formulario';
        this.mensaje = '';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'El código es inválido, intente de nuevo');
      }
    });
  }

  onTipoCasoChange(): void {
    this.idCategoria = null;
    this.departamentoResponsable = '';
    this.categorias = [];
    if (!this.tipoCaso) {
      return;
    }
    this.categoriaService.listar(this.tipoCaso).subscribe({
      next: (categorias) => (this.categorias = categorias),
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'Debe seleccionar el tipo de caso (queja, reclamo, denuncia o sugerencia).');
      }
    });
  }

  onCategoriaChange(): void {
    const categoria = this.categorias.find((item) => item.id === Number(this.idCategoria));
    this.departamentoResponsable = categoria?.departamentoResponsable ?? '';
  }

  onEvidenciaSeleccionada(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    this.evidencia = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  registrarCaso(): void {
    this.mensaje = '';
    if (!this.ciudadano) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }
    if (!this.tipoCaso) {
      this.mensaje = 'Debe seleccionar el tipo de caso (queja, reclamo, denuncia o sugerencia).';
      return;
    }
    if (!this.idCategoria) {
      this.mensaje = 'Debe seleccionar la categoría del problema reportado.';
      return;
    }
    if (!this.direccionProblema.trim()) {
      this.mensaje = 'Debe indicar la dirección o ubicación del problema reportado.';
      return;
    }
    if (!this.descripcion.trim()) {
      this.mensaje = 'Debe completar todos los campos obligatorios del formulario.';
      return;
    }

    this.cargando = true;
    this.casoService.registrar(
      this.ciudadano.tokenSesion,
      this.tipoCaso,
      Number(this.idCategoria),
      this.direccionProblema.trim(),
      this.descripcion.trim(),
      this.evidencia ?? undefined
    ).subscribe({
      next: (respuesta) => {
        this.cargando = false;
        this.codigoSeguimiento = respuesta.codigoSeguimiento;
        this.mensajeExito = respuesta.mensaje;
        this.vista = 'exito';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }

  private reiniciarFormulario(): void {
    this.dpi = '';
    this.codigoVerificacion = '';
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = false;
    this.ciudadano = null;
    this.tipoCaso = '';
    this.categorias = [];
    this.idCategoria = null;
    this.departamentoResponsable = '';
    this.direccionProblema = '';
    this.descripcion = '';
    this.evidencia = null;
    this.codigoSeguimiento = '';
  }

  private desplazarARegistro(): void {
    setTimeout(() => {
      document.getElementById('registro')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  }
}
