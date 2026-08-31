package gt.muni.quejas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "correlativo_seguimiento")
@Getter
@Setter
@NoArgsConstructor
public class CorrelativoSeguimiento {

    @Id
    @Column(name = "anio")
    private Integer anio;

    @Column(nullable = false)
    private Integer ultimo;
}
