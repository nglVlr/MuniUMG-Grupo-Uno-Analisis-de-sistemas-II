package gt.muni.quejas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    public static final String RESULTADO_ENVIADA = "ENVIADA";
    public static final String RESULTADO_ERROR = "ERROR";
    public static final String EVENTO_REGISTRO = "registro del caso";
    public static final String EVENTO_AMPLIACION = "requerimiento de ampliación";
    public static final String EVENTO_ASIGNACION = "asignación a Técnico";
    public static final String EVENTO_DEVOLUCION = "devolución del caso";
    public static final String EVENTO_RESOLUCION = "resolución del caso";
    public static final String EVENTO_RECHAZO = "rechazo del caso";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String evento;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(nullable = false)
    private Integer intentos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caso")
    private Caso caso;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;
}
