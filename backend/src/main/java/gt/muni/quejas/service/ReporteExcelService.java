package gt.muni.quejas.service;

import gt.muni.quejas.dto.DtoConteo;
import gt.muni.quejas.dto.DtoHistorialCaso;
import gt.muni.quejas.dto.DtoReporteEstadistico;
import gt.muni.quejas.dto.DtoTrazabilidadCaso;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

// CU-14 paso 5: exportación de los reportes en formato Excel (.xlsx).
// Los colores son los mismos del material institucional de la Municipalidad,
// para que el archivo descargado se vea igual que la pantalla.
@Service
public class ReporteExcelService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Paleta institucional: verde profundo, dorado y crema
    private static final byte[] VERDE = {(byte) 0x0E, (byte) 0x2B, (byte) 0x25};
    private static final byte[] VERDE_CLARO = {(byte) 0x12, (byte) 0x38, (byte) 0x30};
    private static final byte[] DORADO = {(byte) 0xC9, (byte) 0xA2, (byte) 0x27};
    private static final byte[] CREMA = {(byte) 0xF7, (byte) 0xF4, (byte) 0xEC};
    private static final byte[] BLANCO = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] TINTA = {(byte) 0x14, (byte) 0x21, (byte) 0x1D};
    private static final byte[] BORDE = {(byte) 0xE5, (byte) 0xE0, (byte) 0xD5};

    // Estilos que se arman una sola vez por libro y se reutilizan en cada celda
    private static final class Estilos {
        private CellStyle titulo;
        private CellStyle subtitulo;
        private CellStyle seccion;
        private CellStyle encabezado;
        private CellStyle etiqueta;
        private CellStyle valor;
        private CellStyle valorNumero;
        private CellStyle filaClara;
        private CellStyle filaOscura;
        private CellStyle numeroClaro;
        private CellStyle numeroOscuro;
        private CellStyle totalEtiqueta;
        private CellStyle totalNumero;
    }

    // ---------- Reporte estadístico ----------
    public byte[] generarEstadisticas(DtoReporteEstadistico reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Estilos estilos = construirEstilos(libro);
            XSSFSheet hoja = libro.createSheet("Reporte estadístico");
            hoja.setDisplayGridlines(false);
            hoja.setColumnWidth(0, 1000);
            hoja.setColumnWidth(1, 12000);
            hoja.setColumnWidth(2, 5000);

            int fila = 1;
            fila = titulo(hoja, estilos, fila, "REPORTE ESTADÍSTICO DE CASOS",
                    "Periodo del " + reporte.fechaInicio() + " al " + reporte.fechaFin(), 2);

            fila = seccion(hoja, estilos, fila, "Resumen del periodo");
            fila = resumenNumero(hoja, estilos, fila, "Total de casos", reporte.totalCasos());
            fila++;

            fila = tablaConteo(hoja, estilos, fila, "Casos por estado", "Estado", reporte.porEstado());
            fila = tablaConteo(hoja, estilos, fila, "Casos por tipo de caso", "Tipo de caso", reporte.porTipoCaso());
            fila = tablaConteo(hoja, estilos, fila, "Casos por departamento", "Departamento", reporte.porDepartamento());
            tablaConteo(hoja, estilos, fila, "Casos por categoría", "Categoría", reporte.porCategoria());

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el reporte");
        }
    }

    // ---------- Trazabilidad del caso ----------
    public byte[] generarTrazabilidad(DtoTrazabilidadCaso traza) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            Estilos estilos = construirEstilos(libro);
            XSSFSheet hoja = libro.createSheet("Trazabilidad");
            hoja.setDisplayGridlines(false);
            hoja.setColumnWidth(0, 1000);
            hoja.setColumnWidth(1, 8000);
            hoja.setColumnWidth(2, 6000);
            hoja.setColumnWidth(3, 3600);
            hoja.setColumnWidth(4, 3000);
            hoja.setColumnWidth(5, 4000);
            hoja.setColumnWidth(6, 12000);

            int fila = 1;
            fila = titulo(hoja, estilos, fila, "REPORTE DE TRAZABILIDAD DEL CASO",
                    "Código de seguimiento " + traza.codigoSeguimiento(), 6);

            fila = seccion(hoja, estilos, fila, "Datos del caso");
            fila = resumen(hoja, estilos, fila, "Fecha de registro", traza.fechaIngreso().format(FECHA));
            fila = resumen(hoja, estilos, fila, "Tipo de caso", traza.tipoCaso());
            fila = resumen(hoja, estilos, fila, "Categoría", traza.categoria());
            fila = resumen(hoja, estilos, fila, "Departamento responsable", traza.departamento());
            fila = resumen(hoja, estilos, fila, "Estado actual", traza.estado());
            fila = resumen(hoja, estilos, fila, "Ciudadano", traza.nombreCiudadano());
            fila = resumen(hoja, estilos, fila, "Técnico responsable",
                    traza.nombreTecnico().isBlank() ? "Sin asignar" : traza.nombreTecnico());
            fila++;

            fila = seccion(hoja, estilos, fila, "Historial del caso");
            String[] columnas = {"Acción", "Usuario", "Fecha", "Hora", "IP de origen", "Detalle"};
            fila = encabezadoTabla(hoja, estilos, fila, columnas);

            List<DtoHistorialCaso> historial = traza.historial();
            for (int i = 0; i < historial.size(); i++) {
                DtoHistorialCaso linea = historial.get(i);
                boolean par = i % 2 == 0;
                Row renglon = hoja.createRow(fila++);
                renglon.setHeightInPoints(18);
                texto(renglon, 1, linea.accion(), par ? estilos.filaClara : estilos.filaOscura);
                texto(renglon, 2, linea.usuario(), par ? estilos.filaClara : estilos.filaOscura);
                texto(renglon, 3, String.valueOf(linea.fecha()), par ? estilos.filaClara : estilos.filaOscura);
                texto(renglon, 4, String.valueOf(linea.hora()), par ? estilos.filaClara : estilos.filaOscura);
                texto(renglon, 5, linea.ipOrigen(), par ? estilos.filaClara : estilos.filaOscura);
                texto(renglon, 6, linea.detalle(), par ? estilos.filaClara : estilos.filaOscura);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el reporte");
        }
    }

    // ---------- Bloques de la hoja ----------

    // Banda verde con el nombre del reporte y el periodo, igual al encabezado de la pantalla
    private int titulo(XSSFSheet hoja, Estilos estilos, int fila, String texto,
                       String detalle, int ultimaColumna) {
        Row renglonTitulo = hoja.createRow(fila);
        renglonTitulo.setHeightInPoints(30);
        Row renglonDetalle = hoja.createRow(fila + 1);
        renglonDetalle.setHeightInPoints(20);

        // La banda de color se pinta celda por celda para que cubra todo el ancho
        for (int columna = 1; columna <= ultimaColumna; columna++) {
            texto(renglonTitulo, columna, columna == 1 ? texto : "", estilos.titulo);
            texto(renglonDetalle, columna, columna == 1 ? detalle : "", estilos.subtitulo);
        }

        return fila + 3;
    }

    private int seccion(XSSFSheet hoja, Estilos estilos, int fila, String texto) {
        Row renglon = hoja.createRow(fila);
        renglon.setHeightInPoints(22);
        texto(renglon, 1, texto, estilos.seccion);
        return fila + 1;
    }

    private int resumen(XSSFSheet hoja, Estilos estilos, int fila, String etiqueta, String valor) {
        Row renglon = hoja.createRow(fila);
        renglon.setHeightInPoints(18);
        texto(renglon, 1, etiqueta, estilos.etiqueta);
        texto(renglon, 2, valor, estilos.valor);
        return fila + 1;
    }

    // Los indicadores se guardan como número para que Excel pueda operar con ellos
    private int resumenNumero(XSSFSheet hoja, Estilos estilos, int fila, String etiqueta, double valor) {
        Row renglon = hoja.createRow(fila);
        renglon.setHeightInPoints(18);
        texto(renglon, 1, etiqueta, estilos.etiqueta);
        Cell celda = renglon.createCell(2);
        celda.setCellValue(valor);
        celda.setCellStyle(estilos.valorNumero);
        return fila + 1;
    }

    // Tabla de dos columnas con filas alternas y el total al final
    private int tablaConteo(XSSFSheet hoja, Estilos estilos, int fila, String titulo,
                            String nombreColumna, List<DtoConteo> datos) {
        int siguiente = seccion(hoja, estilos, fila, titulo);
        siguiente = encabezadoTabla(hoja, estilos, siguiente, new String[]{nombreColumna, "Cantidad"});

        long total = 0;
        for (int i = 0; i < datos.size(); i++) {
            DtoConteo dato = datos.get(i);
            boolean par = i % 2 == 0;
            Row renglon = hoja.createRow(siguiente++);
            renglon.setHeightInPoints(18);
            texto(renglon, 1, dato.etiqueta(), par ? estilos.filaClara : estilos.filaOscura);
            numero(renglon, 2, dato.cantidad(), par ? estilos.numeroClaro : estilos.numeroOscuro);
            total = total + dato.cantidad();
        }

        Row renglonTotal = hoja.createRow(siguiente++);
        renglonTotal.setHeightInPoints(20);
        texto(renglonTotal, 1, "Total", estilos.totalEtiqueta);
        numero(renglonTotal, 2, total, estilos.totalNumero);

        return siguiente + 1;
    }

    private int encabezadoTabla(XSSFSheet hoja, Estilos estilos, int fila, String[] columnas) {
        Row renglon = hoja.createRow(fila);
        renglon.setHeightInPoints(24);
        for (int i = 0; i < columnas.length; i++) {
            texto(renglon, i + 1, columnas[i], estilos.encabezado);
        }
        return fila + 1;
    }

    private void texto(Row renglon, int columna, String valor, CellStyle estilo) {
        Cell celda = renglon.createCell(columna);
        celda.setCellValue(valor == null ? "" : valor);
        celda.setCellStyle(estilo);
    }

    private void numero(Row renglon, int columna, long valor, CellStyle estilo) {
        Cell celda = renglon.createCell(columna);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    // ---------- Estilos del libro ----------
    private Estilos construirEstilos(XSSFWorkbook libro) {
        Estilos estilos = new Estilos();

        estilos.titulo = estilo(libro, VERDE, fuente(libro, 15, true, DORADO),
                HorizontalAlignment.LEFT, false);
        estilos.subtitulo = estilo(libro, VERDE_CLARO, fuente(libro, 10, false, CREMA),
                HorizontalAlignment.LEFT, false);
        estilos.seccion = estilo(libro, null, fuente(libro, 12, true, VERDE),
                HorizontalAlignment.LEFT, false);

        estilos.encabezado = estilo(libro, VERDE, fuente(libro, 10, true, CREMA),
                HorizontalAlignment.LEFT, true);
        estilos.etiqueta = estilo(libro, CREMA, fuente(libro, 10, true, VERDE),
                HorizontalAlignment.LEFT, true);
        estilos.valor = estilo(libro, BLANCO, fuente(libro, 10, false, TINTA),
                HorizontalAlignment.LEFT, true);
        estilos.valorNumero = estilo(libro, BLANCO, fuente(libro, 10, true, TINTA),
                HorizontalAlignment.LEFT, true);

        estilos.filaClara = estilo(libro, BLANCO, fuente(libro, 10, false, TINTA),
                HorizontalAlignment.LEFT, true);
        estilos.filaOscura = estilo(libro, CREMA, fuente(libro, 10, false, TINTA),
                HorizontalAlignment.LEFT, true);
        estilos.numeroClaro = estilo(libro, BLANCO, fuente(libro, 10, false, TINTA),
                HorizontalAlignment.RIGHT, true);
        estilos.numeroOscuro = estilo(libro, CREMA, fuente(libro, 10, false, TINTA),
                HorizontalAlignment.RIGHT, true);

        estilos.totalEtiqueta = estilo(libro, DORADO, fuente(libro, 10, true, VERDE),
                HorizontalAlignment.LEFT, true);
        estilos.totalNumero = estilo(libro, DORADO, fuente(libro, 10, true, VERDE),
                HorizontalAlignment.RIGHT, true);

        return estilos;
    }

    private XSSFFont fuente(XSSFWorkbook libro, int tamano, boolean negrita, byte[] color) {
        XSSFFont fuente = libro.createFont();
        fuente.setFontName("Calibri");
        fuente.setFontHeightInPoints((short) tamano);
        fuente.setBold(negrita);
        fuente.setColor(new XSSFColor(color, null));
        return fuente;
    }

    private XSSFCellStyle estilo(XSSFWorkbook libro, byte[] fondo, Font fuente,
                                 HorizontalAlignment alineacion, boolean conBorde) {
        XSSFCellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setAlignment(alineacion);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setWrapText(false);

        if (fondo != null) {
            estilo.setFillForegroundColor(new XSSFColor(fondo, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (conBorde) {
            XSSFColor colorBorde = new XSSFColor(BORDE, null);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
            estilo.setBottomBorderColor(colorBorde);
            estilo.setTopBorderColor(colorBorde);
            estilo.setLeftBorderColor(colorBorde);
            estilo.setRightBorderColor(colorBorde);
        }
        return estilo;
    }
}
