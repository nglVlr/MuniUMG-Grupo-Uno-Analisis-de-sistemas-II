package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoria del problema reportado (CU-11). Cada categoria fija automaticamente
 * el departamento responsable (RN-CU02-03), y opcionalmente restringe a que
 * tipos de caso aplica (por ejemplo "Sugerencias generales" solo aplica a SUGERENCIA).
 */
@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    /**
     * Tipos de caso a los que aplica esta categoria, separados por coma
     * (ej. "QUEJA,DENUNCIA,SUGERENCIA"). Si es null/vacio, aplica a todos.
     */
    @Column(name = "tipos_permitidos", length = 60)
    private String tiposPermitidos;

    @Column(nullable = false)
    private boolean activo = true;
}
