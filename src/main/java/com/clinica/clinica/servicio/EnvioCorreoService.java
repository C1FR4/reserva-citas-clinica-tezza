package com.clinica.clinica.servicio;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvioCorreoService {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioCorreoService.class);
    private final JdbcTemplate jdbc;
    private final JavaMailSender correo;
    private final Clock reloj;
    private final String remitente;

    public EnvioCorreoService(JdbcTemplate jdbc, JavaMailSender correo, Clock reloj,
                             @Value("${clinica.correo.remitente}") String remitente) {
        this.jdbc = jdbc;
        this.correo = correo;
        this.reloj = reloj;
        this.remitente = remitente;
    }

    @Transactional
    public void enviar(int idRecordatorio, int idCita) {
        // Cita primero: el mismo orden de bloqueo que modificación/cancelación.
        var citas = jdbc.queryForList("SELECT c.*, u.usuario AS correo, p.nombre AS paciente, "
                        + "CONCAT(m.nombre,' ',m.apellido) AS medico, m.especialidad FROM cita c "
                        + "JOIN paciente p ON p.id_paciente=c.id_paciente "
                        + "JOIN usuario u ON u.id_usuario=p.id_usuario "
                        + "JOIN medico m ON m.id_medico=c.id_medico WHERE c.id_cita=? FOR UPDATE", idCita);
        if (citas.isEmpty()) return;
        var filas = jdbc.queryForList("SELECT * FROM recordatorio WHERE id_recordatorio=? FOR UPDATE", idRecordatorio);
        if (filas.isEmpty()) return;
        var trabajo = filas.getFirst();
        var cita = citas.getFirst();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int intentos = ((Number) trabajo.get("intentos")).intValue();
        if ("ENVIADO".equals(trabajo.get("estado")) || intentos >= 5
                || fechaHora(trabajo.get("proximo_intento")).isAfter(ahora)) return;
        String tipo = (String) trabajo.get("tipo");
        LocalDateTime atencion = ((java.sql.Date) cita.get("fecha")).toLocalDate()
                .atTime(((java.sql.Time) cita.get("hora")).toLocalTime());
        boolean vigente = ((Number) cita.get("version_notificacion")).intValue()
                == ((Number) trabajo.get("version_cita")).intValue();
        boolean cancelacion = CitaNotificable.CANCELACION.equals(tipo);
        if (!vigente || (!cancelacion && ("CANCELADA".equals(cita.get("estado")) || !atencion.isAfter(ahora)))
                || (cancelacion && !"CANCELADA".equals(cita.get("estado")))) {
            jdbc.update("DELETE FROM recordatorio WHERE id_recordatorio=?", idRecordatorio);
            return;
        }
        String destino = (String) cita.get("correo");
        if (!EmailValidator.getInstance(true).isValid(destino)) {
            jdbc.update("UPDATE recordatorio SET estado='FALLIDO',intentos=5 WHERE id_recordatorio=?", idRecordatorio);
            LOG.warn("Correo no disponible para notificación {}", idRecordatorio);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destino);
            String asunto = switch (tipo) {
                case "RECORDATORIO" -> "Recordatorio de tu cita";
                case "CAMBIO" -> "Tu cita fue modificada";
                case "CANCELACION" -> "Tu cita fue cancelada";
                default -> "Ficha de tu cita reservada";
            };
            mensaje.setSubject("Clínica Tezza - " + asunto + " #" + idCita);
            mensaje.setText("Hola, " + cita.get("paciente") + ".\n\n" + asunto + ".\n"
                    + "Código: " + idCita + "\nMédico: " + cita.get("medico")
                    + "\nEspecialidad: " + cita.get("especialidad") + "\nFecha y hora: "
                    + atencion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " (hora de Lima)"
                    + "\nModalidad: " + cita.get("tipo_consulta")
                    + "\n\nConsulta los detalles actualizados en Mis citas de la plataforma."
                    + (cancelacion ? "\nEsta reserva ya no está activa." : "\nLlega con anticipación a tu atención.")
                    + "\n\nClínica Tezza\nMensaje automático. No envíes datos médicos ni contraseñas en respuesta.");
            correo.send(mensaje);
            jdbc.update("UPDATE recordatorio SET estado='ENVIADO',fecha_envio=?,intentos=intentos+1 "
                    + "WHERE id_recordatorio=?", Timestamp.valueOf(ahora), idRecordatorio);
        } catch (org.springframework.mail.MailException | IllegalArgumentException error) {
            jdbc.update("UPDATE recordatorio SET estado='FALLIDO',intentos=intentos+1,proximo_intento=? "
                            + "WHERE id_recordatorio=?", Timestamp.valueOf(ahora.plusMinutes(1L << intentos)), idRecordatorio);
            LOG.warn("Notificación {} pendiente de reintento SMTP (intento {})", idRecordatorio, intentos + 1);
        }
    }

    private LocalDateTime fechaHora(Object valor) {
        return valor instanceof LocalDateTime fecha ? fecha : ((Timestamp) valor).toLocalDateTime();
    }
}
