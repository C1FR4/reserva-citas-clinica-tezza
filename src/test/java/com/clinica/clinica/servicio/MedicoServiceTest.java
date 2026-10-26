package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MedicoServiceTest {
    @Test
    void cifraLaClaveAntesDeGuardarCuentaDelMedico() {
        var usuarios = mock(UsuarioRepository.class);
        var medicos = mock(MedicoRepository.class);
        var encoder = new BCryptPasswordEncoder(4);
        when(usuarios.findByNombreUsuario("medico@tezza.test")).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(call -> call.getArgument(0));

        new MedicoService(usuarios, medicos, encoder, new PoliticaContrasena()).registrarMedico(
                new Medico(), "MEDICO@TEZZA.TEST", "ClaveSegura123!");

        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        verify(medicos).save(any(Medico.class));
        assertNotEquals("ClaveSegura123!", captor.getValue().getContrasena());
        assertTrue(encoder.matches("ClaveSegura123!", captor.getValue().getContrasena()));
    }
}
