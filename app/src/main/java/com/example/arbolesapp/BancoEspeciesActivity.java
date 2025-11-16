package com.example.arbolesapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.arbolesapp.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BancoEspeciesActivity extends AppCompatActivity {

    private EditText etBuscarEspecie;
    private TextView textResultados;

    private final List<SpeciesPhoto> allPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banco_especies);

        etBuscarEspecie = findViewById(R.id.etBuscarEspecie);
        Button btnBuscarEspecie = findViewById(R.id.btnBuscarEspecie);
        Button btnListarEspecies = findViewById(R.id.btnListarEspecies);
        Button btnExportarBanco = findViewById(R.id.btnExportarBanco);
        Button btnVolver = findViewById(R.id.btnVolverMenu);
        textResultados = findViewById(R.id.textResultados);

        btnBuscarEspecie.setOnClickListener(v -> buscarEspecie());
        btnListarEspecies.setOnClickListener(v -> mostrarListadoEspecies());
        btnExportarBanco.setOnClickListener(v -> exportarBanco());
        btnVolver.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarFotos();
    }

    private void cargarFotos() {
        textResultados.setText(R.string.species_bank_loading);

        final String especiePorDefecto = getString(R.string.especie);
        new Thread(() -> {
            LoadResult resultado = recolectarFotos(especiePorDefecto);
            runOnUiThread(() -> aplicarResultadoCarga(resultado));
        }).start();
    }

    private LoadResult recolectarFotos(String especiePorDefecto) {
        List<SpeciesPhoto> nuevasFotos = new ArrayList<>();
        File baseDir = FileUtils.obtenerCarpetaBase(getApplicationContext());
        if (baseDir == null || !baseDir.exists()) {
            return new LoadResult(nuevasFotos, R.string.species_bank_loading_error, true);
        }

        File[] proyectos = baseDir.listFiles(File::isDirectory);
        if (proyectos == null || proyectos.length == 0) {
            return new LoadResult(nuevasFotos, R.string.species_bank_no_species, false);
        }

        for (File proyecto : proyectos) {
            File[] imagenes = proyecto.listFiles((dir, name) -> {
                String lower = name.toLowerCase(Locale.ROOT);
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            });
            if (imagenes == null) {
                continue;
            }
            for (File imagen : imagenes) {
                String especie = obtenerEspecieDesdeNombre(imagen.getName());
                if (TextUtils.isEmpty(especie)) {
                    especie = especiePorDefecto;
                }
                nuevasFotos.add(new SpeciesPhoto(imagen.getAbsolutePath(), especie, proyecto.getName()));
            }
        }

        Collections.sort(nuevasFotos, (p1, p2) -> {
            int comp = p1.species.compareToIgnoreCase(p2.species);
            if (comp == 0) {
                return p1.getFileName().compareToIgnoreCase(p2.getFileName());
            }
            return comp;
        });

        if (nuevasFotos.isEmpty()) {
            return new LoadResult(nuevasFotos, R.string.species_bank_no_species, false);
        }
        return new LoadResult(nuevasFotos, R.string.species_bank_results_placeholder, false);
    }

    private void aplicarResultadoCarga(LoadResult resultado) {
        allPhotos.clear();
        allPhotos.addAll(resultado.photos);
        if (resultado.mostrarToast) {
            Toast.makeText(this, resultado.statusMessage, Toast.LENGTH_SHORT).show();
        }
        textResultados.setVisibility(View.VISIBLE);
        textResultados.setText(getString(resultado.statusMessage));
    }

    private void buscarEspecie() {
        if (allPhotos.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_no_species, Toast.LENGTH_SHORT).show();
            return;
        }
        String consulta = etBuscarEspecie.getText().toString().trim();
        if (TextUtils.isEmpty(consulta)) {
            Toast.makeText(this, R.string.species_bank_search_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String queryLower = consulta.toLowerCase(Locale.ROOT);
        List<SpeciesPhoto> coincidencias = new ArrayList<>();
        for (SpeciesPhoto entry : allPhotos) {
            if (entry.species.toLowerCase(Locale.ROOT).contains(queryLower) ||
                    entry.getFileName().toLowerCase(Locale.ROOT).contains(queryLower)) {
                coincidencias.add(entry);
            }
        }
        if (coincidencias.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_results_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String mensaje = getString(R.string.species_bank_results_placeholder) + ": " + consulta;
        startActivity(SpeciesGalleryActivity.crearIntent(this, new ArrayList<>(coincidencias), mensaje));
    }

    private void mostrarListadoEspecies() {
        if (allPhotos.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_no_species, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(SpeciesListActivity.crearIntent(this, new ArrayList<>(allPhotos)));
    }

    private void exportarBanco() {
        if (allPhotos.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_no_species, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.species_bank_export_preparing, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            File zip = null;
            try {
                zip = crearArchivoBanco();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File finalZip = zip;
            runOnUiThread(() -> {
                if (finalZip != null) {
                    Toast.makeText(this, R.string.species_bank_export_success, Toast.LENGTH_SHORT).show();
                    compartirZip(finalZip);
                } else {
                    Toast.makeText(this, R.string.species_bank_export_error, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private File crearArchivoBanco() throws IOException {
        File exportDir = new File(getCacheDir(), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            return null;
        }
        File zipFile = new File(exportDir, "banco_especies.zip");
        if (zipFile.exists() && !zipFile.delete()) {
            return null;
        }
        Map<String, List<SpeciesPhoto>> agrupadas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (SpeciesPhoto entry : allPhotos) {
            List<SpeciesPhoto> list = agrupadas.get(entry.species);
            if (list == null) {
                list = new ArrayList<>();
                agrupadas.put(entry.species, list);
            }
            list.add(entry);
        }
        final boolean[] addedEntries = {false};
        Set<String> carpetasUsadas = new HashSet<>();
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Map.Entry<String, List<SpeciesPhoto>> specieEntry : agrupadas.entrySet()) {
                String baseFolderName = sanitizarNombre(specieEntry.getKey());
                String folderName = baseFolderName;
                int index = 1;
                while (carpetasUsadas.contains(folderName)) {
                    folderName = baseFolderName + "_" + index;
                    index++;
                }
                carpetasUsadas.add(folderName);
                ZipEntry folder = new ZipEntry(folderName + "/");
                zos.putNextEntry(folder);
                zos.closeEntry();
                for (SpeciesPhoto photo : specieEntry.getValue()) {
                    File file = photo.asFile();
                    if (file == null || !file.exists()) {
                        continue;
                    }
                    String entryName = folderName + "/" + file.getName();
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry entryZip = new ZipEntry(entryName);
                        zos.putNextEntry(entryZip);
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, count);
                        }
                        zos.closeEntry();
                        addedEntries[0] = true;
                    }
                }
            }
        }
        if (!addedEntries[0]) {
            zipFile.delete();
            return null;
        }
        return zipFile;
    }

    private void compartirZip(File zipFile) {
        Uri zipUri = FileProvider.getUriForFile(this,
                "com.example.arbolesapp.fileprovider",
                zipFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : activities) {
            grantUriPermission(info.activityInfo.packageName, zipUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.species_bank_share_title)));
    }

    private String sanitizarNombre(String valor) {
        if (TextUtils.isEmpty(valor)) {
            return "Especie";
        }
        return valor.replaceAll("[\\/:*?\"<>|]", "_");
    }

    private String obtenerEspecieDesdeNombre(String nombreArchivo) {
        if (TextUtils.isEmpty(nombreArchivo)) {
            return "";
        }
        int primer = nombreArchivo.indexOf('_');
        int ultimo = nombreArchivo.lastIndexOf('_');
        int punto = nombreArchivo.lastIndexOf('.');
        if (primer == -1 || ultimo == -1 || ultimo <= primer) {
            return nombreArchivo;
        }
        if (punto != -1 && punto > ultimo) {
            return nombreArchivo.substring(primer + 1, ultimo);
        }
        return nombreArchivo.substring(primer + 1);
    }

    private static class LoadResult {
        final List<SpeciesPhoto> photos;
        final int statusMessage;
        final boolean mostrarToast;

        LoadResult(List<SpeciesPhoto> photos, int statusMessage, boolean mostrarToast) {
            this.photos = photos;
            this.statusMessage = statusMessage;
            this.mostrarToast = mostrarToast;
        }
    }
}
