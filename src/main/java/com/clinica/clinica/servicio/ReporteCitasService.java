package com.clinica.clinica.servicio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Exporta el listado administrativo de citas a un archivo XLSX. */
@Service
public class ReporteCitasService {
    private static final List<String> CAMPOS = List.of("fecha", "hora", "paciente",
            "dni_paciente", "medico", "especialidad", "tipo_consulta", "estado", "asistencia");
    private static final List<String> TITULOS = List.of("Fecha", "Hora", "Paciente",
            "DNI", "Médico", "Especialidad", "Tipo", "Estado", "Asistencia");
    private final AdminService admin;

    public ReporteCitasService(AdminService admin) {
        this.admin = admin;
    }

    /** Construye un libro XLSX con las citas visibles para el administrador. */
    public byte[] exportar() {
        return crearLibro(admin.citas());
    }

    byte[] crearLibro(List<Map<String, Object>> citas) {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Citas");
            CellStyle encabezado = libro.createCellStyle();
            encabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var fuente = libro.createFont();
            fuente.setColor(IndexedColors.WHITE.getIndex());
            fuente.setBold(true);
            encabezado.setFont(fuente);

            var filaTitulos = hoja.createRow(0);
            for (int columna = 0; columna < TITULOS.size(); columna++) {
                var celda = filaTitulos.createCell(columna);
                celda.setCellValue(TITULOS.get(columna));
                celda.setCellStyle(encabezado);
                hoja.setColumnWidth(columna, columna == 2 || columna == 4 ? 6500 : 4300);
            }
            for (int indice = 0; indice < citas.size(); indice++) {
                var fila = hoja.createRow(indice + 1);
                for (int columna = 0; columna < CAMPOS.size(); columna++) {
                    Object valor = citas.get(indice).get(CAMPOS.get(columna));
                    fila.createCell(columna).setCellValue(valor == null ? "" : valor.toString());
                }
            }
            hoja.createFreezePane(0, 1);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, citas.size(), 0, CAMPOS.size() - 1));
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo generar el reporte de citas.", error);
        }
    }
}
