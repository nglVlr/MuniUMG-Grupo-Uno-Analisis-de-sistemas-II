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
@Table(name = "token_verificacion_ciudadano")
@Getter
@Setter
@NoArgsConstructor
public class TokenVerificacionCiudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dpi", nullable = false)
    private Ciudadano ciudadano;

    @Column(name = "codigo_hash", nullable = false, length = 150)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "token_sesion", unique = true, length = 255)
    private String tokenSesion;

    @Column(name = "fecha_expiracion_sesion")
    private LocalDateTime fechaExpiracionSesion;

    @Column(name = "sesion_usada", nullable = false)
    private boolean sesionUsada = false;

    public boolean codigoVigente() {
        return !usado && !LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean sesionVigente() {
        return tokenSesion != null
                && !sesionUsada
                && fechaExpiracionSesion != null
                && !LocalDateTime.now().isAfter(fechaExpiracionSesion);
    }
}
