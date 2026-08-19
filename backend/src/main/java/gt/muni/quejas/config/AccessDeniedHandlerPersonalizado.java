package gt.muni.quejas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import gt.muni.quejas.dto.DtoError;
import gt.muni.quejas.exception.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccessDeniedHandlerPersonalizado implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public AccessDeniedHandlerPersonalizado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        DtoError error = DtoError.of(
                CodigoError.ACCESO_DENEGADO.getHttpStatus().value(),
                CodigoError.ACCESO_DENEGADO.name(),
                CodigoError.ACCESO_DENEGADO.getMensajePorDefecto(),
                request.getRequestURI()
        );

        response.setStatus(CodigoError.ACCESO_DENEGADO.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}