package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.util.List;
import java.util.UUID;
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
        if (usuarios.findByNombreUsuario("medico.demo@tezza.test").isPresent()) return;
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario("medico.demo@tezza.test");
        usuario.setContrasena(encoder.encode(UUID.randomUUID().toString()));
        usuario.setRol("MEDICO");
        usuario = usuarios.saveAndFlush(usuario);

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
