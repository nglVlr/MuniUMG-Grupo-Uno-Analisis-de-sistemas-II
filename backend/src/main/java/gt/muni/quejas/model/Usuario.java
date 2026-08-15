package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario interno del sistema (CU-10 Mantenimiento de Usuarios).
 * El campo "estado" es clave para la reasignacion de casos: un Administrador
 * puede marcar a un empleado/jefe/supervisor como VACACIONES o PERMISO, y el
 * algoritmo de asignacion (RN-CU05-05) y la reasignacion manual deben excluir
 * a los usuarios que no esten ACTIVO.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    /** Nota del proyecto: NO se cifra la contraseña (decisión explícita del curso). */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;
}
