package com.clinica.clinica.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ReporteCitasServiceTest {
    @Test
    void exportaCitasConEncabezadosYDatos() throws Exception {
        byte[] datos = new ReporteCitasService(null).crearLibro(List.of(Map.of(
                "fecha", "25/10/2026", "hora", "10:00", "paciente", "Ana Torres",
                "dni_paciente", "12345678", "medico", "Jorge Pérez",
                "especialidad", "Medicina General", "tipo_consulta", "PRESENCIAL",
                "estado", "RESERVADA", "asistencia", "PENDIENTE")));
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(datos))) {
            var hoja = libro.getSheet("Citas");
            assertEquals("Paciente", hoja.getRow(0).getCell(2).getStringCellValue());
            assertEquals("Ana Torres", hoja.getRow(1).getCell(2).getStringCellValue());
            assertEquals("PENDIENTE", hoja.getRow(1).getCell(8).getStringCellValue());
        }
    }
}
