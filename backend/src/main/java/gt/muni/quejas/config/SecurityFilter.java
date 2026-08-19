package gt.muni.quejas.config;

import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.UsuarioRepository;
import gt.muni.quejas.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;


    public SecurityFilter(UsuarioRepository usuarioRepository, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
    }

    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if(authHeader != null){
            var token = authHeader.replace("Bearer ", "");

            try {
                var subjet = tokenService.getSubject(token);

                Optional<Usuario> usuarios = usuarioRepository.findByCorreo(subjet);

                if(usuarios.isPresent()){
                    var usuario = usuarios.get();

                    if(usuario.isEnabled()){
                        var authentication = new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                usuario.getAuthorities());
                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                }
            } catch (NegocioException ex) {
                // Token inválido/expirado: simplemente no se autentica.
                // El AuthenticationEntryPoint se encarga de responder el 401 si el endpoint lo requiere.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
