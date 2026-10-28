package com.clinica.clinica.servicio;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CuentaInternaServiceTest {
    UsuarioRepository usuarios;
    MedicoRepository medicos;
    JdbcTemplate jdbc;
    BCryptPasswordEncoder encoder;
    CuentaInternaService servicio;

    @BeforeEach void preparar() {
        usuarios = mock(UsuarioRepository.class); medicos = mock(MedicoRepository.class);
        jdbc = mock(JdbcTemplate.class); encoder = new BCryptPasswordEncoder(4);
        servicio = new CuentaInternaService(usuarios,medicos,jdbc,encoder,new PoliticaContrasena());
        when(usuarios.saveAndFlush(any())).thenAnswer(i -> { Usuario u=i.getArgument(0);u.setIdUsuario(90);return u; });
    }

    @Test void creaAdministradorConHashYPerfil() {
        servicio.crear("ADMINISTRADOR","AdminGrupo","Grupo2026!",null,"Admin","Grupo","admin@example.com");
        var cuenta=org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(cuenta.capture());
        assertEquals("ADMINISTRADOR",cuenta.getValue().getRol());
        assertEquals("admingrupo",cuenta.getValue().getNombreUsuario());
        assertTrue(encoder.matches("Grupo2026!",cuenta.getValue().getContrasena()));
        verify(jdbc).update(anyString(),eq("Admin"),eq("Grupo"),eq("admin@example.com"),eq(90));
    }

    @Test void vinculaCuentaMedicaAlPerfilSinCambiarCorreo() {
        var medico=new Medico();medico.setEstado("ACTIVO");medico.setCorreo("ficticio@tezza.test");
        when(jdbc.queryForList(anyString(),eq("87654324"))).thenReturn(List.of(Map.of("id_medico",4)));
        when(medicos.findByDni("87654324")).thenReturn(Optional.of(medico));
        servicio.crear("MEDICO","medicogrupo","Grupo2026!","87654324",null,null,null);
        assertEquals("MEDICO",medico.getUsuario().getRol());
        assertEquals("ficticio@tezza.test",medico.getCorreo());
        verify(medicos).saveAndFlush(medico);
    }

    @Test void noReemplazaCuentaExistente() {
        when(usuarios.findByNombreUsuario("admin")).thenReturn(Optional.of(new Usuario()));
        assertThrows(IllegalArgumentException.class,()->servicio.crear("ADMINISTRADOR","admin","Grupo2026!",null,"A","B","a@example.com"));
        verify(usuarios,never()).saveAndFlush(any());
    }

    @Test void rechazaMedicoConAccesoYaAsignado() {
        var medico=new Medico();medico.setUsuario(new Usuario());
        when(jdbc.queryForList(anyString(),eq("87654324"))).thenReturn(List.of(Map.of("id_medico",4)));
        when(medicos.findByDni("87654324")).thenReturn(Optional.of(medico));
        assertThrows(IllegalArgumentException.class,()->servicio.crear("MEDICO","medico","Grupo2026!","87654324",null,null,null));
        verify(usuarios,never()).saveAndFlush(any());
    }

    @Test void rechazaRolPacienteYClaveDebil() {
        assertThrows(IllegalArgumentException.class,()->servicio.crear("PACIENTE","paciente","Grupo2026!",null,null,null,null));
        assertThrows(IllegalArgumentException.class,()->servicio.crear("ADMINISTRADOR","admin","abc",null,"A","B","a@example.com"));
        verify(usuarios,never()).saveAndFlush(any());
    }

    @Test void noCreaAccesoSiMedicoNoExiste() {
        when(jdbc.queryForList(anyString(),anyString())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,()->servicio.crear("MEDICO","medico","Grupo2026!","87654324",null,null,null));
        verify(usuarios,never()).saveAndFlush(any());
    }
}
