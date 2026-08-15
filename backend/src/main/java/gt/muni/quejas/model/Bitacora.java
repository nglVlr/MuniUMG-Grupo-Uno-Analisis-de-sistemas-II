package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de auditoria (CU-15 sesiones / CU-16 cambios). Es append-only:
 * no se expone ningun endpoint de actualizacion o borrado sobre esta tabla.
 */
@Entity
@Table(name = "bitacora")
@Getter
@Setter
@NoArgsConstructor
public class Bitacora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String accion;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    /** IPv4 o IPv6 del cliente; 50 caracteres para cubrir IPv6 completa. */
    @Column(nullable = false, length = 50)
    private String ip;

    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_caso")
    private Caso caso;
}
