package gt.muni.quejas.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import gt.muni.quejas.exception.CodigoError;
import gt.muni.quejas.exception.NegocioException;
import gt.muni.quejas.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {
    @Value("${api.security.secret}")
    private String apiSecret;

    public String generarToken(UserDetails correo){
        String rol;
        String cui;

        if(correo instanceof Usuario usuario){
            rol = usuario.getNombre();
            cui = usuario.getCui();
        } else {
            throw new NegocioException(CodigoError.ERROR_INTERNO, "No fue posible generar el token: tipo de usuario no reconocido");
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);
            return JWT.create()
                    .withIssuer("app municipalidad")
                    .withSubject(usuario.getUsername())
                    .withClaim("cui", cui)
                    .withClaim("rol", rol)
                    .withExpiresAt(generatorDateExpiration())
                    .sign(algorithm);
        } catch (JWTCreationException exception){
            throw new NegocioException(CodigoError.ERROR_INTERNO, "Error al generar el token");
        }
    }

    private Instant generatorDateExpiration(){
        return Instant.now()
                .plus(15, ChronoUnit.MINUTES);
    }

    public String getSubject(String token){
        if(token == null || token.isEmpty()) {
            throw new NegocioException(CodigoError.TOKEN_INVALIDO, "Token nulo o vacío");
        }

        try {
            Algorithm algorithm = Algorithm.HMAC256(apiSecret);
            DecodedJWT verifier = JWT.require(algorithm)
                    .withIssuer("app municipalidad")
                    .build()
                    .verify(token);
            String subject = verifier.getSubject();

            if(subject == null || subject.isEmpty()){
                throw new NegocioException(CodigoError.TOKEN_INVALIDO, "Token sin sujeto válido");
            }

            return subject;

        } catch (JWTVerificationException e){
            throw new NegocioException(CodigoError.TOKEN_INVALIDO, "Token inválido o expirado");
        }
    }
}
