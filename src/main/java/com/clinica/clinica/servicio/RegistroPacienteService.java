package com.clinica.clinica.servicio;

import com.clinica.clinica.controlador.RegistroPacienteController.Registro;
import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.PacienteRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroPacienteService {
    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final BCryptPasswordEncoder encoder;
    private final PoliticaContrasena politica;

    public RegistroPacienteService(UsuarioRepository usuarios, PacienteRepository pacientes,
                                  BCryptPasswordEncoder encoder, PoliticaContrasena politica) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.encoder = encoder;
        this.politica = politica;
    }

    @Transactional
    public void registrar(Registro datos) {
        if (!politica.esValida(datos.contrasena())
                || datos.contrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña exige 8 caracteres como mínimo, minúscula, mayúscula, dígito y símbolo, sin espacios (máximo 72 bytes).");
        }
        if (!datos.contrasena().equals(datos.confirmarContrasena())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByNombreUsuario(correo).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado. Inicia sesión con tu cuenta.");
        }
        Usuario cuenta = new Usuario();
        cuenta.setNombreUsuario(correo);
        cuenta.setContrasena(encoder.encode(datos.contrasena()));
        // El alta pública nunca acepta ni deriva un rol del navegador.
        cuenta.setRol("PACIENTE");
        usuarios.saveAndFlush(cuenta);
        Paciente paciente = new Paciente();
        paciente.setNombre(datos.nombre().trim());
        paciente.setApellido(datos.apellido().trim());
        paciente.setDni(datos.dni());
        paciente.setFechaNacimiento(datos.fechaNacimiento());
        paciente.setGenero(datos.genero());
        paciente.setTelefono(datos.telefono().trim());
        paciente.setUsuario(cuenta);
        pacientes.saveAndFlush(paciente);
    }
}
