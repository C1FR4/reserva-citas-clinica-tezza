package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clinica.clinica.repositorio.MedicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminServiceEliminacionTest {
    @Test
    void soloEliminaFisicamenteUnaCitaCancelada() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("estado='CANCELADA'"), eq(13))).thenReturn(1);

        new AdminService(mock(MedicoRepository.class), jdbc).eliminarCitaCancelada(13);

        verify(jdbc).update(contains("DELETE FROM cita WHERE id_cita=? AND estado='CANCELADA'"), eq(13));
    }

    @Test
    void rechazaCuandoLaCitaSigueActiva() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(contains("estado='CANCELADA'"), eq(13))).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> new AdminService(mock(MedicoRepository.class), jdbc).eliminarCitaCancelada(13));
    }
}
