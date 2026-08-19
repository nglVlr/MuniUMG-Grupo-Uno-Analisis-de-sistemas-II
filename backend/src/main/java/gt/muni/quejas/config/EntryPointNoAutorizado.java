package gt.muni.quejas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import gt.muni.quejas.dto.DtoError;
import gt.muni.quejas.exception.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EntryPointNoAutorizado implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public EntryPointNoAutorizado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        DtoError error = DtoError.of(
                CodigoError.TOKEN_INVALIDO.getHttpStatus().value(),
                CodigoError.TOKEN_INVALIDO.name(),
                CodigoError.TOKEN_INVALIDO.getMensajePorDefecto(),
                request.getRequestURI()
        );

        response.setStatus(CodigoError.TOKEN_INVALIDO.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
