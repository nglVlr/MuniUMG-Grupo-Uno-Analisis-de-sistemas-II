package gt.muni.quejas.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import gt.muni.quejas.config.ArchivoProperties;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.ActividadCaso;
import gt.muni.quejas.model.Caso;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// AN04 # 2 Reporte de Atención del Caso
@Service
public class InformeAtencionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ArchivoProperties archivoProperties;

    public InformeAtencionService(ArchivoProperties archivoProperties) {
        this.archivoProperties = archivoProperties;
    }

    public String generar(Caso caso, List<ActividadCaso> actividades, List<String> nombresEvidencia) {
        Path directorio = Path.of(archivoProperties.getArchivos().getDirectorio(), "informes");
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve("reporte-atencion-" + caso.getId() + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                escribirPdf(caso, actividades, nombresEvidencia, out);
            }
            return destino.toAbsolutePath().toString();
        } catch (IOException | DocumentException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el reporte de atención");
        }
    }

    private void escribirPdf(Caso caso, List<ActividadCaso> actividades,
                             List<String> nombresEvidencia, OutputStream out) throws DocumentException {
        Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(14, 43, 37));
        Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(14, 43, 37));
        Font cuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        document.add(new Paragraph("MUNICIPALIDAD DE GRANADOS, B.V.", titulo));
        document.add(new Paragraph("Sistema de Quejas, Reclamos, Denuncias y Sugerencias", cuerpo));
        document.add(new Paragraph("REPORTE DE ATENCIÓN DEL CASO", seccion));
        document.add(new Paragraph(" "));
        document.add(linea("Código de seguimiento", caso.getCodigoSeguimiento(), cuerpo));
        document.add(linea("Departamento", caso.getDepartamento().getNombre(), cuerpo));
        document.add(linea("Técnico responsable", caso.getTecnico().getNombre(), cuerpo));
        document.add(linea("Fecha de atención", LocalDateTime.now().format(FECHA), cuerpo));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Acciones realizadas:", seccion));
        for (int i = 0; i < actividades.size(); i++) {
            ActividadCaso actividad = actividades.get(i);
            String fila = (i + 1) + ". " + actividad.getFecha().format(FECHA) + " - " + actividad.getDetalle();
            document.add(new Paragraph(fila, cuerpo));
        }
        document.add(new Paragraph(" "));

        String listaEvidencia = "";
        for (int i = 0; i < nombresEvidencia.size(); i++) {
            if (i > 0) {
                listaEvidencia = listaEvidencia + ", ";
            }
            listaEvidencia = listaEvidencia + nombresEvidencia.get(i);
        }
        document.add(linea("Evidencia adjunta", listaEvidencia, cuerpo));
        document.close();
    }

    private Paragraph linea(String etiqueta, String valor, Font fuente) {
        return new Paragraph(etiqueta + ": " + valor, fuente);
    }
}
