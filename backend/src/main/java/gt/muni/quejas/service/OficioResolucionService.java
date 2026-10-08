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
import gt.muni.quejas.model.Usuario;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// AN04 # 4 Oficio de Resolución del Caso
@Service
public class OficioResolucionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ArchivoProperties archivoProperties;

    public OficioResolucionService(ArchivoProperties archivoProperties) {
        this.archivoProperties = archivoProperties;
    }

    public String generar(Caso caso, Usuario jefatura, List<ActividadCaso> actividades) {
        Path directorio = Path.of(archivoProperties.getArchivos().getDirectorio(), "informes");
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve("oficio-resolucion-" + caso.getId() + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                escribirPdf(caso, jefatura, actividades, out);
            }
            return destino.toAbsolutePath().toString();
        } catch (IOException | DocumentException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el oficio de resolución");
        }
    }

    private void escribirPdf(Caso caso, Usuario jefatura, List<ActividadCaso> actividades,
                             OutputStream out) throws DocumentException {
        Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(14, 43, 37));
        Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(14, 43, 37));
        Font cuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        document.add(new Paragraph("MUNICIPALIDAD DE GRANADOS, B.V.", titulo));
        document.add(new Paragraph("Sistema de Quejas, Reclamos, Denuncias y Sugerencias", cuerpo));
        document.add(new Paragraph("OFICIO DE RESOLUCIÓN DEL CASO", seccion));
        document.add(new Paragraph(" "));
        document.add(linea("Código de seguimiento", caso.getCodigoSeguimiento(), cuerpo));
        document.add(linea("Fecha de registro", caso.getCreateAt().format(FECHA), cuerpo));
        document.add(linea("Tipo de caso", caso.getTipoCaso(), cuerpo));
        document.add(linea("Categoría", caso.getCategoria().getNombre(), cuerpo));
        document.add(linea("Ciudadano", caso.getCiudadano().getNombre(), cuerpo));
        document.add(linea("Departamento responsable", caso.getDepartamento().getNombre(), cuerpo));
        document.add(linea("Técnico responsable", caso.getTecnico() == null ? "" : caso.getTecnico().getNombre(), cuerpo));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Acciones realizadas:", seccion));
        for (int i = 0; i < actividades.size(); i++) {
            ActividadCaso actividad = actividades.get(i);
            document.add(new Paragraph((i + 1) + ". " + actividad.getFecha().format(FECHA)
                    + " - " + actividad.getDetalle(), cuerpo));
        }
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Resolución:", seccion));
        document.add(new Paragraph("Habiéndose revisado el reporte de atención del Técnico y el reporte de "
                + "supervisión, el Jefe de Departamento firma el presente oficio y da por resuelto el caso.", cuerpo));
        document.add(new Paragraph(" "));

        // Firma electrónica: queda asociada al usuario que firma, con su CUI y la fecha
        document.add(new Paragraph("Firma electrónica", seccion));
        document.add(linea("Firmado por", jefatura.getNombre(), cuerpo));
        document.add(linea("CUI", jefatura.getCui(), cuerpo));
        document.add(linea("Cargo", "Jefe de Departamento de " + jefatura.getIdDepartamento().getNombre(), cuerpo));
        document.add(linea("Fecha y hora de la firma", LocalDateTime.now().format(FECHA), cuerpo));
        document.close();
    }

    private Paragraph linea(String etiqueta, String valor, Font fuente) {
        return new Paragraph(etiqueta + ": " + valor, fuente);
    }
}
