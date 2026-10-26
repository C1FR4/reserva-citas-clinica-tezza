package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class MantenimientoCitaServiceTest {
    @Test
    void rechazaCitaAjenaAntesDeCambiarla() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(7), eq(3))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> new MantenimientoCitaService(jdbc)
                .modificar(7, 3, 2, 4, LocalDate.now().plusDays(3),
                        LocalTime.of(9, 0), "PRESENCIAL", null));
    }

    @Test
    void limitaLasModificacionesAUnMaximoDeDos() {
        var jdbc = mock(JdbcTemplate.class);
        var fecha = LocalDate.now().plusDays(3);
        when(jdbc.queryForList(anyString(), eq(7), eq(3))).thenReturn(List.of(Map.of(
                "fecha", Date.valueOf(fecha), "hora", Time.valueOf("09:00:00"),
                "estado", "RESERVADA", "numero_modificaciones", 2)));

        assertThrows(IllegalArgumentException.class, () -> new MantenimientoCitaService(jdbc)
                .modificar(7, 3, 2, 4, fecha, LocalTime.of(10, 0), "VIRTUAL", null));
    }

    @Test
    void actualizaUnaReservaValidaConSuPropietarioYLimiteEnSql() {
        var jdbc = mock(JdbcTemplate.class);
        var fecha = LocalDate.now().plusDays(3);
        var hora = LocalTime.of(10, 0);
        when(jdbc.queryForList(anyString(), eq(7), eq(3))).thenReturn(List.of(Map.of(
                "fecha", Date.valueOf(fecha), "hora", Time.valueOf("09:00:00"),
                "estado", "RESERVADA", "numero_modificaciones", 0)));
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.contains("FROM horario h"),
                eq(Integer.class), eq(4), eq(2), org.mockito.ArgumentMatchers.anyString(),
                eq(hora), eq(hora))).thenReturn(1);
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.contains("FROM cita WHERE id_cita<>?"),
                eq(Integer.class), eq(7), eq(fecha), eq(hora), eq(2), eq(3))).thenReturn(0);
        when(jdbc.update(org.mockito.ArgumentMatchers.contains("numero_modificaciones=numero_modificaciones+1"),
                eq(fecha), eq(hora), eq(2), eq(4), eq("VIRTUAL"), eq("control"),
                eq(7), eq(3))).thenReturn(1);

        new MantenimientoCitaService(jdbc).modificar(7, 3, 2, 4, fecha, hora, "VIRTUAL", "control");

        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("id_paciente=? AND estado='RESERVADA'"),
                eq(fecha), eq(hora), eq(2), eq(4), eq("VIRTUAL"), eq("control"),
                eq(7), eq(3));
    }
}
