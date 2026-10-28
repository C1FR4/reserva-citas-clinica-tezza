package com.clinica.clinica.controlador;

import com.clinica.clinica.servicio.RegistroPacienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistroPacienteController {
    private final RegistroPacienteService registro;

    public RegistroPacienteController(RegistroPacienteService registro) {
        this.registro = registro;
    }

    public record Registro(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 120) String correo,
            @NotBlank @Pattern(regexp = "[0-9]{8}") String dni,
            @NotNull @Past LocalDate fechaNacimiento,
            @NotBlank @Pattern(regexp = "FEMENINO|MASCULINO|OTRO|NO_ESPECIFICA") String genero,
            @NotBlank @Pattern(regexp = "[+0-9() -]{7,20}") String telefono,
            @NotBlank @Size(max = 72) String contrasena,
            @NotBlank String confirmarContrasena) {}

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> datosInvalidos() {
        return ResponseEntity.badRequest().body(Map.of("error",
                "Revisa tus datos: correo válido, DNI de 8 dígitos, fecha de nacimiento anterior a hoy y teléfono de 7 a 20 caracteres."));
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody Registro datos) {
        try {
            registro.registrar(datos);
            return ResponseEntity.status(201).body(Map.of("mensaje",
                    "Cuenta de paciente creada. Inicia sesión con tu correo y contraseña."));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
        } catch (DataIntegrityViolationException error) {
            return ResponseEntity.status(409).body(Map.of("error", "El correo o DNI ya está registrado."));
        }
    }
}
