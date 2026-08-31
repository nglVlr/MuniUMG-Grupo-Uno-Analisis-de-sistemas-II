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
import gt.muni.quejas.model.Caso;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

@Service
public class InformeInicialService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ArchivoProperties archivoProperties;

    public InformeInicialService(ArchivoProperties archivoProperties) {
        this.archivoProperties = archivoProperties;
    }

    public String generar(Caso caso) {
        Path directorio = Path.of(archivoProperties.getArchivos().getDirectorio(), "informes");
        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve("informe-inicial-" + caso.getId() + ".pdf");
            try (OutputStream out = Files.newOutputStream(destino)) {
                escribirPdf(caso, out);
            }
            return destino.toAbsolutePath().toString();
        } catch (IOException | DocumentException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el informe inicial");
        }
    }

    private void escribirPdf(Caso caso, OutputStream out) throws DocumentException {
        Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(14, 43, 37));
        Font seccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(14, 43, 37));
        Font cuerpo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        document.add(new Paragraph("MUNICIPALIDAD DE GRANADOS, B.V.", titulo));
        document.add(new Paragraph("Sistema de Quejas, Reclamos, Denuncias y Sugerencias", cuerpo));
        document.add(new Paragraph("INFORME INICIAL DEL CASO", seccion));
        document.add(new Paragraph(" "));
        document.add(linea("Caso No.", String.valueOf(caso.getId()), cuerpo));
        document.add(linea("Código de seguimiento", caso.getCodigoSeguimiento(), cuerpo));
        document.add(linea("Fecha de registro", caso.getCreateAt().format(FECHA), cuerpo));
        document.add(linea("Tipo de caso", caso.getTipoCaso(), cuerpo));
        document.add(linea("Categoría", caso.getCategoria().getNombre(), cuerpo));
        document.add(linea("Ciudadano", caso.getCiudadano().getNombre(), cuerpo));
        document.add(linea("DPI", caso.getCiudadano().getDpi(), cuerpo));
        document.add(linea("Departamento responsable", caso.getDepartamento().getNombre(), cuerpo));
        document.add(linea("Dirección o ubicación del problema", caso.getDireccion(), cuerpo));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Descripción del caso", seccion));
        document.add(new Paragraph(caso.getDescripcion(), cuerpo));
        document.close();
    }

    private Paragraph linea(String etiqueta, String valor, Font fuente) {
        return new Paragraph(etiqueta + ": " + valor, fuente);
    }
}
