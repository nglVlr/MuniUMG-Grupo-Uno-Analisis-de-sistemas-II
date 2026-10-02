package gt.muni.quejas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estado_caso")
@Getter
@Setter
@NoArgsConstructor
public class EstadoCaso {

    public static final String REGISTRADO = "Registrado";
    public static final String EN_REVISION = "En Revisión";
    public static final String EN_ATENCION = "En Atención";
    public static final String PENDIENTE_APROBACION_RECHAZO = "Pendiente de Aprobación de Rechazo";
    public static final String REQUIERE_AMPLIACION = "Requiere Ampliación";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String estado;
}
