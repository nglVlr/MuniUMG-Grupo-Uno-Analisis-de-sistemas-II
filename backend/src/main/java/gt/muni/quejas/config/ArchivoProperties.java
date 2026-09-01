package gt.muni.quejas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ArchivoProperties {

    private final Cache cache = new Cache();
    private final Evidencia evidencia = new Evidencia();
    private final Archivos archivos = new Archivos();

    public Cache getCache() {
        return cache;
    }

    public Evidencia getEvidencia() {
        return evidencia;
    }

    public Archivos getArchivos() {
        return archivos;
    }

    public static class Cache {
        private int catalogosTtlMinutos = 60;

        public int getCatalogosTtlMinutos() {
            return catalogosTtlMinutos;
        }

        public void setCatalogosTtlMinutos(int catalogosTtlMinutos) {
            this.catalogosTtlMinutos = catalogosTtlMinutos;
        }
    }

    public static class Evidencia {
        private int tamanoMaximoMb = 100;

        public int getTamanoMaximoMb() {
            return tamanoMaximoMb;
        }

        public void setTamanoMaximoMb(int tamanoMaximoMb) {
            this.tamanoMaximoMb = tamanoMaximoMb;
        }
    }

    public static class Archivos {
        private String directorio = "./data/archivos";

        public String getDirectorio() {
            return directorio;
        }

        public void setDirectorio(String directorio) {
            this.directorio = directorio;
        }
    }
}
