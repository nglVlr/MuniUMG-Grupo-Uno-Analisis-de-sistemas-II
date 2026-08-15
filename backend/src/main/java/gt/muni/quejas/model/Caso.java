package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Caso (queja, denuncia o sugerencia) registrado por un ciudadano (CU-02). */
@Entity
@Table(name = "caso")
@Getter
@Setter
@NoArgsConstructor
public class Caso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_seguimiento", nullable = false, unique = true, length = 20)
    private String codigoSeguimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_caso", nullable = false, length = 15)
    private TipoCaso tipoCaso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCaso estado = EstadoCaso.REGISTRADO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_ciudadano", nullable = false)
    private Ciudadano ciudadano;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    /** Empleado actualmente responsable; puede cambiar por reasignacion. */
    @ManyToOne
    @JoinColumn(name = "id_empleado_asignado")
    private Usuario empleadoAsignado;

    @Column(name = "direccion_problema", nullable = false, length = 250)
    private String direccionProblema;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_rechazo", length = 40)
    private MotivoRechazo motivoRechazo;

    @Column(name = "detalle_rechazo", length = 500)
    private String detalleRechazo;
}
