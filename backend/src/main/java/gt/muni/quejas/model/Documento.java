package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Documento formal generado en cada paso del flujo (informe inicial, informe de
 * validacion, oficio de delegacion, reporte de atencion, reporte de supervision).
 * Se modela como una sola entidad con un "tipo" en vez de una jerarquia de clases,
 * para mantener el codigo simple; el campo "contenido" guarda el detalle propio
 * de cada tipo de documento en texto libre / JSON simple.
 */
@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_caso", nullable = false)
    private Caso caso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoDocumentoCaso tipo;

    @Column(name = "numero_correlativo", nullable = false, length = 30)
    private String numeroCorrelativo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_autor", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 4000)
    private String contenido;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();
}
