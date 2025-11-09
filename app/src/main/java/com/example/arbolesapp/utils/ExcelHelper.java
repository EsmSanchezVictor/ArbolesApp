package com.example.arbolesapp.utils;
import java.io.FileInputStream;
import android.util.Log;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.DataFormatter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExcelHelper {

    public static class TreeRecord {
        public final int rowIndex;
        public final String especie;
        public final String altura;
        public final String diametroCopa;
        public final String formaCopa;

        public TreeRecord(int rowIndex, String especie, String altura, String diametroCopa, String formaCopa) {
            this.rowIndex = rowIndex;
            this.especie = especie;
            this.altura = altura;
            this.diametroCopa = diametroCopa;
            this.formaCopa = formaCopa;
        }
    }

    public static boolean crearArchivoExcel(File carpeta, String nombreProyecto) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Arboles");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Fecha", "Hora", "Especie", "Altura", "Radio Copa", "Forma Copa", "Latitud", "Longitud", "Archivo Foto"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            File archivoExcel = new File(carpeta, nombreProyecto + ".xlsx");
            if (!carpeta.exists() && !carpeta.mkdirs()) {
                Log.e("ExcelHelper", "No se pudo crear la carpeta del proyecto: " + carpeta.getAbsolutePath());
                return false;
            }
            try (FileOutputStream outputStream = new FileOutputStream(archivoExcel)) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            return true;
        } catch (IOException e) {
            Log.e("ExcelHelper", "Error al crear archivo Excel", e);
            return false;
        }
    }

    public static boolean agregarRegistro(File archivoExcel, String especie,
                                          String altura, String radioCopa, String formaCopa,
                                          double latitud, double longitud, String archivoFoto) {
        if (archivoExcel == null || !archivoExcel.exists()) {
            Log.e("ExcelHelper", "El archivo de Excel no existe: " + (archivoExcel == null ? "null" : archivoExcel.getAbsolutePath()));
            return false;
        }
        try (FileInputStream inputStream = new FileInputStream(archivoExcel);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            int lastRow = sheet.getLastRowNum();
            Row newRow = sheet.createRow(lastRow + 1);

            Date ahora = new Date();
            SimpleDateFormat fechaFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat horaFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            newRow.createCell(0).setCellValue(fechaFormat.format(ahora));
            newRow.createCell(1).setCellValue(horaFormat.format(ahora));
            newRow.createCell(2).setCellValue(especie);
            newRow.createCell(3).setCellValue(altura);
            newRow.createCell(4).setCellValue(radioCopa);
            newRow.createCell(5).setCellValue(formaCopa);
            newRow.createCell(6).setCellValue(latitud);
            newRow.createCell(7).setCellValue(longitud);
            newRow.createCell(8).setCellValue(archivoFoto);

            try (FileOutputStream outputStream = new FileOutputStream(archivoExcel)) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            return true;
        } catch (Exception e) {
            Log.e("ExcelHelper", "Error al agregar registro", e);
            return false;
        }
    }
    public static TreeRecord obtenerRegistroPorFoto(File archivoExcel, String archivoFoto) {
        if (archivoExcel == null || !archivoExcel.exists() || archivoFoto == null) {
            Log.e("ExcelHelper", "Archivo Excel inválido o nombre de foto nulo");
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        try (FileInputStream inputStream = new FileInputStream(archivoExcel);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row == null || row.getRowNum() == 0) {
                    continue;
                }
                Cell cellFoto = row.getCell(8);
                String cellValue = cellFoto == null ? "" : formatter.formatCellValue(cellFoto);
                if (archivoFoto.equalsIgnoreCase(cellValue)) {
                    String especie = getStringValue(row, 2, formatter);
                    String altura = getStringValue(row, 3, formatter);
                    String diametro = getStringValue(row, 4, formatter);
                    String formaCopa = getStringValue(row, 5, formatter);
                    return new TreeRecord(row.getRowNum(), especie, altura, diametro, formaCopa);
                }
            }
        } catch (Exception e) {
            Log.e("ExcelHelper", "Error al buscar registro por foto", e);
        }
        return null;
    }

    public static boolean actualizarRegistro(File archivoExcel, int rowIndex,
                                             String especie, String altura, String diametroCopa, String formaCopa) {
        if (archivoExcel == null || !archivoExcel.exists()) {
            Log.e("ExcelHelper", "El archivo de Excel no existe: " + (archivoExcel == null ? "null" : archivoExcel.getAbsolutePath()));
            return false;
        }
        if (rowIndex <= 0) {
            Log.e("ExcelHelper", "Índice de fila inválido para actualizar: " + rowIndex);
            return false;
        }

        try (FileInputStream inputStream = new FileInputStream(archivoExcel);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                Log.e("ExcelHelper", "No se encontró la fila a actualizar: " + rowIndex);
                return false;
            }

            row.getCell(2, MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(especie);
            row.getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(altura);
            row.getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(diametroCopa);
            row.getCell(5, MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(formaCopa);

            try (FileOutputStream outputStream = new FileOutputStream(archivoExcel)) {
                workbook.write(outputStream);
                outputStream.flush();
            }

            return true;
        } catch (Exception e) {
            Log.e("ExcelHelper", "Error al actualizar registro", e);
            return false;
        }
    }

    private static String getStringValue(Row row, int columnIndex, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }
}
