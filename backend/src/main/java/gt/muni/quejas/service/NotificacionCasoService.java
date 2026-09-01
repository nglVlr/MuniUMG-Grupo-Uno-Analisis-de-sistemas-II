package gt.muni.quejas.service;

import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Notificacion;
import gt.muni.quejas.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificacionCasoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCasoService.class);
    private static final int MAX_INTENTOS = 3;

    private final EmailService emailService;
    private final NotificacionRepository notificacionRepository;
    private final BitacoraCambioService bitacoraCambioService;

    public NotificacionCasoService(EmailService emailService,
                                   NotificacionRepository notificacionRepository,
                                   BitacoraCambioService bitacoraCambioService) {
        this.emailService = emailService;
        this.notificacionRepository = notificacionRepository;
        this.bitacoraCambioService = bitacoraCambioService;
    }

    public void notificarRegistro(Caso caso) {
        String cuerpo = "Su caso fue registrado exitosamente. Su código de seguimiento es: "
                + caso.getCodigoSeguimiento() + ".";
        int intentos = 0;
        Exception ultimoError = null;

        while (intentos < MAX_INTENTOS) {
            intentos++;
            try {
                emailService.enviarCorreo(
                        caso.getCiudadano().getCorreo(),
                        "Registro del caso - Municipalidad de Granados",
                        cuerpo
                );
                guardar(caso, cuerpo, Notificacion.RESULTADO_ENVIADA, intentos);
                return;
            } catch (Exception ex) {
                ultimoError = ex;
                log.warn("Fallo envío de notificación de registro, intento {}", intentos, ex);
            }
        }

        guardar(caso, cuerpo, Notificacion.RESULTADO_ERROR, intentos);
        bitacoraCambioService.registrar(
                "Error envío de notificación",
                caso.getCiudadano().getDpi(),
                caso,
                caso.getIpOrigen(),
                ultimoError == null ? "No se pudo enviar el correo de registro" : ultimoError.getMessage()
        );
    }

    private void guardar(Caso caso, String cuerpo, String resultado, int intentos) {
        Notificacion notificacion = new Notificacion();
        notificacion.setEvento(Notificacion.EVENTO_REGISTRO);
        notificacion.setDestinatario(caso.getCiudadano().getCorreo());
        notificacion.setCuerpo(cuerpo);
        notificacion.setResultado(resultado);
        notificacion.setIntentos(intentos);
        notificacion.setCaso(caso);
        notificacion.setCreateAt(LocalDateTime.now());
        notificacionRepository.save(notificacion);
    }
}
