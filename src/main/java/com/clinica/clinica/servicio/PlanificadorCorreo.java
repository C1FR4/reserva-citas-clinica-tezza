package com.clinica.clinica.servicio;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanificadorCorreo {
    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public PlanificadorCorreo(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void planificar(CitaNotificable evento) {
        // Se escribe en la misma transacción de la cita; nunca se envía antes del commit.
        jdbc.update("UPDATE cita SET version_notificacion=version_notificacion+1 WHERE id_cita=?", evento.idCita());
        var cita = jdbc.queryForMap("SELECT fecha, hora, version_notificacion FROM cita WHERE id_cita=?", evento.idCita());
        int version = ((Number) cita.get("version_notificacion")).intValue();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime atencion = ((java.sql.Date) cita.get("fecha")).toLocalDate()
                .atTime(((java.sql.Time) cita.get("hora")).toLocalTime());
        jdbc.update("DELETE FROM recordatorio WHERE id_cita=? AND estado<>'ENVIADO'", evento.idCita());
        agregar(evento.idCita(), version, evento.tipo(), ahora);
        // Una reserva tardía recibe la ficha; no un segundo correo idéntico inmediato.
        if (!CitaNotificable.CANCELACION.equals(evento.tipo()) && atencion.minusHours(24).isAfter(ahora)) {
            agregar(evento.idCita(), version, "RECORDATORIO", atencion.minusHours(24));
        }
    }

    private void agregar(int id, int version, String tipo, LocalDateTime fecha) {
        jdbc.update("INSERT INTO recordatorio (id_cita,tipo,version_cita,mensaje,programado_para,proximo_intento) "
                        + "VALUES (?,?,?,?,?,?)", id, tipo, version, tipo,
                Timestamp.valueOf(fecha), Timestamp.valueOf(fecha));
    }
}
