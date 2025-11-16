package com.example.arbolesapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arbolesapp.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BancoEspeciesActivity extends AppCompatActivity {

    private EditText etBuscarEspecie;
    private RecyclerView recyclerResultados;
    private TextView textResultados;

    private final List<PhotoEntry> allPhotos = new ArrayList<>();
    private SpeciesPhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banco_especies);

        etBuscarEspecie = findViewById(R.id.etBuscarEspecie);
        Button btnBuscarEspecie = findViewById(R.id.btnBuscarEspecie);
        Button btnListarEspecies = findViewById(R.id.btnListarEspecies);
        Button btnExportarBanco = findViewById(R.id.btnExportarBanco);
        Button btnVolver = findViewById(R.id.btnVolverMenu);
        recyclerResultados = findViewById(R.id.recyclerResultados);
        textResultados = findViewById(R.id.textResultados);

        recyclerResultados.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerResultados.setHasFixedSize(true);
        recyclerResultados.setNestedScrollingEnabled(false);
        adapter = new SpeciesPhotoAdapter();
        recyclerResultados.setAdapter(adapter);

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
        allPhotos.clear();
        File baseDir = FileUtils.obtenerCarpetaBase(getApplicationContext());
        if (baseDir == null || !baseDir.exists()) {
            Toast.makeText(this, R.string.species_bank_loading_error, Toast.LENGTH_SHORT).show();
            mostrarResultados(Collections.emptyList(), getString(R.string.species_bank_loading_error));
            return;
        }

        File[] proyectos = baseDir.listFiles(File::isDirectory);
        if (proyectos == null) {
            mostrarResultados(Collections.emptyList(), getString(R.string.species_bank_no_species));
            return;
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
                    especie = getString(R.string.especie);
                }
                allPhotos.add(new PhotoEntry(imagen, especie, proyecto.getName()));
            }
        }
        Collections.sort(allPhotos, (p1, p2) -> {
            int comp = p1.species.compareToIgnoreCase(p2.species);
            if (comp == 0) {
                return p1.file.getName().compareToIgnoreCase(p2.file.getName());
            }
            return comp;
        });

        if (allPhotos.isEmpty()) {
            mostrarResultados(Collections.emptyList(), getString(R.string.species_bank_no_species));
        } else {
            mostrarResultados(Collections.emptyList(), getString(R.string.species_bank_results_placeholder));
        }
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
        List<PhotoEntry> coincidencias = new ArrayList<>();
        for (PhotoEntry entry : allPhotos) {
            if (entry.species.toLowerCase(Locale.ROOT).contains(queryLower) ||
                    entry.file.getName().toLowerCase(Locale.ROOT).contains(queryLower)) {
                coincidencias.add(entry);
            }
        }
        if (coincidencias.isEmpty()) {
            mostrarResultados(Collections.emptyList(), getString(R.string.species_bank_results_empty));
        } else {
            String mensaje = getString(R.string.species_bank_results_placeholder) + ": " + consulta;
            mostrarResultados(coincidencias, mensaje);
        }
    }

    private void mostrarListadoEspecies() {
        if (allPhotos.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_no_species, Toast.LENGTH_SHORT).show();
            return;
        }
        LinkedHashSet<String> especiesUnicas = new LinkedHashSet<>();
        for (PhotoEntry entry : allPhotos) {
            if (!TextUtils.isEmpty(entry.species)) {
                especiesUnicas.add(entry.species);
            }
        }
        if (especiesUnicas.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_no_species, Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> especiesOrdenadas = new ArrayList<>(especiesUnicas);
        Collections.sort(especiesOrdenadas, String::compareToIgnoreCase);
        CharSequence[] items = especiesOrdenadas.toArray(new CharSequence[0]);

        new AlertDialog.Builder(this)
                .setTitle(R.string.species_bank_list_button)
                .setItems(items, (dialog, which) -> mostrarFotosDeEspecie(especiesOrdenadas.get(which)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void mostrarFotosDeEspecie(String especie) {
        List<PhotoEntry> seleccionadas = new ArrayList<>();
        for (PhotoEntry entry : allPhotos) {
            if (entry.species.equalsIgnoreCase(especie)) {
                seleccionadas.add(entry);
            }
        }
        if (seleccionadas.isEmpty()) {
            mostrarResultados(Collections.emptyList(), getString(R.string.species_bank_results_empty));
        } else {
            String mensaje = getString(R.string.species_bank_results_placeholder) + ": " + especie;
            mostrarResultados(seleccionadas, mensaje);
        }
    }

    private void mostrarResultados(List<PhotoEntry> resultados, String mensaje) {
        adapter.submitList(resultados);
        if (resultados == null || resultados.isEmpty()) {
            recyclerResultados.setVisibility(View.GONE);
            textResultados.setVisibility(View.VISIBLE);
            textResultados.setText(TextUtils.isEmpty(mensaje) ? getString(R.string.species_bank_results_empty) : mensaje);
        } else {
            recyclerResultados.setVisibility(View.VISIBLE);
            textResultados.setVisibility(View.VISIBLE);
            textResultados.setText(TextUtils.isEmpty(mensaje) ? getString(R.string.species_bank_results_placeholder) : mensaje);
        }
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
        Map<String, List<PhotoEntry>> agrupadas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (PhotoEntry entry : allPhotos) {
            List<PhotoEntry> list = agrupadas.get(entry.species);
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
            for (Map.Entry<String, List<PhotoEntry>> specieEntry : agrupadas.entrySet()) {
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
                for (PhotoEntry photo : specieEntry.getValue()) {
                    if (photo.file == null || !photo.file.exists()) {
                        continue;
                    }
                    String entryName = folderName + "/" + photo.file.getName();
                    try (FileInputStream fis = new FileInputStream(photo.file)) {
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

    private static class PhotoEntry {
        final File file;
        final String species;
        final String projectName;

        PhotoEntry(File file, String species, String projectName) {
            this.file = file;
            this.species = species;
            this.projectName = projectName;
        }
    }

    private static class SpeciesPhotoAdapter extends RecyclerView.Adapter<SpeciesPhotoAdapter.SpeciesPhotoViewHolder> {

        private final List<PhotoEntry> items = new ArrayList<>();

        void submitList(List<PhotoEntry> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @Override
        public SpeciesPhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_photo, parent, false);
            return new SpeciesPhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SpeciesPhotoViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SpeciesPhotoViewHolder extends RecyclerView.ViewHolder {

            private final ImageView imageView;
            private final TextView textView;

            SpeciesPhotoViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imagePhoto);
                textView = itemView.findViewById(R.id.textPhotoName);
            }

            void bind(PhotoEntry entry) {
                textView.setText(entry.species + "\n" + entry.projectName);
                if (entry.file != null && entry.file.exists()) {
                    imageView.setImageURI(null);
                    imageView.setImageURI(Uri.fromFile(entry.file));
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
        }
    }
}