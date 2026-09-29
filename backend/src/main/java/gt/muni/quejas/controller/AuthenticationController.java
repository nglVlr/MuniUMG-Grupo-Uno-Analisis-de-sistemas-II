package gt.muni.quejas.controller;

import gt.muni.quejas.dto.DtoJwtRespuesta;
import gt.muni.quejas.dto.DtoUsuario;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.service.AutenticationService;
import gt.muni.quejas.service.BitacoraCambioService;
import gt.muni.quejas.service.TokenService;
import gt.muni.quejas.util.ClienteHttp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("api/login")
public class AuthenticationController {
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PASSWORD = Pattern.compile("^[A-Za-z0-9]{6,}$");

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AutenticationService autenticationService;
    private final BitacoraCambioService bitacoraCambioService;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    TokenService tokenService,
                                    AutenticationService autenticationService,
                                    BitacoraCambioService bitacoraCambioService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.autenticationService = autenticationService;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    @PostMapping
    public ResponseEntity<DtoJwtRespuesta> autenticarUsuario(@Valid @RequestBody DtoUsuario dtoUsuario,
                                                             HttpServletRequest request){
        String usuario = dtoUsuario == null || dtoUsuario.usuario() == null ? "" : dtoUsuario.usuario().trim();
        String password = dtoUsuario == null || dtoUsuario.password() == null ? "" : dtoUsuario.password();
        String ip = ClienteHttp.ipOrigen(request);

        if (!CORREO.matcher(usuario).matches() || !PASSWORD.matcher(password).matches()) {
            bitacoraCambioService.registrarIngreso(BitacoraCambioService.INGRESO_FALLIDO, usuario, ip, "Empleado");
            throw new NegocioException(CodigoError.CREDENCIALES_INVALIDAS);
        }

        Authentication usuarioAutenticado;

        try {
            Authentication authToken = new UsernamePasswordAuthenticationToken(usuario, password);
            usuarioAutenticado = authenticationManager.authenticate(authToken);
        } catch (BadCredentialsException ex) {
            autenticationService.registrarIntentoFallido(usuario);
            bitacoraCambioService.registrarIngreso(BitacoraCambioService.INGRESO_FALLIDO, usuario, ip, "Empleado");
            throw ex;
        } catch (DisabledException | LockedException ex) {
            bitacoraCambioService.registrarIngreso(BitacoraCambioService.INGRESO_FALLIDO, usuario, ip, "Empleado");
            throw ex;
        }

        autenticationService.registrarLoginExitoso(usuario);

        var userDetails = usuarioAutenticado.getPrincipal();

        String nombreUsuario;
        Integer rol;
        String correo;
        String cui;

        if(userDetails instanceof Usuario usuarios){
            nombreUsuario = usuarios.getNombre();
            rol = Math.toIntExact(usuarios.getIdRol().getId());
            correo = usuarios.getCorreo();
            cui = usuarios.getCui();
        } else {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible procesar la autenticación");
        }

        String jwtToken = tokenService.generarToken((UserDetails) userDetails);
        bitacoraCambioService.registrarIngreso(BitacoraCambioService.INGRESO_EXITOSO, usuario, ip, "Empleado");
        return ResponseEntity.ok(new DtoJwtRespuesta(
                jwtToken,
                correo,
                nombreUsuario,
                rol,
                cui));
    }
}
