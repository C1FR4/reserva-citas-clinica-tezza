package com.clinica.clinica.controlador;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.repositorio.PacienteRepository;
import com.clinica.clinica.servicio.MantenimientoCitaService;
import com.clinica.clinica.servicio.CitaNotificable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.web.bind.annotation.PutMapping;
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
    private final MantenimientoCitaService mantenimiento;
    private final ApplicationEventPublisher eventos;

    public CitaController(JdbcTemplate jdbc, PacienteRepository pacientes,
                           MantenimientoCitaService mantenimiento, ApplicationEventPublisher eventos) {
        this.jdbc = jdbc;
        this.pacientes = pacientes;
        this.mantenimiento = mantenimiento;
        this.eventos = eventos;
    }

    public record HorarioSemanal(String dia, String inicio, String fin) {}
    public record MedicoOpcion(int idMedico, String nombre, String especialidad,
                               List<HorarioSemanal> horarios) {}
    public record HorarioOpcion(int idHorario, String hora) {}
    public record NuevaCita(int idMedico, int idHorario, String fecha, String hora,
                            String tipoConsulta, String motivo) {}
    public record CitaResumen(int idCita, String fecha, String hora, String medico,
                              String especialidad, String tipoConsulta, String estado) {}

    public record CambioCita(int idMedico, int idHorario, String fecha, String hora,
                            String tipoConsulta, String motivo) {}
    public record CitaDetalle(int idCita, int idMedico, String fecha, String hora,
                             String tipoConsulta, String motivo, String estado,
                             int numeroModificaciones) {}

    @GetMapping("/medicos")
    public ResponseEntity<?> medicos(HttpServletRequest request) {
        if (pacienteActual(request) == null) return sinSesion();
        List<MedicoOpcion> opciones = jdbc.query(
                "SELECT id_medico, CONCAT(nombre, ' ', apellido) AS nombre, especialidad " +
                "FROM medico m WHERE estado='ACTIVO' AND EXISTS " +
                "(SELECT 1 FROM horario h WHERE h.id_medico=m.id_medico) ORDER BY apellido, nombre",
                (rs, row) -> new MedicoOpcion(rs.getInt("id_medico"), rs.getString("nombre"),
                        rs.getString("especialidad"), jdbc.query(
                                "SELECT dia, TIME_FORMAT(hora_inicio,'%H:%i') AS inicio, " +
                                "TIME_FORMAT(hora_fin,'%H:%i') AS fin FROM horario " +
                                "WHERE id_medico=? ORDER BY CASE dia " +
                                "WHEN 'LUNES' THEN 1 WHEN 'MARTES' THEN 2 " +
                                "WHEN 'MIERCOLES' THEN 3 WHEN 'JUEVES' THEN 4 " +
                                "WHEN 'VIERNES' THEN 5 WHEN 'SABADO' THEN 6 ELSE 7 END, hora_inicio",
                                (horario, index) -> new HorarioSemanal(horario.getString("dia"),
                                        horario.getString("inicio"), horario.getString("fin")),
                                rs.getInt("id_medico"))));
        return ResponseEntity.ok(opciones);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<?> disponibilidad(@RequestParam int idMedico, @RequestParam String fecha,
                                             HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        try {
            LocalDate dia = LocalDate.parse(fecha);
            if (dia.isBefore(LocalDate.now()) || idMedico < 1) return error(HttpStatus.BAD_REQUEST, "Elige una fecha futura.");
            return ResponseEntity.ok(opciones(idMedico, dia, paciente.getIdPaciente()));
        } catch (DateTimeException error) {
            return error(HttpStatus.BAD_REQUEST, "La fecha no es válida.");
        }
    }

    @PostMapping
    @Transactional
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
            boolean valido = opciones(nueva.idMedico(), fecha, paciente.getIdPaciente()).stream()
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
            int idCita = clave.getKey().intValue();
            eventos.publishEvent(new CitaNotificable(idCita, CitaNotificable.FICHA));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("idCita", idCita));
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

    @GetMapping("/{id}")
    public ResponseEntity<?> detalle(@PathVariable int id, HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        var citas = jdbc.query("SELECT id_cita, id_medico, fecha, " +
                        "TIME_FORMAT(hora,'%H:%i') AS hora, tipo_consulta, motivo, " +
                        "estado, numero_modificaciones FROM cita " +
                        "WHERE id_cita=? AND id_paciente=?",
                (rs, row) -> new CitaDetalle(rs.getInt("id_cita"), rs.getInt("id_medico"),
                        rs.getDate("fecha").toLocalDate().toString(), rs.getString("hora"),
                        rs.getString("tipo_consulta"), rs.getString("motivo"),
                        rs.getString("estado"), rs.getInt("numero_modificaciones")),
                id, paciente.getIdPaciente());
        return citas.isEmpty() ? error(HttpStatus.NOT_FOUND, "No encontramos esa cita.")
                : ResponseEntity.ok(citas.getFirst());
    }

    @PostMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<?> cancelar(@PathVariable int id, HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        int cambios = jdbc.update("UPDATE cita SET estado='CANCELADA' " +
                        "WHERE id_cita=? AND id_paciente=? AND estado='RESERVADA' " +
                        "AND TIMESTAMP(fecha, hora)>=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 24 HOUR)",
                id, paciente.getIdPaciente());
        if (cambios == 0) return error(HttpStatus.CONFLICT, "Esta cita no se puede cancelar.");
        eventos.publishEvent(new CitaNotificable(id, CitaNotificable.CANCELACION));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable int id, @RequestBody CambioCita cambio,
                                       HttpServletRequest request) {
        Paciente paciente = pacienteActual(request);
        if (paciente == null) return sinSesion();
        if (cambio == null || cambio.fecha() == null || cambio.hora() == null) {
            return error(HttpStatus.BAD_REQUEST, "Revisa los datos de la cita.");
        }
        try {
            mantenimiento.modificar(id, paciente.getIdPaciente(), cambio.idMedico(),
                    cambio.idHorario(), LocalDate.parse(cambio.fecha()),
                    LocalTime.parse(cambio.hora()), cambio.tipoConsulta(), cambio.motivo());
            return ResponseEntity.noContent().build();
        } catch (DateTimeException | IllegalArgumentException error) {
            return error(HttpStatus.CONFLICT, error.getMessage());
        }
    }

    private List<HorarioOpcion> opciones(int idMedico, LocalDate fecha, int idPaciente) {
        String dia = DIAS.get(fecha.getDayOfWeek().getValue() - 1);
        List<Map<String, Object>> rangos = jdbc.queryForList(
                "SELECT h.id_horario, TIME_FORMAT(h.hora_inicio,'%H:%i') AS inicio, " +
                "TIME_FORMAT(h.hora_fin,'%H:%i') AS fin FROM horario h " +
                "JOIN medico m ON m.id_medico=h.id_medico " +
                "WHERE h.id_medico=? AND h.dia=? AND m.estado='ACTIVO' ORDER BY h.hora_inicio",
                idMedico, dia);
        Set<String> ocupadas = jdbc.query(
                "SELECT TIME_FORMAT(hora,'%H:%i') AS hora FROM cita " +
                "WHERE fecha=? AND estado<>'CANCELADA' AND (id_medico=? OR id_paciente=?)",
                (rs, row) -> rs.getString("hora"), fecha, idMedico, idPaciente)
                .stream().collect(Collectors.toSet());
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
