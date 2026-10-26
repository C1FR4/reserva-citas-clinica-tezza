package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.PacienteRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("seed")
public class SeedPaciente implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final BCryptPasswordEncoder encoder;
    private final PoliticaContrasena politica;

    @Value("${SEED_EMAIL:}") private String email;
    @Value("${SEED_PASSWORD:}") private String password;
    @Value("${SEED_NOMBRE:}") private String nombre;
    @Value("${SEED_APELLIDO:}") private String apellido;
    @Value("${SEED_DNI:}") private String dni;
    @Value("${SEED_FECHA_NACIMIENTO:}") private String fechaNacimiento;
    @Value("${SEED_GENERO:}") private String genero;

    public SeedPaciente(UsuarioRepository usuarios, PacienteRepository pacientes,
                        BCryptPasswordEncoder encoder, PoliticaContrasena politica) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.encoder = encoder;
        this.politica = politica;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || !politica.esValida(password) || nombre.isBlank() ||
            apellido.isBlank() || !dni.matches("[0-9]{8}") ||
            fechaNacimiento.isBlank() || genero.isBlank()) {
            throw new IllegalArgumentException("Configura SEED_EMAIL, SEED_PASSWORD (8+, minúscula, mayúscula, dígito y símbolo), " +
                    "SEED_NOMBRE, SEED_APELLIDO, SEED_DNI (8 dígitos), " +
                    "SEED_FECHA_NACIMIENTO (AAAA-MM-DD) y SEED_GENERO.");
        }
        String usuarioNormalizado = email.trim().toLowerCase();
        if (usuarios.findByNombreUsuario(usuarioNormalizado).isPresent()) {
            System.out.println("La cuenta de prueba ya existe; no se modificó.");
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(usuarioNormalizado);
        usuario.setContrasena(encoder.encode(password));
        usuario.setRol("PACIENTE");
        usuario = usuarios.save(usuario);

        Paciente paciente = new Paciente();
        paciente.setNombre(nombre.trim());
        paciente.setApellido(apellido.trim());
        paciente.setDni(dni);
        paciente.setGenero(genero.trim());
        paciente.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
        paciente.setUsuario(usuario);
        pacientes.save(paciente);
        System.out.println("Paciente de prueba creado.");
    }
}
