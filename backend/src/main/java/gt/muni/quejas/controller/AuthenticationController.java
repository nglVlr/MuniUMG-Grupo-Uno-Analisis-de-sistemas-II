package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoJwtRespuesta;
import gt.muni.quejas.dto.DtoUsuario;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.AutenticationService;
import gt.muni.quejas.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/login")
public class AuthenticationController {
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AutenticationService autenticationService;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    TokenService tokenService,
                                    AutenticationService autenticationService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.autenticationService = autenticationService;
    }

    @PostMapping
    public ResponseEntity<DtoJwtRespuesta> autenticarUsuario(@Valid @RequestBody DtoUsuario dtoUsuario){
        Authentication usuarioAutenticado;

        try {
            Authentication authToken = new UsernamePasswordAuthenticationToken(dtoUsuario.usuario(), dtoUsuario.password());
            usuarioAutenticado = authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException ex) {
            autenticationService.registrarIntentoFallido(dtoUsuario.usuario());
            throw ex;
        }

        autenticationService.registrarLoginExitoso(dtoUsuario.usuario());

        var userDetails = usuarioAutenticado.getPrincipal();

        String nombreUsuario;
        Integer rol;
        String usuario;
        String cui;

        if(userDetails instanceof Usuario usuarios){
            nombreUsuario = usuarios.getNombre();
            rol = Math.toIntExact(usuarios.getIdRol().getId());
            usuario = usuarios.getCorreo();
            cui = usuarios.getCui();
        } else {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible procesar la autenticación");
        }

        String jwtToken = tokenService.generarToken((UserDetails) userDetails);
        return ResponseEntity.ok(new DtoJwtRespuesta(
                jwtToken,
                usuario,
                nombreUsuario,
                rol,
                cui));
    }
}
