import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Categoria, CategoriaService, TipoCaso } from '../core/categoria.service';
import { CasoService, CiudadanoVerificado, ConsultaCasoRespuesta } from '../core/caso.service';
import { CiudadanoSesionService } from '../core/ciudadano-sesion.service';

type VistaBandeja = 'menus' | 'formulario' | 'exito' | 'consulta' | 'consulta-resultado';

@Component({
  selector: 'app-bandeja',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bandeja.component.html',
  styleUrl: './bandeja.component.css'
})
export class BandejaComponent implements OnInit {
  vista: VistaBandeja = 'menus';
  ciudadano: CiudadanoVerificado | null = null;
  mensaje = '';
  mensajeExito = '';
  cargando = false;

  readonly tiposCaso: { valor: TipoCaso; descripcion: string }[] = [
    { valor: 'Queja', descripcion: 'Inconformidad presentada por un ciudadano sobre un servicio municipal.' },
    { valor: 'Reclamo', descripcion: 'Inconformidad presentada por un ciudadano' },
    { valor: 'Denuncia', descripcion: 'Señalamiento de una irregularidad o falta relacionada con la gestión municipal.' },
    { valor: 'Sugerencia', descripcion: 'Propuesta de mejora sobre un servicio municipal.' }
  ];

  tipoCaso: TipoCaso | '' = '';
  categorias: Categoria[] = [];
  idCategoria: number | null = null;
  departamentoResponsable = '';
  direccionProblema = '';
  descripcion = '';
  evidencia: File | null = null;

  codigoConsulta = '';
  resultadoConsulta: ConsultaCasoRespuesta | null = null;

  constructor(
    private sesion: CiudadanoSesionService,
    private casoService: CasoService,
    private categoriaService: CategoriaService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.ciudadano = this.sesion.obtener();
    if (!this.ciudadano) {
      this.router.navigate(['/login']);
    }
  }

  ingresarCaso(): void {
    this.limpiarFormulario();
    this.vista = 'formulario';
  }

  consultarCaso(): void {
    this.mensaje = '';
    this.codigoConsulta = '';
    this.resultadoConsulta = null;
    this.vista = 'consulta';
  }

  cancelarConsulta(): void {
    this.mensaje = '';
    this.codigoConsulta = '';
    this.resultadoConsulta = null;
    this.vista = 'menus';
  }

  regresarConsulta(): void {
    this.resultadoConsulta = null;
    this.mensaje = '';
    this.vista = 'consulta';
  }

  ampliarCaso(): void {
  }

  cancelar(): void {
    this.limpiarFormulario();
    this.vista = 'menus';
  }

  cerrarSesion(): void {
    const token = this.ciudadano?.tokenSesion;
    this.sesion.limpiar();
    if (token) {
      this.casoService.cerrarSesionCiudadano(token).subscribe({ error: () => undefined });
    }
    this.router.navigate(['/']);
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
    const archivo = input.files && input.files.length > 0 ? input.files[0] : null;
    this.mensaje = '';
    this.evidencia = null;
    if (!archivo) {
      return;
    }
    const esPdf = archivo.type === 'application/pdf' || archivo.name.toLowerCase().endsWith('.pdf');
    if (archivo.size === 0) {
      input.value = '';
      this.mensaje = 'El archivo no debe estar vacío.';
      return;
    }
    if (archivo.size > 10 * 1024 * 1024) {
      input.value = '';
      this.mensaje = 'El archivo adjunto supera el tamaño máximo permitido de 10MB.';
      return;
    }
    if (!esPdf) {
      input.value = '';
      this.mensaje = 'El archivo debe ser un PDF.';
      return;
    }
    this.evidencia = archivo;
  }

  registrar(): void {
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
        this.mensajeExito = respuesta.mensaje;
        this.vista = 'exito';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeError(error, 'Debe completar todos los campos obligatorios del formulario.');
      }
    });
  }

  enviarConsulta(): void {
    this.mensaje = '';
    const codigo = this.codigoConsulta.trim();
    if (!/^MG-\d{4}-\d{4}$/.test(codigo)) {
      this.mensaje = 'El código es inválido, intente de nuevo.';
      return;
    }
    this.cargando = true;
    this.casoService.consultarEstado(codigo).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.resultadoConsulta = resultado;
        this.vista = 'consulta-resultado';
      },
      error: () => {
        this.cargando = false;
        this.mensaje = 'El código es inválido, intente de nuevo.';
        this.vista = 'consulta';
      }
    });
  }

  private limpiarFormulario(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.cargando = false;
    this.tipoCaso = '';
    this.categorias = [];
    this.idCategoria = null;
    this.departamentoResponsable = '';
    this.direccionProblema = '';
    this.descripcion = '';
    this.evidencia = null;
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
