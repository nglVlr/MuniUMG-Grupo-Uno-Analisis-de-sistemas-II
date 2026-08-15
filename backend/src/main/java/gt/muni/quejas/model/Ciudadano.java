package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Base precargada de ciudadanos (RN-GLOBAL-04). Solo las personas registradas
 * aqui pueden presentar un caso; el sistema busca por numero de identificacion
 * y obtiene nombre y correo automaticamente, sin registro anonimo.
 */
@Entity
@Table(name = "ciudadano")
@Getter
@Setter
@NoArgsConstructor
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, unique = true, length = 30)
    private String numeroDocumento;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String correo;
}
