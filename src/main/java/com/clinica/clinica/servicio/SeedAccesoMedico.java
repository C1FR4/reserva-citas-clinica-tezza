package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Crea una sola cuenta de demostración y la vincula a un médico existente. */
@Component
@Profile("seed-medico")
public class SeedAccesoMedico implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final MedicoRepository medicos;
    private final BCryptPasswordEncoder encoder;
    private final PoliticaContrasena politica;

    @Value("${SEED_MEDICO_DNI:87654321}") private String dni;
    @Value("${SEED_MEDICO_PASSWORD:}") private String password;

    public SeedAccesoMedico(UsuarioRepository usuarios, MedicoRepository medicos,
                            BCryptPasswordEncoder encoder, PoliticaContrasena politica) {
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.encoder = encoder;
        this.politica = politica;
    }

    @Override
    @Transactional
    public void run(String... args) {
        var medico = medicos.findByDni(dni)
                .orElseThrow(() -> new IllegalArgumentException("No existe el médico de demostración."));
        Usuario usuario = usuarios.findByNombreUsuario("medicotest").orElse(null);
        if (usuario == null) {
            if (!politica.esValida(password)) {
                throw new IllegalArgumentException("SEED_MEDICO_PASSWORD exige 8+ caracteres, minúscula, mayúscula, dígito y símbolo.");
            }
            usuario = new Usuario();
            usuario.setNombreUsuario("medicotest");
            usuario.setContrasena(encoder.encode(password));
            usuario.setRol("MEDICO");
            usuario = usuarios.saveAndFlush(usuario);
        }
        if (!"MEDICO".equals(usuario.getRol())) {
            throw new IllegalStateException("La cuenta medicotest tiene un rol incorrecto.");
        }
        medico.setUsuario(usuario);
        medicos.save(medico);
    }
}
