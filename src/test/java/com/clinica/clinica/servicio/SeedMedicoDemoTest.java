package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clinica.clinica.modelo.Medico;
import com.clinica.clinica.modelo.Usuario;
import com.clinica.clinica.repositorio.MedicoRepository;
import com.clinica.clinica.repositorio.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class SeedMedicoDemoTest {
    @Test
    void separaCuentaAdministradorDelPerfilMedico() {
        var usuarios = mock(UsuarioRepository.class);
        var medicos = mock(MedicoRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        var admin = new Usuario();
        ReflectionTestUtils.setField(admin, "idUsuario", 7);
        admin.setRol("ADMINISTRADOR");
        when(usuarios.findByNombreUsuario("admin")).thenReturn(Optional.of(admin));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);
        when(medicos.findByDni("87654321")).thenReturn(Optional.empty());
        when(medicos.saveAndFlush(any(Medico.class))).thenAnswer(call -> call.getArgument(0));

        new SeedMedicoDemo(usuarios, medicos, jdbc,
                new BCryptPasswordEncoder(4), new PoliticaContrasena()).run();

        var medico = ArgumentCaptor.forClass(Medico.class);
        verify(medicos).saveAndFlush(medico.capture());
        assertNull(medico.getValue().getUsuario());
        verify(jdbc).update("UPDATE medico SET id_usuario=NULL WHERE id_usuario=?", 7);
    }
}
