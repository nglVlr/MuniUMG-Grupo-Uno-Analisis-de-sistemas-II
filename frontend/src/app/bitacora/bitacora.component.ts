import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BitacoraService, FiltrosBitacora, PaginaBitacora } from '../core/bitacora.service';

// CU-15 Bitácora de actividades: consulta de solo lectura para el Admin.
@Component({
  selector: 'app-bitacora',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bitacora.component.html',
  styleUrl: './bitacora.component.css'
})
export class BitacoraComponent implements OnInit {
  entidades = ['Caso', 'Usuario', 'Catálogo', 'Sesión'];
  roles = ['Admin', 'Auxiliar', 'Tecnico', 'Supervisor', 'Jefe', 'Ciudadano'];
  acciones: string[] = [];

  filtros: FiltrosBitacora = {
    desde: '',
    hasta: '',
    usuario: '',
    accion: '',
    entidad: '',
    rol: '',
    pagina: 0
  };

  resultado: PaginaBitacora | null = null;

  mensaje = '';
  cargando = false;

  constructor(private bitacoraService: BitacoraService) {}

  ngOnInit(): void {
    // Por defecto, el mes en curso
    const hoy = new Date();
    this.filtros.hasta = this.comoTexto(hoy);
    this.filtros.desde = this.comoTexto(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
    this.cargarAcciones();
    this.consultar();
  }

  private cargarAcciones(): void {
    this.bitacoraService.listarAcciones().subscribe({
      next: (resultado) => {
        this.acciones = resultado;
      },
      error: () => {
        this.acciones = [];
      }
    });
  }

  // ---------- CU-15 pasos 3 y 4: consultar con los filtros ----------
  consultar(): void {
    this.mensaje = '';
    this.cargando = true;

    this.bitacoraService.consultar(this.filtros).subscribe({
      next: (resultado) => {
        this.cargando = false;
        this.resultado = resultado;

        // FA01: no existen registros para los filtros aplicados
        if (resultado.totalRegistros === 0) {
          this.mensaje = 'No existen registros para los filtros aplicados.';
        }
      },
      error: (error) => {
        this.cargando = false;
        this.resultado = null;
        this.mensaje = this.mensajeError(error, 'El rango de fechas ingresado no es válido.');
      }
    });
  }

  buscar(): void {
    this.filtros.pagina = 0;
    this.consultar();
  }

  limpiarFiltros(): void {
    this.filtros.usuario = '';
    this.filtros.accion = '';
    this.filtros.entidad = '';
    this.filtros.rol = '';
    this.buscar();
  }

  // ---------- Paginación sencilla ----------
  paginaAnterior(): void {
    if (this.filtros.pagina > 0) {
      this.filtros.pagina = this.filtros.pagina - 1;
      this.consultar();
    }
  }

  paginaSiguiente(): void {
    if (this.resultado && this.filtros.pagina + 1 < this.resultado.totalPaginas) {
      this.filtros.pagina = this.filtros.pagina + 1;
      this.consultar();
    }
  }

  exportar(): void {
    this.bitacoraService.exportarCsv(this.filtros).subscribe({
      next: (archivo) => {
        const url = window.URL.createObjectURL(archivo);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = 'bitacora.csv';
        enlace.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        this.mensaje = this.mensajeError(error, 'No existen registros para los filtros aplicados.');
      }
    });
  }

  private comoTexto(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }

  private mensajeError(error: { error?: { mensaje?: string } }, respaldo: string): string {
    return error?.error?.mensaje ?? respaldo;
  }
}
