package gt.muni.quejas;

import gt.muni.quejas.config.ArchivoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ArchivoProperties.class)
public class QuejasApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuejasApplication.class, args);
    }
}
