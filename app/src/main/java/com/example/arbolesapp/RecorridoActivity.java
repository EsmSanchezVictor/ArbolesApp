package com.example.arbolesapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.example.arbolesapp.ui.RouteMapView;
import com.example.arbolesapp.utils.ExcelHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RecorridoActivity extends AppCompatActivity {

    public static final String EXTRA_PROYECTO = "PROYECTO";
    public static final String EXTRA_RUTA_CARPETA = "RUTA_CARPETA";

    private RouteMapView routeMapView;
    private TextView emptyState;
    private Button exportButton;
    private ProgressBar progressBar;

    private String proyecto;
    private String rutaCarpeta;
    private List<ExcelHelper.TreePoint> puntos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorrido);

        routeMapView = findViewById(R.id.routeMapView);
        emptyState = findViewById(R.id.tvEmptyState);
        exportButton = findViewById(R.id.btnExportarKml);
        progressBar = findViewById(R.id.progressExport);

        proyecto = getIntent().getStringExtra(EXTRA_PROYECTO);
        rutaCarpeta = getIntent().getStringExtra(EXTRA_RUTA_CARPETA);

        if (proyecto == null || rutaCarpeta == null) {
            Toast.makeText(this, R.string.project_missing_data, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        cargarPuntos();

        exportButton.setOnClickListener(v -> mostrarOpcionesExportacion());
    }

    private void cargarPuntos() {
        File excel = new File(rutaCarpeta, proyecto + ".xlsx");
        List<ExcelHelper.TreePoint> registros = ExcelHelper.leerRegistros(excel);
        puntos.clear();
        for (ExcelHelper.TreePoint punto : registros) {
            if (punto.latitud != 0.0 || punto.longitud != 0.0) {
                puntos.add(punto);
            }
        }

        if (puntos.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            routeMapView.setVisibility(View.GONE);
            exportButton.setEnabled(false);
        } else {
            emptyState.setVisibility(View.GONE);
            routeMapView.setVisibility(View.VISIBLE);
            routeMapView.setPoints(puntos);
            exportButton.setEnabled(true);
        }
    }

    private void mostrarOpcionesExportacion() {
        if (puntos.isEmpty()) {
            Toast.makeText(this, R.string.route_no_points, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.route_export)
                .setMessage(R.string.route_export_choose_images)
                .setPositiveButton(R.string.route_export_with_images, (dialog, which) -> exportarKmz(true))
                .setNegativeButton(R.string.route_export_without_images, (dialog, which) -> exportarKmz(false))
                .setNeutralButton(R.string.cancelar, null)
                .show();
    }

    private void exportarKmz(boolean includeImages) {
        if (puntos.isEmpty()) {
            Toast.makeText(this, R.string.route_no_points, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        exportButton.setEnabled(false);

        new Thread(() -> {
            File kmz = crearKmz(includeImages);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                exportButton.setEnabled(true);

                if (kmz != null && kmz.exists()) {
                    shareKmz(kmz);
                } else {
                    Toast.makeText(RecorridoActivity.this, R.string.route_export_error, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private File crearKmz(boolean includeImages) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File kmzFile = new File(rutaCarpeta, proyecto + "_recorrido_" + timestamp + ".kmz");

            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(kmzFile))) {
                zipOut.putNextEntry(new ZipEntry("doc.kml"));
                String kml = buildKml(includeImages);
                zipOut.write(kml.getBytes(StandardCharsets.UTF_8));
                zipOut.closeEntry();

                Set<String> addedImages = new HashSet<>();
                if (includeImages) {
                    for (ExcelHelper.TreePoint punto : puntos) {
                        if (punto.archivoFoto == null || punto.archivoFoto.trim().isEmpty()) {
                            continue;
                        }
                        if (addedImages.contains(punto.archivoFoto)) {
                            continue;
                        }
                        File foto = new File(rutaCarpeta, punto.archivoFoto);
                        if (!foto.exists()) {
                            continue;
                        }
                        addedImages.add(punto.archivoFoto);
                        zipOut.putNextEntry(new ZipEntry("images/" + punto.archivoFoto));
                        try (FileInputStream fis = new FileInputStream(foto)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, len);
                            }
                        }
                        zipOut.closeEntry();
                        }


                }
            }

            return kmzFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void shareKmz(File kmzFile) {
        Uri uri = FileProvider.getUriForFile(this, "com.example.arbolesapp.fileprovider", kmzFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/vnd.google-earth.kmz");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.route_share_title)));
    }

    private String buildKml(boolean includeImages) throws IOException {
        Map<String, String> speciesStyleMap = new HashMap<>();
        StringBuilder styles = new StringBuilder();

        for (ExcelHelper.TreePoint punto : puntos) {
            String especie = punto.especie == null || punto.especie.isEmpty() ? "especie" : punto.especie;
            if (speciesStyleMap.containsKey(especie)) {
                continue;
            }
            String styleId = "style_" + speciesStyleMap.size();
            speciesStyleMap.put(especie, styleId);
            String colorKml = colorToKmlHex(getColorForSpecies(especie));
            styles.append("    <Style id=\"").append(styleId).append("\">\n")
                    .append("      <IconStyle>\n")
                    .append("        <color>").append(colorKml).append("</color>\n")
                    .append("        <scale>1.2</scale>\n")
                    .append("        <Icon>\n")
                    .append("          <href>http://maps.google.com/mapfiles/kml/paddle/wht-blank.png</href>\n")
                    .append("        </Icon>\n")
                    .append("      </IconStyle>\n")
                    .append("    </Style>\n");
        }

        StringBuilder placemarks = new StringBuilder();
        for (ExcelHelper.TreePoint punto : puntos) {
            String especie = punto.especie == null || punto.especie.isEmpty() ? "Especie" : punto.especie;
            String styleId = speciesStyleMap.get(especie);
            String descripcion = buildDescription(punto, includeImages);
            placemarks.append("    <Placemark>\n")
                    .append("      <name>").append(escape(especie)).append("</name>\n")
                    .append(styleId == null ? "" : "      <styleUrl>#" + styleId + "</styleUrl>\n")
                    .append("      <description><![CDATA[").append(descripcion).append("]]></description>\n")
                    .append("      <Point><coordinates>")
                    .append(punto.longitud).append(",").append(punto.latitud).append(",0</coordinates></Point>\n")
                    .append("    </Placemark>\n");
        }

        StringBuilder kml = new StringBuilder();
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
                .append("  <Document>\n")
                .append("    <name>").append(escape(proyecto)).append(" - Recorrido</name>\n")
                .append(styles)
                .append(placemarks)
                .append("  </Document>\n")
                .append("</kml>");
        return kml.toString();
    }

    private String buildDescription(ExcelHelper.TreePoint punto, boolean includeImages) {
        StringBuilder desc = new StringBuilder();
        desc.append("<p><b>Especie:</b> ").append(escape(punto.especie)).append("</p>")
                .append("<p><b>Altura:</b> ").append(escape(punto.altura)).append("</p>")
                .append("<p><b>Radio de copa:</b> ").append(escape(punto.radioCopa)).append("</p>")
                .append("<p><b>Forma:</b> ").append(escape(punto.formaCopa)).append("</p>");
        if (includeImages && punto.archivoFoto != null && !punto.archivoFoto.trim().isEmpty()) {
            desc.append("<p><b>Imagen:</b><br/><img src=\"images/")
                    .append(escape(punto.archivoFoto)).append("\" width=\"400\"/></p>");
        }
        return desc.toString();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private int getColorForSpecies(String species) {
        String key = species == null || species.trim().isEmpty() ? "_" : species.trim();
        float[] hsv = new float[]{Math.abs(key.hashCode()) % 360, 0.7f, 0.95f};
        return android.graphics.Color.HSVToColor(240, hsv);
    }

    private String colorToKmlHex(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return String.format(Locale.US, "%02x%02x%02x%02x", a, b, g, r);
    }
}