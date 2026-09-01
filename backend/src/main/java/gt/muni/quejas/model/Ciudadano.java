package gt.muni.quejas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciudadano")
@Getter
@Setter
@NoArgsConstructor
public class Ciudadano {

    @Id
    @Column(length = 13)
    private String dpi;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 8)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String correo;
}
