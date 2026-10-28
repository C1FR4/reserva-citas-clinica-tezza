package com.clinica.clinica.servicio;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "clinica.correo.habilitado", havingValue = "true")
public class ProcesadorRecordatorios {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ProcesadorRecordatorios.class);
    private final JdbcTemplate jdbc;
    private final EnvioCorreoService envios;
    private final Clock reloj;

    public ProcesadorRecordatorios(JdbcTemplate jdbc, EnvioCorreoService envios, Clock reloj) {
        this.jdbc = jdbc;
        this.envios = envios;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${clinica.correo.intervalo-ms}", initialDelay = 5000)
    public void procesar() {
        var pendientes = jdbc.queryForList("SELECT id_recordatorio,id_cita FROM recordatorio "
                        + "WHERE estado IN ('PENDIENTE','FALLIDO') AND intentos<5 AND proximo_intento<=? "
                        + "ORDER BY proximo_intento,id_recordatorio LIMIT 25", Timestamp.valueOf(LocalDateTime.now(reloj)));
        for (var pendiente : pendientes) {
            int id = ((Number) pendiente.get("id_recordatorio")).intValue();
            try {
                envios.enviar(id, ((Number) pendiente.get("id_cita")).intValue());
            } catch (RuntimeException error) {
                LOG.error("No se pudo procesar notificación {}: {}", id, error.getClass().getSimpleName());
            }
        }
    }
}
