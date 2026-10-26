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
    private final PoliticaContrasena politica;

    @Value("${SEED_ADMIN_PASSWORD:}") private String adminPassword;

    public SeedMedicoDemo(UsuarioRepository usuarios, MedicoRepository medicos,
                          JdbcTemplate jdbc, BCryptPasswordEncoder encoder,
                          PoliticaContrasena politica) {
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.politica = politica;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Usuario admin = usuarios.findByNombreUsuario("admin").orElse(null);
        if (admin == null) {
            if (!politica.esValida(adminPassword)) {
                throw new IllegalArgumentException("SEED_ADMIN_PASSWORD exige 8+ caracteres, minúscula, mayúscula, dígito y símbolo.");
            }
            admin = new Usuario();
            admin.setNombreUsuario("admin");
            admin.setContrasena(encoder.encode(adminPassword));
            admin.setRol("ADMINISTRADOR");
            admin = usuarios.saveAndFlush(admin);
        }
        if (!"ADMINISTRADOR".equals(admin.getRol())) {
            throw new IllegalStateException("La cuenta admin tiene un rol incorrecto.");
        }
        Integer registrosAdmin = jdbc.queryForObject(
                "SELECT COUNT(*) FROM administrador WHERE id_usuario=?", Integer.class,
                admin.getIdUsuario());
        if (registrosAdmin == 0) {
            jdbc.update("INSERT INTO administrador (nombre, apellido, correo, id_usuario) " +
                    "VALUES ('Jorge', 'Pérez Castillo', 'admin@tezza.test', ?)", admin.getIdUsuario());
        }
        jdbc.update("UPDATE medico SET id_usuario=NULL WHERE id_usuario=?", admin.getIdUsuario());
        if (medicos.findByDni("87654321").isPresent()) return;

        Medico medico = new Medico();
        medico.setNombre("Jorge");
        medico.setApellido("Pérez Castillo");
        medico.setEspecialidad("Medicina General");
        medico.setCorreo("medico.demo@tezza.test");
        medico.setDni("87654321");
        medico.setEstado("ACTIVO");
        medico.setAniosExperiencia((short) 10);
        medico = medicos.saveAndFlush(medico);

        for (String dia : List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")) {
            jdbc.update("INSERT INTO horario (id_medico, dia, hora_inicio, hora_fin) " +
                    "VALUES (?, ?, '08:00:00', '13:00:00')", medico.getIdMedico(), dia);
        }
        System.out.println("Médico y horarios de demostración creados.");
    }
}
