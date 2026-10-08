package gt.muni.quejas.service;

import gt.muni.quejas.config.ArchivoProperties;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Evidencia;
import gt.muni.quejas.repository.EvidenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenciaStorageService {

    // La evidencia del caso puede ser el documento en PDF o una fotografia del
    // problema reportado; el soporte del reporte de atencion sigue siendo PDF.
    private static final Set<String> TIPOS_DOCUMENTO = Set.of("application/pdf");

    private static final Set<String> EXTENSIONES_DOCUMENTO = Set.of(".pdf");

    private static final Set<String> TIPOS_IMAGEN = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of(
            ".jpg", ".jpeg", ".png", ".webp");

    private final ArchivoProperties archivoProperties;
    private final EvidenciaRepository evidenciaRepository;

    public EvidenciaStorageService(ArchivoProperties archivoProperties,
                                   EvidenciaRepository evidenciaRepository) {
        this.archivoProperties = archivoProperties;
        this.evidenciaRepository = evidenciaRepository;
    }

    public void guardarSiPresente(Caso caso, MultipartFile archivo) {
        guardarSiPresente(caso, archivo, Evidencia.TIPO_REGISTRO);
    }

    // CU-05 FA02: el soporte del reporte de atención se guarda con tipo ATENCION
    public void guardarSiPresente(Caso caso, MultipartFile archivo, String tipo) {
        if (archivo == null) {
            return;
        }
        if (archivo.isEmpty()) {
            throw new NegocioException(CodigoError.EVIDENCIA_VACIA);
        }

        long maxBytes = archivoProperties.getEvidencia().getTamanoMaximoMb() * 1024L * 1024L;
        if (archivo.getSize() > maxBytes) {
            throw new NegocioException(CodigoError.EVIDENCIA_TAMANO);
        }
        // La fotografia solo se acepta como evidencia del caso, no como soporte
        boolean admiteImagen = Evidencia.TIPO_REGISTRO.equals(tipo);
        if (!formatoPermitido(archivo, admiteImagen)) {
            throw new NegocioException(admiteImagen
                    ? CodigoError.EVIDENCIA_FORMATO
                    : CodigoError.SOPORTE_FORMATO);
        }

        Path directorio = Path.of(archivoProperties.getArchivos().getDirectorio(), "evidencias");
        try {
            Files.createDirectories(directorio);
            String original = archivo.getOriginalFilename() == null ? "evidencia" : archivo.getOriginalFilename();
            String seguro = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destino = directorio.resolve(caso.getId() + "-" + UUID.randomUUID() + "-" + seguro).toAbsolutePath();
            archivo.transferTo(destino.toFile());

            Evidencia evidencia = new Evidencia();
            evidencia.setCaso(caso);
            evidencia.setTipo(tipo);
            evidencia.setNombreArchivo(original);
            evidencia.setContentType(archivo.getContentType() == null ? "application/octet-stream" : archivo.getContentType());
            evidencia.setTamanoBytes(archivo.getSize());
            evidencia.setRuta(destino.toAbsolutePath().toString());
            evidenciaRepository.save(evidencia);
        } catch (IOException ex) {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible guardar la evidencia");
        }
    }

    private boolean formatoPermitido(MultipartFile archivo, boolean admiteImagen) {
        Set<String> tipos = new HashSet<>(TIPOS_DOCUMENTO);
        Set<String> extensiones = new HashSet<>(EXTENSIONES_DOCUMENTO);
        if (admiteImagen) {
            tipos.addAll(TIPOS_IMAGEN);
            extensiones.addAll(EXTENSIONES_IMAGEN);
        }

        String contentType = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        if (tipos.contains(contentType)) {
            return true;
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        return extensiones.stream().anyMatch(nombre::endsWith);
    }
}
