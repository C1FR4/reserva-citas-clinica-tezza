package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaInternaService {
    private final UsuarioRepository usuarios;
    private final MedicoRepository medicos;
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder;
    private final PoliticaContrasena politica;

    public CuentaInternaService(UsuarioRepository usuarios, MedicoRepository medicos,
                               JdbcTemplate jdbc, BCryptPasswordEncoder encoder, PoliticaContrasena politica) {
        this.usuarios = usuarios;
        this.medicos = medicos;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.politica = politica;
    }

    @Transactional
    public void crear(String rol, String nombreUsuario, String clave, String dni,
                      String nombre, String apellido, String correo) {
        if (!Set.of("ADMINISTRADOR", "MEDICO").contains(rol == null ? "" : rol)) {
            throw new IllegalArgumentException("La herramienta interna solo crea ADMINISTRADOR o MEDICO.");
        }
        if (nombreUsuario == null || !nombreUsuario.trim().matches("[a-zA-Z0-9._@+\\-]{3,120}")) {
            throw new IllegalArgumentException("Usa un identificador de 3 a 120 caracteres sin espacios.");
        }
        if (!politica.esValida(clave) || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La clave exige 8 caracteres, minúscula, mayúscula, dígito y símbolo, sin espacios (máximo 72 bytes).");
        }
        String identificador = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByNombreUsuario(identificador).isPresent()) {
            throw new IllegalArgumentException("La cuenta ya existe; esta herramienta no modifica cuentas existentes.");
        }
        if ("MEDICO".equals(rol)) {
            if (dni == null || !dni.matches("[0-9]{8}")) throw new IllegalArgumentException("Indica el DNI de 8 dígitos del perfil médico existente.");
            var perfiles = jdbc.queryForList("SELECT id_medico FROM medico WHERE dni=? FOR UPDATE", dni);
            if (perfiles.isEmpty()) throw new IllegalArgumentException("Primero registra al médico en el panel administrativo.");
            var medico = medicos.findByDni(dni).orElseThrow();
            if (medico.getUsuario() != null) throw new IllegalArgumentException("Ese médico ya tiene una cuenta; no se reemplazó.");
            if (!"ACTIVO".equals(medico.getEstado())) throw new IllegalArgumentException("Activa el perfil médico antes de crear su acceso.");
            medico.setUsuario(guardar(identificador, clave, rol));
            medicos.saveAndFlush(medico);
        } else {
            if (nombre == null || nombre.isBlank() || nombre.trim().length() > 80
                    || apellido == null || apellido.isBlank() || apellido.trim().length() > 100
                    || correo == null || correo.length() > 120 || !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                throw new IllegalArgumentException("Revisa nombre, apellido y correo de contacto del administrador.");
            }
            var usuario = guardar(identificador, clave, rol);
            jdbc.update("INSERT INTO administrador(nombre,apellido,correo,id_usuario) VALUES(?,?,?,?)",
                    nombre.trim(), apellido.trim(), correo.trim().toLowerCase(Locale.ROOT), usuario.getIdUsuario());
        }
    }

    private Usuario guardar(String identificador, String clave, String rol) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(identificador);
        usuario.setContrasena(encoder.encode(clave));
        usuario.setRol(rol);
        return usuarios.saveAndFlush(usuario);
    }
}
