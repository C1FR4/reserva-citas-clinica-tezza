package com.clinica.clinica.controlador;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.repositorio.PacienteRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.sql.Statement;

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DIAS = List.of("LUNES", "MARTES", "MIERCOLES",
            "JUEVES", "VIERNES", "SABADO", "DOMINGO");

    private final JdbcTemplate jdbc;
    private final PacienteRepository pacientes;

    public CitaController(JdbcTemplate jdbc, PacienteRepository pacientes) {
        this.jdbc = jdbc;
        this.pacientes = pacientes;
    }

    public record MedicoOpcion(int idMedico, String nombre, String especialidad) {}
    public record HorarioOpcion(int idHorario, String hora) {}
    public record NuevaCita(int idMedico, int idHorario, String fecha, String hora,
                            String tipoConsulta, String motivo) {}
    public record CitaResumen(int idCita, String fecha, String hora, String medico,
                              String especialidad, String tipoConsulta, String estado) {}

    @GetMapping("/medicos")
    public ResponseEntity<?> medicos(HttpServletRequest request) {
        if (pacienteActual(request) == null) return sinSesion();
        List<MedicoOpcion> opciones = jdbc.query(
                "SELECT id_medico, CONCAT(nombre, ' ', apellido) AS nombre, especialidad " +
                "FROM medico WHERE estado='ACTIVO' ORDER BY apellido, nombre",
                (rs, row) -> new MedicoOpcion(rs.getInt("id_medico"), rs.getString("nombre"),
                        rs.getString("especialidad")));
        return ResponseEntity.ok(opciones);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> disponibilidad(@RequestParam int idMedico, @RequestParam String fecha,
                                             HttpServletRequest request) {
        if (pacienteActual(request) == null) return sinSesion();
        try {
            LocalDate dia = LocalDate.parse(fecha);
            if (dia.isBefore(LocalDate.now()) || idMedico < 1) return error(HttpStatus.BAD_REQUEST, "Elige una fecha futura.");
            return ResponseEntity.ok(opciones(idMedico, dia));
        } catch (DateTimeException error) {
            return error(HttpStatus.BAD_REQUEST, "La fecha no es válida.");
        }
    }

    @PostMapping
    public ResponseEntity<?> reservar(@RequestBody NuevaCita nueva, HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        if (nueva == null || nueva.fecha() == null || nueva.hora() == null ||
            nueva.idMedico() < 1 || nueva.idHorario() < 1 ||
            !("PRESENCIAL".equals(nueva.tipoConsulta()) || "VIRTUAL".equals(nueva.tipoConsulta())) ||
            (nueva.motivo() != null && nueva.motivo().length() > 255)) {
            return error(HttpStatus.BAD_REQUEST, "Revisa los datos de la cita.");
        }
        try {
            LocalDate fecha = LocalDate.parse(nueva.fecha());
            LocalTime hora = LocalTime.parse(nueva.hora());
            if (!LocalDateTime.of(fecha, hora).isAfter(LocalDateTime.now())) {
                return error(HttpStatus.BAD_REQUEST, "Elige un horario futuro.");
            }
            boolean valido = opciones(nueva.idMedico(), fecha).stream()
                    .anyMatch(opcion -> opcion.idHorario() == nueva.idHorario() &&
                            opcion.hora().equals(hora.format(HORA)));
            if (!valido) return error(HttpStatus.CONFLICT, "Ese horario ya no está disponible.");

            GeneratedKeyHolder clave = new GeneratedKeyHolder();
            jdbc.update(conexion -> {
                var ps = conexion.prepareStatement(
                        "INSERT INTO cita (fecha, hora, tipo_consulta, motivo, id_paciente, id_medico, id_horario) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setObject(1, fecha);
                ps.setObject(2, hora);
                ps.setString(3, nueva.tipoConsulta());
                ps.setString(4, nueva.motivo());
                ps.setInt(5, paciente.getIdPaciente());
                ps.setInt(6, nueva.idMedico());
                ps.setInt(7, nueva.idHorario());
                return ps;
            }, clave);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("idCita", clave.getKey().intValue()));
        } catch (DateTimeException error) {
            return error(HttpStatus.BAD_REQUEST, "Fecha u hora no válida.");
        } catch (DataIntegrityViolationException error) {
            return error(HttpStatus.CONFLICT, "Ese horario ya no está disponible.");
        }
    }

    @GetMapping("/mias")
    public ResponseEntity<?> misCitas(HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        List<CitaResumen> citas = jdbc.query(
                "SELECT c.id_cita, c.fecha, TIME_FORMAT(c.hora,'%H:%i') AS hora, " +
                "CONCAT(m.nombre,' ',m.apellido) AS medico, m.especialidad, c.tipo_consulta, c.estado " +
                "FROM cita c JOIN medico m ON m.id_medico=c.id_medico " +
                "WHERE c.id_paciente=? ORDER BY c.fecha DESC, c.hora DESC LIMIT 100",
                (rs, row) -> new CitaResumen(rs.getInt("id_cita"), rs.getDate("fecha").toLocalDate().toString(),
                        rs.getString("hora"), rs.getString("medico"), rs.getString("especialidad"),
                        rs.getString("tipo_consulta"), rs.getString("estado")), paciente.getIdPaciente());
        return ResponseEntity.ok(citas);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable int id, HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        int cambios = jdbc.update("UPDATE cita SET estado='CANCELADA' " +
                        "WHERE id_cita=? AND id_paciente=? AND estado='RESERVADA' " +
                        "AND TIMESTAMP(fecha, hora)>CURRENT_TIMESTAMP",
                id, paciente.getIdPaciente());
        if (cambios == 0) return error(HttpStatus.CONFLICT, "Esta cita no se puede cancelar.");
        return ResponseEntity.noContent().build();
    }

    private List<HorarioOpcion> opciones(int idMedico, LocalDate fecha) {
        String dia = DIAS.get(fecha.getDayOfWeek().getValue() - 1);
        List<Map<String, Object>> rangos = jdbc.queryForList(
                "SELECT h.id_horario, TIME_FORMAT(h.hora_inicio,'%H:%i') AS inicio, " +
                "TIME_FORMAT(h.hora_fin,'%H:%i') AS fin FROM horario h " +
                "JOIN medico m ON m.id_medico=h.id_medico " +
                "WHERE h.id_medico=? AND h.dia=? AND m.estado='ACTIVO' ORDER BY h.hora_inicio",
                idMedico, dia);
        Set<String> ocupadas = jdbc.query(
                "SELECT TIME_FORMAT(hora,'%H:%i') AS hora FROM cita " +
                "WHERE id_medico=? AND fecha=? AND estado<>'CANCELADA'",
                (rs, row) -> rs.getString("hora"), idMedico, fecha).stream().collect(Collectors.toSet());
        Map<String, HorarioOpcion> libres = new LinkedHashMap<>();
        for (Map<String, Object> rango : rangos) {
            LocalTime inicio = LocalTime.parse((String) rango.get("inicio"));
            LocalTime fin = LocalTime.parse((String) rango.get("fin"));
            for (LocalTime hora = inicio; Duration.between(hora, fin).toMinutes() >= 30;
                 hora = hora.plusMinutes(30)) {
                if (!fecha.atTime(hora).isAfter(LocalDateTime.now())) continue;
                String etiqueta = hora.format(HORA);
                if (!ocupadas.contains(etiqueta)) {
                    libres.putIfAbsent(etiqueta, new HorarioOpcion(((Number) rango.get("id_horario")).intValue(), etiqueta));
                }
            }
        }
        return new ArrayList<>(libres.values());
    }

    private Paciente pacienteActual(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !"PACIENTE".equals(sesion.getAttribute("rol"))) return null;
        Object id = sesion.getAttribute("usuarioId");
        if (!(id instanceof Integer usuarioId)) return null;
        return pacientes.findByUsuario_IdUsuario(usuarioId).orElse(null);
    }

    private ResponseEntity<Map<String, String>> sinSesion() {
        return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
