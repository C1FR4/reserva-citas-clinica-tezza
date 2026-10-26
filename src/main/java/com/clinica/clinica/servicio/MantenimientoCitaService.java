package com.clinica.clinica.servicio;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Actualiza reservas propias con controles de cupo y anticipación. */
@Service
public class MantenimientoCitaService {
    private static final List<String> DIAS = List.of("LUNES", "MARTES", "MIERCOLES",
            "JUEVES", "VIERNES", "SABADO", "DOMINGO");
    private final JdbcTemplate jdbc;

    public MantenimientoCitaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void modificar(int idCita, int idPaciente, int idMedico, int idHorario,
                          LocalDate fecha, LocalTime hora, String tipoConsulta, String motivo) {
        if (fecha == null || hora == null || idMedico < 1 || idHorario < 1 ||
                !("PRESENCIAL".equals(tipoConsulta) || "VIRTUAL".equals(tipoConsulta)) ||
                (motivo != null && motivo.length() > 255)) {
            throw new IllegalArgumentException("Revisa los datos de la cita.");
        }
        var actual = jdbc.queryForList("SELECT fecha, hora, estado, numero_modificaciones " +
                "FROM cita WHERE id_cita=? AND id_paciente=?", idCita, idPaciente);
        if (actual.isEmpty()) throw new IllegalArgumentException("La cita no existe en tu cuenta.");
        Map<String, Object> cita = actual.getFirst();
        LocalDate fechaActual = ((Date) cita.get("fecha")).toLocalDate();
        LocalTime horaActual = ((Time) cita.get("hora")).toLocalTime();
        LocalDateTime limite = LocalDateTime.now().plusHours(24);
        if (!"RESERVADA".equals(cita.get("estado")) ||
                ((Number) cita.get("numero_modificaciones")).intValue() >= 2 ||
                fechaActual.atTime(horaActual).isBefore(limite) ||
                fecha.atTime(hora).isBefore(limite)) {
            throw new IllegalArgumentException("Solo puedes modificar dos veces una reserva con al menos 24 horas de anticipación.");
        }
        String dia = DIAS.get(fecha.getDayOfWeek().getValue() - 1);
        Integer turnos = jdbc.queryForObject("SELECT COUNT(*) FROM horario h " +
                "JOIN medico m ON m.id_medico=h.id_medico " +
                "WHERE h.id_horario=? AND h.id_medico=? AND h.dia=? AND m.estado='ACTIVO' " +
                "AND ? >= h.hora_inicio AND ADDTIME(?, '00:30:00') <= h.hora_fin",
                Integer.class, idHorario, idMedico, dia, hora, hora);
        if (turnos == null || turnos == 0) {
            throw new IllegalArgumentException("La hora no pertenece al horario activo del médico.");
        }
        Integer ocupadas = jdbc.queryForObject("SELECT COUNT(*) FROM cita WHERE id_cita<>? " +
                "AND fecha=? AND hora=? AND estado<>'CANCELADA' " +
                "AND (id_medico=? OR id_paciente=?)", Integer.class,
                idCita, fecha, hora, idMedico, idPaciente);
        if (ocupadas != null && ocupadas > 0) {
            throw new IllegalArgumentException("El nuevo espacio ya está ocupado.");
        }
        try {
            int cambios = jdbc.update("UPDATE cita SET fecha=?, hora=?, id_medico=?, id_horario=?, " +
                    "tipo_consulta=?, motivo=?, numero_modificaciones=numero_modificaciones+1 " +
                    "WHERE id_cita=? AND id_paciente=? AND estado='RESERVADA' " +
                    "AND numero_modificaciones<2 AND TIMESTAMP(fecha,hora)>=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)",
                    fecha, hora, idMedico, idHorario, tipoConsulta, motivo, idCita, idPaciente);
            if (cambios == 0) throw new IllegalArgumentException("La cita cambió; vuelve a consultar tus reservas.");
        } catch (DataIntegrityViolationException error) {
            throw new IllegalArgumentException("El nuevo espacio ya está ocupado.");
        }
    }

}
