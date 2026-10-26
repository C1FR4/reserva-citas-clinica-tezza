package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clinica.clinica.repositorio.MedicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AgendaMedicoServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AgendaMedicoService agenda = new AgendaMedicoService(mock(MedicoRepository.class), jdbc);

    @Test
    void registraSoloCitaDeSuMedicoCuandoCumpleCondiciones() {
        when(jdbc.update(anyString(), eq("ASISTIO"), eq(12), eq(4))).thenReturn(1);
        agenda.registrarAsistencia(4, 12, "ASISTIO");
        verify(jdbc).update(anyString(), eq("ASISTIO"), eq(12), eq(4));
    }

    @Test
    void rechazaEstadoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> agenda.registrarAsistencia(4, 12, "CANCELADA"));
    }

    @Test
    void rechazaCitaAjenaFuturaOYaRegistrada() {
        when(jdbc.update(anyString(), eq("NO_ASISTIO"), eq(12), eq(4))).thenReturn(0);
        assertThrows(IllegalArgumentException.class,
                () -> agenda.registrarAsistencia(4, 12, "NO_ASISTIO"));
    }
}
