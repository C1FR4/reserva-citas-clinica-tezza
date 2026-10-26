package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Medico;
import com.google.common.base.CharMatcher;
import org.apache.commons.validator.routines.EmailValidator;
import com.clinica.clinica.repositorio.MedicoRepository;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private static final Set<String> DIAS = Set.of("LUNES", "MARTES", "MIERCOLES",
            "JUEVES", "VIERNES", "SABADO", "DOMINGO");

    private final MedicoRepository medicos;
    private final JdbcTemplate jdbc;

    public AdminService(MedicoRepository medicos, JdbcTemplate jdbc) {
        this.medicos = medicos;
        this.jdbc = jdbc;
    }

    public List<Medico> medicos() {
        return medicos.findAll();
    }

    public Medico medico(int id) {
        return medicos.findById(id).orElse(null);
    }

    public List<Map<String, Object>> horarios() {
        return jdbc.queryForList("SELECT h.id_horario, h.id_medico, " +
                "CONCAT(m.nombre,' ',m.apellido) AS medico, m.especialidad, h.dia, " +
                "TIME_FORMAT(h.hora_inicio,'%H:%i') AS inicio, " +
                "TIME_FORMAT(h.hora_fin,'%H:%i') AS fin " +
                "FROM horario h JOIN medico m ON m.id_medico=h.id_medico " +
                "ORDER BY m.apellido, CASE h.dia WHEN 'LUNES' THEN 1 WHEN 'MARTES' THEN 2 " +
                "WHEN 'MIERCOLES' THEN 3 WHEN 'JUEVES' THEN 4 WHEN 'VIERNES' THEN 5 " +
                "WHEN 'SABADO' THEN 6 ELSE 7 END, h.hora_inicio");
    }

    public List<Map<String, Object>> citas() {
        return jdbc.queryForList("SELECT c.id_cita, DATE_FORMAT(c.fecha,'%d/%m/%Y') AS fecha, " +
                "TIME_FORMAT(c.hora,'%H:%i') AS hora, c.estado, c.asistencia, c.tipo_consulta, " +
                "CONCAT(p.nombre,' ',p.apellido) AS paciente, p.dni AS dni_paciente, " +
                "CONCAT(m.nombre,' ',m.apellido) AS medico, m.especialidad " +
                "FROM cita c JOIN paciente p ON p.id_paciente=c.id_paciente " +
                "JOIN medico m ON m.id_medico=c.id_medico " +
                "ORDER BY c.fecha DESC, c.hora DESC");
    }

    public int totalCitas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class);
    }

    @Transactional
    public void guardarMedico(Integer id, String nombre, String apellido, String dni,
                              String correo, String especialidad, String telefono, String estado) {
        nombre = obligatorio(nombre, "nombre", 80);
        apellido = obligatorio(apellido, "apellido", 100);
        especialidad = obligatorio(especialidad, "especialidad", 100);
        correo = obligatorio(correo, "correo", 120).toLowerCase();
        dni = obligatorio(dni, "DNI", 8);
        if (!dni.matches("[0-9]{8}")) throw new IllegalArgumentException("El DNI debe tener 8 dígitos.");
        if (!EmailValidator.getInstance().isValid(correo)
                && !correo.matches("^[^@\\s]+@tezza\\.test$")) {
            throw new IllegalArgumentException("Ingresa un correo válido.");
        }
        if (telefono != null && telefono.trim().length() > 20) {
            throw new IllegalArgumentException("El teléfono no puede superar 20 caracteres.");
        }
        if (!Set.of("ACTIVO", "INACTIVO").contains(estado)) {
            throw new IllegalArgumentException("El estado del médico no es válido.");
        }
        if (id != null && "INACTIVO".equals(estado)) {
            Integer futuras = jdbc.queryForObject("SELECT COUNT(*) FROM cita " +
                    "WHERE id_medico=? AND estado<>'CANCELADA' " +
                    "AND TIMESTAMP(fecha,hora)>CURRENT_TIMESTAMP", Integer.class, id);
            if (futuras != null && futuras > 0) {
                throw new IllegalArgumentException("Este médico tiene citas futuras; revísalas antes de desactivarlo.");
            }
        }
        try {
            if (id == null) {
                jdbc.update("INSERT INTO medico (nombre,apellido,dni,correo,especialidad,telefono,estado,anios_experiencia,id_usuario) " +
                        "VALUES (?,?,?,?,?,?,?,0,NULL)", nombre, apellido, dni, correo,
                        especialidad, vacioANulo(telefono), estado);
            } else {
                int cambios = jdbc.update("UPDATE medico SET nombre=?,apellido=?,dni=?,correo=?," +
                                "especialidad=?,telefono=?,estado=? WHERE id_medico=?",
                        nombre, apellido, dni, correo, especialidad, vacioANulo(telefono), estado, id);
                if (cambios == 0) throw new IllegalArgumentException("El médico ya no existe.");
            }
        } catch (DataIntegrityViolationException error) {
            throw new IllegalArgumentException("El DNI o correo ya está registrado.");
        }
    }

    @Transactional
    public void agregarHorario(int idMedico, String dia, String inicioTexto, String finTexto) {
        if (!DIAS.contains(dia)) throw new IllegalArgumentException("Selecciona un día válido.");
        LocalTime inicio;
        LocalTime fin;
        try {
            inicio = LocalTime.parse(inicioTexto);
            fin = LocalTime.parse(finTexto);
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Ingresa horas válidas.");
        }
        if (Duration.between(inicio, fin).toMinutes() < 30) {
            throw new IllegalArgumentException("El turno debe durar al menos 30 minutos.");
        }
        Medico medico = medicos.findById(idMedico).orElse(null);
        if (medico == null) throw new IllegalArgumentException("Selecciona un médico registrado.");
        if (!"ACTIVO".equals(medico.getEstado())) {
            throw new IllegalArgumentException("Activa al médico antes de asignarle turnos.");
        }
        Integer solapados = jdbc.queryForObject("SELECT COUNT(*) FROM horario " +
                "WHERE id_medico=? AND dia=? AND hora_inicio<? AND hora_fin>?",
                Integer.class, idMedico, dia, fin, inicio);
        if (solapados != null && solapados > 0) {
            throw new IllegalArgumentException("Ese turno se superpone con otro horario del médico.");
        }
        try {
            jdbc.update("INSERT INTO horario (id_medico,dia,hora_inicio,hora_fin) VALUES (?,?,?,?)",
                    idMedico, dia, inicio, fin);
        } catch (DataIntegrityViolationException error) {
            throw new IllegalArgumentException("Ese horario ya existe o no es válido.");
        }
    }

    @Transactional
    public void quitarHorario(int idHorario) {
        int cambios = jdbc.update("DELETE FROM horario WHERE id_horario=? AND NOT EXISTS " +
                "(SELECT 1 FROM cita WHERE id_horario=?)", idHorario, idHorario);
        if (cambios == 0) {
            throw new IllegalArgumentException("El horario tiene citas asociadas o ya no existe.");
        }
    }

    @Transactional
    public void eliminarCitaCancelada(int idCita) {
        int cambios = jdbc.update("DELETE FROM cita WHERE id_cita=? AND estado='CANCELADA'", idCita);
        if (cambios == 0) {
            throw new IllegalArgumentException("Solo se pueden eliminar físicamente citas canceladas.");
        }
    }

    private String obligatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || CharMatcher.whitespace().trimFrom(valor).length() > maximo) {
            throw new IllegalArgumentException("Revisa el campo " + campo + ".");
        }
        return CharMatcher.whitespace().trimFrom(valor);
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
