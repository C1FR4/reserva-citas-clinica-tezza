
package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class MedicoService {

    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final BCryptPasswordEncoder encoder;

    public MedicoService(
            UsuarioRepository usuarioRepository,
            MedicoRepository medicoRepository,
            BCryptPasswordEncoder encoder) {

        this.usuarioRepository = usuarioRepository;
        this.medicoRepository = medicoRepository;
        this.encoder = encoder;
    }

    @Transactional
    public void registrarMedico(
            Medico medico,
            String nombreUsuario,
            String contrasena) {

        // Validar la contraseña
        if (!esContrasenaValida(contrasena)) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres, "
                            + "una minúscula y una mayúscula.");
        }

        // Validar el nombre de usuario
        if (nombreUsuario == null || nombreUsuario.isBlank() || nombreUsuario.length() > 120) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio.");
        }

        // Comprobar que el usuario no exista
        String usuarioNormalizado = nombreUsuario.trim().toLowerCase();
        if (usuarioRepository.findByNombreUsuario(usuarioNormalizado).isPresent()) {
            throw new IllegalArgumentException(
                    "Ese nombre de usuario ya está registrado.");
        }

        // Crear la cuenta del médico
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(usuarioNormalizado);
        usuario.setContrasena(encoder.encode(contrasena));
        usuario.setRol("MEDICO");

        usuarioRepository.save(usuario);

        // Asociar la cuenta al perfil médico
        medico.setUsuario(usuario);

        // Guardar el perfil médico
        medicoRepository.save(medico);
    }

    // Validar longitud, minúsculas y mayúsculas
    private boolean esContrasenaValida(String contrasena) {

        if (contrasena == null || contrasena.length() < 8) {
            return false;
        }

        boolean tieneMinuscula = false;
        boolean tieneMayuscula = false;

        for (char caracter : contrasena.toCharArray()) {

            if (Character.isLowerCase(caracter)) {
                tieneMinuscula = true;
            }

            if (Character.isUpperCase(caracter)) {
                tieneMayuscula = true;
            }
        }

        return tieneMinuscula && tieneMayuscula;
    }
}
