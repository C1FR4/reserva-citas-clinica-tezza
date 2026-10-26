package com.clinica.clinica.controlador;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.PacienteRepository;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final MedicoRepository medicos;
    private final BCryptPasswordEncoder encoder;

    public LoginController(UsuarioRepository usuarios, PacienteRepository pacientes,
                           MedicoRepository medicos,
                           BCryptPasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.medicos = medicos;
        this.encoder = encoder;
    }

    public record Credenciales(String email, String password) {}
    public record PacienteActual(Integer id_paciente, String nombre, String apellido) {}
    public record Acceso(String destino) {}

    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(@RequestBody Credenciales credenciales,
                                             HttpServletRequest request) {
        if (credenciales == null || credenciales.email() == null ||
            credenciales.password() == null || credenciales.email().isBlank() ||
            credenciales.email().length() > 120) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ingresa un correo y una contraseña válidos."));
        }

        Usuario usuario = usuarios.findByNombreUsuario(credenciales.email().trim().toLowerCase())
                .orElse(null);
        if (usuario == null || !("PACIENTE".equals(usuario.getRol()) ||
            "ADMINISTRADOR".equals(usuario.getRol()) || "MEDICO".equals(usuario.getRol())) ||
            !encoder.matches(credenciales.password(), usuario.getContrasena())) {
            LOG.warn("Intento de acceso rechazado por credenciales inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas."));
        }
        Paciente paciente = "PACIENTE".equals(usuario.getRol())
                ? pacientes.findByUsuario(usuario).orElse(null) : null;
        if ("PACIENTE".equals(usuario.getRol()) && paciente == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas."));
        }
        Medico medico = "MEDICO".equals(usuario.getRol())
                ? medicos.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElse(null) : null;
        if ("MEDICO".equals(usuario.getRol()) && (medico == null || !"ACTIVO".equals(medico.getEstado()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas."));
        }

        HttpSession sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        sesion = request.getSession(true);
        sesion.setAttribute("usuarioId", usuario.getIdUsuario());
        sesion.setAttribute("rol", usuario.getRol());
        LOG.info("Acceso autorizado para rol {}", usuario.getRol());
        return ResponseEntity.ok(new Acceso("ADMINISTRADOR".equals(usuario.getRol())
                ? "/admin" : "MEDICO".equals(usuario.getRol()) ? "/medico" : "/PantallaPrincipal.html"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> sesionActual(HttpServletRequest request) {
        Paciente paciente = buscarPacienteActual(request);
        if (paciente == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesión."));
        }
        return ResponseEntity.ok(new PacienteActual(paciente.getIdPaciente(),
                paciente.getNombre(), paciente.getApellido()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        return ResponseEntity.noContent().build();
    }

    private Paciente buscarPacienteActual(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !"PACIENTE".equals(sesion.getAttribute("rol"))) return null;
        Object id = sesion.getAttribute("usuarioId");
        if (!(id instanceof Integer usuarioId)) return null;
        return pacientes.findByUsuario_IdUsuario(usuarioId).orElse(null);
    }
}
