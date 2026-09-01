package gt.muni.quejas.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.util.HashSet;
import java.util.Set;

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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    @Column(nullable = false)
    private boolean activo = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "categoria_tipo_caso", joinColumns = @JoinColumn(name = "id_categoria"))
    @Column(name = "tipo_caso", nullable = false, length = 20)
    private Set<String> tiposCaso = new HashSet<>();

    public boolean admiteTipo(String tipoCaso) {
        return tiposCaso == null || tiposCaso.isEmpty() || tiposCaso.contains(tipoCaso);
    }
}
