package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("seed")
public class SeedMedicoDemo implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final MedicoRepository medicos;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder;

    @Value("${SEED_ADMIN_PASSWORD:}") private String adminPassword;

    public SeedMedicoDemo(UsuarioRepository usuarios, MedicoRepository medicos,
                          JdbcTemplate jdbc, BCryptPasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Usuario usuario = usuarios.findByNombreUsuario("admin").orElse(null);
        if (usuario == null) {
            if (adminPassword.length() < 8) {
                throw new IllegalArgumentException("Configura SEED_ADMIN_PASSWORD (8+).");
            }
            usuario = new Usuario();
            usuario.setNombreUsuario("admin");
            usuario.setContrasena(encoder.encode(adminPassword));
            usuario.setRol("ADMINISTRADOR");
            usuario = usuarios.saveAndFlush(usuario);
        }
        if (!"ADMINISTRADOR".equals(usuario.getRol())) {
            throw new IllegalStateException("La cuenta admin tiene un rol incorrecto.");
        }
        Integer registrosAdmin = jdbc.queryForObject(
                "SELECT COUNT(*) FROM administrador WHERE id_usuario=?", Integer.class,
                usuario.getIdUsuario());
        if (registrosAdmin == 0) {
            jdbc.update("INSERT INTO administrador (nombre, apellido, correo, id_usuario) " +
                    "VALUES ('Jorge', 'Pérez Castillo', 'admin@tezza.test', ?)", usuario.getIdUsuario());
        }
        Integer registrosMedico = jdbc.queryForObject(
                "SELECT COUNT(*) FROM medico WHERE id_usuario=?", Integer.class,
                usuario.getIdUsuario());
        if (registrosMedico > 0) return;

        Medico medico = new Medico();
        medico.setNombre("Jorge");
        medico.setApellido("Pérez Castillo");
        medico.setEspecialidad("Medicina General");
        medico.setCorreo("medico.demo@tezza.test");
        medico.setDni("87654321");
        medico.setEstado("ACTIVO");
        medico.setAniosExperiencia((short) 10);
        medico.setUsuario(usuario);
        medico = medicos.saveAndFlush(medico);

        for (String dia : List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")) {
            jdbc.update("INSERT INTO horario (id_medico, dia, hora_inicio, hora_fin) " +
                    "VALUES (?, ?, '08:00:00', '13:00:00')", medico.getIdMedico(), dia);
        }
        System.out.println("Médico y horarios de demostración creados.");
    }
}
