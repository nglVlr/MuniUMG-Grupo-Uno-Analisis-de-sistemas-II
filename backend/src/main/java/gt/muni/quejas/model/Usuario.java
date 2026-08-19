package gt.muni.quejas.model;

import gt.muni.quejas.dto.DtoUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario implements UserDetails {

    public static final int MAX_INTENTOS_FALLIDOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(length = 13)
    private String cui;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(nullable = false, length = 150)
    private String password;

    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol idRol;

    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento idDepartamento;

    @ManyToOne
    @JoinColumn(name = "id_estado")
    private EstadoUsuarios idEstado;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(nullable = false)
    private LocalDateTime createAt;

    @Column(nullable = false, length = 50)
    private String usuarioCreacion;

    public Usuario (DtoUsuario dtoUsuario){
        this.correo = dtoUsuario.usuario();
        this.nombre = dtoUsuario.nombre();
        this.password = dtoUsuario.password();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword(){return password;}

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return idEstado == null || !"Bloqueado".equalsIgnoreCase(idEstado.getEstado());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return idEstado != null && "Activo".equalsIgnoreCase(idEstado.getEstado());
    }

    public void incrementarIntentosFallidos(){
        this.intentosFallidos = (this.intentosFallidos == null ? 0: this.intentosFallidos) + 1;
    }

    public void reiniciarIntentosFallidos(){
        this.intentosFallidos = 0;
    }

    public boolean debeBloquearse(){
        return this.intentosFallidos != null && this.intentosFallidos >= MAX_INTENTOS_FALLIDOS;
    }
}
