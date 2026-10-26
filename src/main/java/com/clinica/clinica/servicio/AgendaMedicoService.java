package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.repositorio.MedicoRepository;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta la agenda del médico y registra la asistencia de sus propias citas. */
@Service
public class AgendaMedicoService {
    private final MedicoRepository medicos;
    private final JdbcTemplate jdbc;

    public AgendaMedicoService(MedicoRepository medicos, JdbcTemplate jdbc) {
        this.medicos = medicos;
        this.jdbc = jdbc;
    }

    /** Devuelve el perfil médico vinculado a la cuenta autenticada, si existe. */
    public Medico medicoDeUsuario(int idUsuario) {
        return medicos.findByUsuario_IdUsuario(idUsuario).orElse(null);
    }

    /** Lista únicamente las citas asignadas al médico indicado. */
    public List<Map<String, Object>> citas(int idMedico) {
        return jdbc.queryForList("SELECT c.id_cita, DATE_FORMAT(c.fecha,'%d/%m/%Y') AS fecha, " +
                "c.fecha AS fecha_orden, TIME_FORMAT(c.hora,'%H:%i') AS hora, " +
                "c.tipo_consulta, c.estado, c.asistencia, " +
                "CONCAT(p.nombre,' ',p.apellido) AS paciente, p.dni " +
                "FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente " +
                "WHERE c.id_medico=? ORDER BY c.fecha DESC,c.hora DESC", idMedico);
    }

    /** Busca una cita por su identificador y por el médico propietario. */
    public Map<String, Object> cita(int idMedico, int idCita) {
        var resultados = jdbc.queryForList("SELECT c.id_cita, DATE_FORMAT(c.fecha,'%d/%m/%Y') AS fecha, " +
                "c.fecha AS fecha_orden, TIME_FORMAT(c.hora,'%H:%i') AS hora, " +
                "c.tipo_consulta, c.motivo, c.estado, c.asistencia, " +
                "CONCAT(p.nombre,' ',p.apellido) AS paciente, p.dni, " +
                "m.especialidad FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente " +
                "JOIN medico m ON m.id_medico=c.id_medico " +
                "WHERE c.id_medico=? AND c.id_cita=?", idMedico, idCita);
        return resultados.isEmpty() ? null : resultados.getFirst();
    }

    /** Registra asistencia solo en una cita propia, activa, iniciada y aún pendiente. */
    @Transactional
    public void registrarAsistencia(int idMedico, int idCita, String asistencia) {
        if (!("ASISTIO".equals(asistencia) || "NO_ASISTIO".equals(asistencia))) {
            throw new IllegalArgumentException("Selecciona un estado de asistencia válido.");
        }
        int cambios = jdbc.update("UPDATE cita SET asistencia=? " +
                        "WHERE id_cita=? AND id_medico=? AND estado<>'CANCELADA' " +
                        "AND asistencia='PENDIENTE' AND TIMESTAMP(fecha,hora)<=CURRENT_TIMESTAMP",
                asistencia, idCita, idMedico);
        if (cambios == 0) {
            throw new IllegalArgumentException("La cita no pertenece a tu agenda, aún no comenzó o ya fue registrada.");
        }
    }
}
