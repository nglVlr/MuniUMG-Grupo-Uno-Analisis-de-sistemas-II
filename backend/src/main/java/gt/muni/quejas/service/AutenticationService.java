package gt.muni.quejas.service;

import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.EstadoUsuarioRepository;
import gt.muni.quejas.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticationService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;

    public AutenticationService(UsuarioRepository usuarioRepository, EstadoUsuarioRepository estadoUsuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estadoUsuarioRepository = estadoUsuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreo(username)
                .map(user -> (UserDetails) user)
                .orElseThrow(()-> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public void registrarIntentoFallido(String correo) {
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            usuario.incrementarIntentosFallidos();

            if (usuario.debeBloquearse()) {
                var estadoBloqueado = estadoUsuarioRepository.findByEstado("Bloqueado")
                        .orElseThrow(() -> new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible bloquear al usuario"));
                usuario.setIdEstado(estadoBloqueado);
            }

            usuarioRepository.save(usuario);
        });
    }

    @Transactional
    public void registrarLoginExitoso(String correo) {
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            if (usuario.getIntentosFallidos() != null && usuario.getIntentosFallidos() > 0) {
                usuario.reiniciarIntentosFallidos();
                usuarioRepository.save(usuario);
            }
        });
    }
}
