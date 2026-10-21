package com.clinica.clinica.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clinica.clinica.modelo.Paciente;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.PacienteRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LoginControllerTest {
    private UsuarioRepository usuarios;
    private PacienteRepository pacientes;
    private LoginController controller;
    private Usuario usuario;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        pacientes = mock(PacienteRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        controller = new LoginController(usuarios, pacientes, encoder);
        usuario = new Usuario();
        usuario.setIdUsuario(7);
        usuario.setNombreUsuario("ana@tezza.test");
        usuario.setContrasena(encoder.encode("ClaveSegura123"));
        usuario.setRol("PACIENTE");
        paciente = new Paciente();
        paciente.setIdPaciente(4);
        paciente.setNombre("Ana");
        paciente.setApellido("Pérez");
        paciente.setUsuario(usuario);
        when(usuarios.findByNombreUsuario("ana@tezza.test")).thenReturn(Optional.of(usuario));
        when(pacientes.findByUsuario(usuario)).thenReturn(Optional.of(paciente));
        when(pacientes.findByUsuario_IdUsuario(7)).thenReturn(Optional.of(paciente));
    }

    @Test
    void loginSesionYLogout() {
        var request = new MockHttpServletRequest();
        var result = controller.iniciarSesion(
                new LoginController.Credenciales("ANA@TEZZA.TEST", "ClaveSegura123"), request);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("/PantallaPrincipal.html", ((LoginController.Acceso) result.getBody()).destino());
        assertNotNull(request.getSession(false));
        assertEquals(7, request.getSession(false).getAttribute("usuarioId"));
        assertEquals(HttpStatus.OK, controller.sesionActual(request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.cerrarSesion(request).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.sesionActual(request).getStatusCode());
    }

    @Test
    void credencialesIncorrectasNoCreanSesion() {
        var request = new MockHttpServletRequest();
        var result = controller.iniciarSesion(
                new LoginController.Credenciales("ana@tezza.test", "incorrecta"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertNull(request.getSession(false));
    }

    @Test
    void administradorAccedeASuPanel() {
        usuario.setRol("ADMINISTRADOR");
        var request = new MockHttpServletRequest();
        var result = controller.iniciarSesion(
                new LoginController.Credenciales("ana@tezza.test", "ClaveSegura123"), request);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("/admin", ((LoginController.Acceso) result.getBody()).destino());
        assertEquals("ADMINISTRADOR", request.getSession(false).getAttribute("rol"));
    }
}
