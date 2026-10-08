import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Categoria, CategoriaService, TipoCaso } from '../core/categoria.service';
import {
  CasoService,
  CasoAmpliacionDetalle,
  CasoEnAmpliacion,
  CiudadanoVerificado,
  ConsultaCasoRespuesta
} from '../core/caso.service';
import { CiudadanoSesionService } from '../core/ciudadano-sesion.service';

type VistaBandeja = 'menus' | 'formulario' | 'exito' | 'consulta' | 'consulta-resultado' | 'ampliacion-lista' | 'ampliacion-detalle';

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
  // Vista previa de la fotografía adjunta, para confirmar que es la correcta
  evidenciaPrevia: string | null = null;

  // CU-18 Ampliacion de casos
  casosEnAmpliacion: CasoEnAmpliacion[] = [];
  casoAmpliacion: CasoAmpliacionDetalle | null = null;
  informacionAmpliacion = '';
  evidenciaAmpliacion: File | null = null;
  evidenciaAmpliacionPrevia: string | null = null;

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

  // ---------- CU-18 pasos 4 y 5: casos que requieren ampliacion ----------
  ampliarCaso(): void {
    this.mensaje = '';
    this.mensajeExito = '';
    this.casosEnAmpliacion = [];
    this.casoAmpliacion = null;
    this.vista = 'ampliacion-lista';

    const token = this.ciudadano?.tokenSesion;
    if (!token) {
      this.router.navigate(['/login']);
      return;
    }

    this.cargando = true;
    this.casoService.listarCasosEnAmpliacion(token).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.casosEnAmpliacion = resultado;
      },
      error: (error) => {
        this.cargando = false;
        // FA02: no existen casos que requieran ampliacion
        this.mensaje = this.mensajeErrorAmpliacion(error,
          'No existen casos que requieran ampliación para el número de identificación ingresado.');
      }
    });
  }

  // ---------- CU-18 pasos 6 y 7: detalle e instrucciones del Auxiliar ----------
  verCasoEnAmpliacion(id: number): void {
    const token = this.ciudadano?.tokenSesion;
    if (!token) {
      return;
    }
    this.mensaje = '';
    this.informacionAmpliacion = '';
    this.evidenciaAmpliacion = null;
    this.limpiarPrevia('ampliacion');
    this.cargando = true;

    this.casoService.obtenerCasoEnAmpliacion(id, token).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.casoAmpliacion = resultado;
        this.vista = 'ampliacion-detalle';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeErrorAmpliacion(error, 'El código es inválido, intente de nuevo.');
      }
    });
  }

  seleccionarEvidenciaAmpliacion(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files && entrada.files.length > 0 ? entrada.files[0] : null;
    this.mensaje = '';
    this.evidenciaAmpliacion = null;
    this.limpiarPrevia('ampliacion');
    if (!archivo) {
      return;
    }

    const error = this.validarEvidencia(archivo);
    if (error) {
      entrada.value = '';
      this.mensaje = error;
      return;
    }

    this.evidenciaAmpliacion = archivo;
    this.evidenciaAmpliacionPrevia = this.previaDeImagen(archivo);
  }

  // ---------- CU-18 pasos 9 al 14: Reenviar Caso ----------
  reenviarCaso(): void {
    const token = this.ciudadano?.tokenSesion;
    if (!this.casoAmpliacion || !token) {
      return;
    }
    this.mensaje = '';

    // FA04: la informacion solicitada es obligatoria
    if (!this.informacionAmpliacion.trim()) {
      this.mensaje = 'Debe completar la información obligatoria solicitada.';
      return;
    }

    this.cargando = true;
    this.casoService.reenviarCaso(
      this.casoAmpliacion.id,
      token,
      this.informacionAmpliacion.trim(),
      this.evidenciaAmpliacion
    ).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.mensajeExito = resultado.mensaje;
        this.casoAmpliacion = null;
        this.casosEnAmpliacion = [];
        this.vista = 'exito';
      },
      error: (error) => {
        this.cargando = false;
        this.mensaje = this.mensajeErrorAmpliacion(error, 'Debe completar la información obligatoria solicitada.');
      }
    });
  }

  volverAlListadoAmpliacion(): void {
    this.mensaje = '';
    this.casoAmpliacion = null;
    this.vista = 'ampliacion-lista';
  }

  private mensajeErrorAmpliacion(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
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
    this.limpiarPrevia('registro');
    if (!archivo) {
      return;
    }

    const error = this.validarEvidencia(archivo);
    if (error) {
      input.value = '';
      this.mensaje = error;
      return;
    }

    this.evidencia = archivo;
    this.evidenciaPrevia = this.previaDeImagen(archivo);
  }

  // ---------- Evidencia: se acepta el documento en PDF o una fotografía ----------

  // Devuelve el mensaje de error, o cadena vacía si el archivo es válido
  private validarEvidencia(archivo: File): string {
    if (archivo.size === 0) {
      return 'El archivo no debe estar vacío.';
    }
    if (archivo.size > 10 * 1024 * 1024) {
      return 'El archivo adjunto supera el tamaño máximo permitido de 10MB.';
    }
    if (!this.esPdf(archivo) && !this.esImagen(archivo)) {
      return 'El archivo debe ser un PDF o una imagen (JPG, PNG o WEBP).';
    }
    return '';
  }

  private esPdf(archivo: File): boolean {
    return archivo.type === 'application/pdf' || archivo.name.toLowerCase().endsWith('.pdf');
  }

  private esImagen(archivo: File): boolean {
    const tipos = ['image/jpeg', 'image/png', 'image/webp'];
    const extensiones = ['.jpg', '.jpeg', '.png', '.webp'];
    const nombre = archivo.name.toLowerCase();
    return tipos.includes(archivo.type) || extensiones.some((ext) => nombre.endsWith(ext));
  }

  // Solo las fotografías se pueden mostrar antes de enviarlas
  private previaDeImagen(archivo: File): string | null {
    return this.esImagen(archivo) ? window.URL.createObjectURL(archivo) : null;
  }

  private limpiarPrevia(donde: 'registro' | 'ampliacion'): void {
    const url = donde === 'registro' ? this.evidenciaPrevia : this.evidenciaAmpliacionPrevia;
    if (url) {
      window.URL.revokeObjectURL(url);
    }
    if (donde === 'registro') {
      this.evidenciaPrevia = null;
    } else {
      this.evidenciaAmpliacionPrevia = null;
    }
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
    this.limpiarPrevia('registro');
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
