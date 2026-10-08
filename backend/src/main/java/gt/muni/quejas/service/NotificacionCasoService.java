package gt.muni.quejas.service;

import gt.muni.quejas.model.Caso;
import gt.muni.quejas.model.Notificacion;
import gt.muni.quejas.model.Usuario;
import gt.muni.quejas.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// CU-09 Notificaciones: arma el mensaje del evento, lo envía, reintenta hasta 3
// veces y deja registro del envío o del error. Nunca interrumpe la operación
// principal: si el correo falla, el caso de uso que la invocó continúa igual.
@Service
public class NotificacionCasoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionCasoService.class);
    private static final int MAX_INTENTOS = 3;
    private static final String FIRMA = " - Municipalidad de Granados";

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

    // ---------- CU-02: el ciudadano registra su caso ----------
    public void notificarRegistro(Caso caso) {
        String cuerpo = "Su caso fue registrado exitosamente. Su código de seguimiento es: "
                + caso.getCodigoSeguimiento() + ".";

        notificar(Notificacion.EVENTO_REGISTRO, caso.getCiudadano().getCorreo(),
                "Registro del caso" + FIRMA, cuerpo,
                caso, caso.getCiudadano().getDpi(), caso.getIpOrigen());
    }

    // ---------- CU-04 FA03: el Auxiliar pide ampliación de información (NOT01) ----------
    public void notificarAmpliacion(Caso caso, Usuario auxiliar, String ip) {
        String cuerpo = "Su caso " + caso.getCodigoSeguimiento() + " requiere ampliación de información. "
                + "Comentario del Auxiliar: " + caso.getComentarioRechazo();

        notificar(Notificacion.EVENTO_AMPLIACION, caso.getCiudadano().getCorreo(),
                "Su caso requiere ampliación de información" + FIRMA, cuerpo,
                caso, auxiliar.getCui(), ip);
    }

    // ---------- CU-04: el sistema asigna el caso a un Técnico ----------
    public void notificarAsignacionTecnico(Caso caso, Usuario auxiliar, String ip) {
        if (caso.getTecnico() == null) {
            return;
        }
        String cuerpo = "Le fue asignado el caso " + caso.getCodigoSeguimiento()
                + " del departamento de " + caso.getDepartamento().getNombre()
                + ". Ingrese al sistema para atenderlo.";

        notificar(Notificacion.EVENTO_ASIGNACION, caso.getTecnico().getCorreo(),
                "Nuevo caso asignado" + FIRMA, cuerpo,
                caso, auxiliar.getCui(), ip);
    }

    // ---------- CU-06 FA02: el Supervisor devuelve el caso al Técnico ----------
    public void notificarDevolucionAlTecnico(Caso caso, String observaciones, Usuario supervisor, String ip) {
        if (caso.getTecnico() == null) {
            return;
        }
        String cuerpo = "El caso " + caso.getCodigoSeguimiento() + " le fue devuelto por el Supervisor. "
                + "Observaciones: " + observaciones;

        notificar(Notificacion.EVENTO_DEVOLUCION, caso.getTecnico().getCorreo(),
                "Caso devuelto por el Supervisor" + FIRMA, cuerpo,
                caso, supervisor.getCui(), ip);
    }

    // ---------- CU-07 FA02: la Jefatura devuelve el caso a Supervisión ----------
    public void notificarDevolucionASupervision(Caso caso, String observaciones,
                                                List<Usuario> supervisores, Usuario jefatura, String ip) {
        String cuerpo = "El caso " + caso.getCodigoSeguimiento() + " fue devuelto por la Jefatura. "
                + "Observaciones: " + observaciones;

        for (int i = 0; i < supervisores.size(); i++) {
            notificar(Notificacion.EVENTO_DEVOLUCION, supervisores.get(i).getCorreo(),
                    "Caso devuelto por la Jefatura" + FIRMA, cuerpo,
                    caso, jefatura.getCui(), ip);
        }
    }

    // ---------- CU-06 FA03: el Supervisor aprueba el rechazo del Auxiliar ----------
    public void notificarRechazo(Caso caso, String estadoFinal, Usuario supervisor, String ip) {
        String motivo = caso.getMotivoRechazo() == null ? "" : caso.getMotivoRechazo().getMotivo();
        String cuerpo = "Su caso " + caso.getCodigoSeguimiento() + " quedó en estado " + estadoFinal
                + ". Motivo: " + motivo;

        notificar(Notificacion.EVENTO_RECHAZO, caso.getCiudadano().getCorreo(),
                "Resolución de su caso" + FIRMA, cuerpo,
                caso, supervisor.getCui(), ip);
    }

    // ---------- CU-07: la Jefatura firma el oficio y el caso queda Resuelto ----------
    public void notificarResolucion(Caso caso, Usuario jefatura, String ip) {
        String cuerpo = "Su caso " + caso.getCodigoSeguimiento() + " fue resuelto. "
                + "El oficio de resolución fue firmado por el Jefe de Departamento de "
                + caso.getDepartamento().getNombre() + ".";

        notificar(Notificacion.EVENTO_RESOLUCION, caso.getCiudadano().getCorreo(),
                "Su caso fue resuelto" + FIRMA, cuerpo,
                caso, jefatura.getCui(), ip);
    }

    // ---------- Flujo básico del CU-09: enviar, reintentar y registrar ----------
    private void notificar(String evento, String destinatario, String asunto, String cuerpo,
                           Caso caso, String usuarioResponsable, String ip) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Notificación de {} sin destinatario, no se envía", evento);
            return;
        }

        int intentos = 0;
        Exception ultimoError = null;

        // FA01: se reintenta el envío hasta 3 veces
        while (intentos < MAX_INTENTOS) {
            intentos++;
            try {
                emailService.enviarCorreo(destinatario, asunto, cuerpo);
                guardar(evento, destinatario, cuerpo, Notificacion.RESULTADO_ENVIADA, intentos, caso);
                return;
            } catch (Exception ex) {
                ultimoError = ex;
                log.warn("Fallo envío de notificación de {}, intento {}", evento, intentos, ex);
            }
        }

        // FA01: si sigue fallando se registra el error para revisión del Admin
        guardar(evento, destinatario, cuerpo, Notificacion.RESULTADO_ERROR, intentos, caso);
        bitacoraCambioService.registrar(
                "Error envío de notificación",
                usuarioResponsable,
                caso,
                ip,
                "Evento " + evento + ": "
                        + (ultimoError == null ? "no se pudo enviar el correo" : ultimoError.getMessage())
        );
    }

    private void guardar(String evento, String destinatario, String cuerpo,
                         String resultado, int intentos, Caso caso) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setEvento(evento);
            notificacion.setDestinatario(destinatario);
            notificacion.setCuerpo(cuerpo);
            notificacion.setResultado(resultado);
            notificacion.setIntentos(intentos);
            notificacion.setCaso(caso);
            notificacion.setCreateAt(LocalDateTime.now());
            notificacionRepository.save(notificacion);
        } catch (Exception ex) {
            // El registro de la notificación no debe tumbar la operación principal
            log.error("No fue posible guardar el registro de la notificación de {}", evento, ex);
        }
    }
}
