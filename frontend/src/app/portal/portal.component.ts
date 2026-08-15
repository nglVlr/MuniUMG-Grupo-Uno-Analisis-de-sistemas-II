import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CategoriaService, Categoria } from '../core/categoria.service';
import { CasoService, RegistroCasoRequest } from '../core/caso.service';

/**
 * CU-00 Portal de Inicio + CU-02 Registro de Caso (simplificado en una sola
 * pantalla para mantener el ejemplo simple). El ciudadano elige primero el
 * tipo de caso, y en base a eso se muestra el listado de categorias
 * correspondiente (RN-CU02-02/03).
 */
@Component({
  selector: 'app-portal',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './portal.component.html'
})
export class PortalComponent {
  tipoCaso: 'QUEJA' | 'DENUNCIA' | 'SUGERENCIA' | '' = '';
  categorias: Categoria[] = [];

  form: Partial<RegistroCasoRequest> = { tipoDocumento: 'DPI' };
  ciudadanoEncontrado: any = null;
  mensaje = '';
  codigoSeguimiento = '';

  constructor(private categoriaService: CategoriaService, private casoService: CasoService) {}

  seleccionarTipo(tipo: 'QUEJA' | 'DENUNCIA' | 'SUGERENCIA'): void {
    this.tipoCaso = tipo;
    this.form.tipoCaso = tipo;
    this.categoriaService.listar(tipo).subscribe((cats) => (this.categorias = cats));
  }

  buscarCiudadano(): void {
    if (!this.form.numeroDocumento) return;
    this.casoService.buscarCiudadano(this.form.tipoDocumento!, this.form.numeroDocumento).subscribe({
      next: (c) => (this.ciudadanoEncontrado = c),
      error: () => (this.mensaje = 'No se encontró un ciudadano registrado con ese número de identificación.') // AN02 CU-02 No.8
    });
  }

  registrar(): void {
    this.casoService.registrar(this.form as RegistroCasoRequest).subscribe({
      next: (caso) => {
        this.codigoSeguimiento = caso.codigoSeguimiento;
        this.mensaje = `Su caso fue registrado exitosamente. Código de seguimiento: ${caso.codigoSeguimiento}`; // AN01 CU-02 No.1
      },
      error: () => (this.mensaje = 'Debe completar todos los campos obligatorios del formulario.') // AN02 CU-02 No.1
    });
  }
}
