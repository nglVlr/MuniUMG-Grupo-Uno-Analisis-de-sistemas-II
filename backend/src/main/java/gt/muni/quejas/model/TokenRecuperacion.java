package gt.muni.quejas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_recuperacion")
@Getter
@Setter
@NoArgsConstructor
public class TokenRecuperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @ManyToOne
    @JoinColumn(name = "cui", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado = false;

    public boolean estaExpirado(){
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean esValido(){
        return !usado && !estaExpirado();
    }
}
