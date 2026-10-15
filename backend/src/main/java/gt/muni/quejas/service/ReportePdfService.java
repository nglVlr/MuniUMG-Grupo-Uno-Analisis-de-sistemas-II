package gt.muni.quejas.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import gt.muni.quejas.dto.DtoConteo;
import gt.muni.quejas.dto.DtoHistorialCaso;
import gt.muni.quejas.dto.DtoReporteEstadistico;
import gt.muni.quejas.dto.DtoTrazabilidadCaso;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// CU-14 paso 5: exportación de los reportes en PDF
@Service
public class ReportePdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarEstadisticas(DtoReporteEstadistico reporte) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, salida);
            document.open();

            encabezado(document, "REPORTE ESTADÍSTICO DE CASOS");
            document.add(linea("Periodo", reporte.fechaInicio() + " al " + reporte.fechaFin()));
            document.add(linea("Total de casos", String.valueOf(reporte.totalCasos())));

            bloque(document, "Casos por estado", reporte.porEstado());
            bloque(document, "Casos por tipo de caso", reporte.porTipoCaso());
            bloque(document, "Casos por departamento", reporte.porDepartamento());
            bloque(document, "Casos por categoría", reporte.porCategoria());

            document.close();
            return salida.toByteArray();
        } catch (DocumentException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el reporte");
        }
    }

    public byte[] generarTrazabilidad(DtoTrazabilidadCaso traza) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
            PdfWriter.getInstance(document, salida);
            document.open();

            encabezado(document, "REPORTE DE TRAZABILIDAD DEL CASO");
            document.add(linea("Código de seguimiento", traza.codigoSeguimiento()));
            document.add(linea("Fecha de registro", traza.fechaIngreso().format(FECHA)));
            document.add(linea("Tipo de caso", traza.tipoCaso()));
            document.add(linea("Categoría", traza.categoria()));
            document.add(linea("Departamento responsable", traza.departamento()));
            document.add(linea("Ciudadano", traza.nombreCiudadano()));
            document.add(linea("Técnico responsable", traza.nombreTecnico()));
            document.add(linea("Estado actual", traza.estado()));
            document.add(new Paragraph(" "));

            Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(14, 43, 37));
            Font cuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            document.add(new Paragraph("Historial de cambios", seccion));

            List<DtoHistorialCaso> historial = traza.historial();
            for (int i = 0; i < historial.size(); i++) {
                DtoHistorialCaso h = historial.get(i);
                String fila = (i + 1) + ". " + h.fecha() + " " + h.hora() + " - " + h.accion()
                        + " - usuario " + h.usuario() + " - IP " + h.ipOrigen();
                document.add(new Paragraph(fila, cuerpo));
                if (!h.detalle().isBlank()) {
                    document.add(new Paragraph("    " + h.detalle(), cuerpo));
                }
            }

            document.close();
            return salida.toByteArray();
        } catch (DocumentException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el reporte");
        }
    }

    private void encabezado(Document document, String titulo) throws DocumentException {
        Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(14, 43, 37));
        Font fSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(14, 43, 37));
        Font fCuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        document.add(new Paragraph("MUNICIPALIDAD DE GRANADOS, B.V.", fTitulo));
        document.add(new Paragraph("Sistema de Quejas, Reclamos, Denuncias y Sugerencias", fCuerpo));
        document.add(new Paragraph(titulo, fSeccion));
        document.add(new Paragraph("Generado el " + LocalDateTime.now().format(FECHA), fCuerpo));
        document.add(new Paragraph(" "));
    }

    private void bloque(Document document, String titulo, List<DtoConteo> filas) throws DocumentException {
        Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(14, 43, 37));
        Font cuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(titulo, seccion));
        for (int i = 0; i < filas.size(); i++) {
            document.add(new Paragraph(filas.get(i).etiqueta() + ": " + filas.get(i).cantidad(), cuerpo));
        }
    }

    private Paragraph linea(String etiqueta, String valor) {
        Font cuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        return new Paragraph(etiqueta + ": " + valor, cuerpo);
    }
}
