package com.example.arbolesapp.utils;

import android.util.Log;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExcelHelper {

    public static boolean crearArchivoExcel(File carpeta, String nombreProyecto) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Arboles");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Fecha", "Hora", "Especie", "Altura", "Radio Copa", "Forma Copa", "Latitud", "Longitud", "Archivo Foto"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            File archivoExcel = new File(carpeta, nombreProyecto + ".xlsx");
            FileOutputStream outputStream = new FileOutputStream(archivoExcel);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            return true;
        } catch (IOException e) {
            Log.e("ExcelHelper", "Error al crear archivo Excel", e);
            return false;
        }
    }

    public static boolean agregarRegistro(File archivoExcel, String especie,
                                          String altura, String radioCopa, String formaCopa,
                                          double latitud, double longitud, String archivoFoto) {
        try {
            Workbook workbook = WorkbookFactory.create(archivoExcel);
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

            FileOutputStream outputStream = new FileOutputStream(archivoExcel);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            return true;
        } catch (Exception e) {
            Log.e("ExcelHelper", "Error al agregar registro", e);
            return false;
        }
    }
}
