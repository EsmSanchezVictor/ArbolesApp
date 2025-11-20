package com.example.arbolesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.arbolesapp.utils.ExcelHelper;

import com.example.arbolesapp.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrearProyectoActivity extends AppCompatActivity {

    private EditText etNombreProyecto;
    private Button btnAceptar;
    private LinearLayout existingProjectsContainer;
    private ListView lvExistingProjects;
    private TextView tvEmptyProjects;
    private ArrayAdapter<String> proyectosAdapter;
    private final List<File> proyectos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_proyecto);
        etNombreProyecto = findViewById(R.id.etNombreProyecto);
        btnAceptar = findViewById(R.id.btnAceptar);
        lvExistingProjects = findViewById(R.id.lvExistingProjects);
        existingProjectsContainer = findViewById(R.id.existingProjectsContainer);
        tvEmptyProjects = findViewById(R.id.tvEmptyProjects);

        proyectosAdapter = new ArrayAdapter<String>(this, R.layout.item_existing_project, R.id.tvProjectName, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.tvProjectName);
                if (textView != null) {
                    textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
                }
                return view;
            }
        };
        lvExistingProjects.setAdapter(proyectosAdapter);

        lvExistingProjects.setOnItemClickListener((parent, view, position, id) -> abrirProyecto(proyectos.get(position)));

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { crearProyecto(); }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        cargarProyectosExistentes();
    }

    private void crearProyecto() {
        String nombre = etNombreProyecto.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(this, "Ingrese un nombre para el proyecto", Toast.LENGTH_SHORT).show();
            return;
        }

        File carpeta = FileUtils.crearCarpetaProyecto(this, nombre);
        if (carpeta == null) {
            Toast.makeText(this, "No se pudo crear la carpeta del proyecto", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ExcelHelper.crearArchivoExcel(carpeta, nombre)) {
            Toast.makeText(this, "No se pudo crear el Excel", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, CapturaArbolActivity.class);
        i.putExtra("PROYECTO", nombre);
        i.putExtra("RUTA_CARPETA", carpeta.getAbsolutePath());
        startActivity(i);
        finish();
    }
    private void cargarProyectosExistentes() {
        proyectos.clear();
        List<String> nombres = new ArrayList<>();

        File baseDir = FileUtils.obtenerCarpetaBase(getApplicationContext());
        if (baseDir != null) {
            File[] directories = baseDir.listFiles();
            if (directories != null) {
                for (File directory : directories) {
                    if (directory.isDirectory()) {
                        proyectos.add(directory);
                    }
                }
                Collections.sort(proyectos, (file1, file2) -> file1.getName().compareToIgnoreCase(file2.getName()));
            }
        }

        for (File proyecto : proyectos) {
            nombres.add(proyecto.getName());
        }

        proyectosAdapter.clear();
        proyectosAdapter.addAll(nombres);
        proyectosAdapter.notifyDataSetChanged();

        boolean hasProjects = !proyectos.isEmpty();
        existingProjectsContainer.setVisibility(hasProjects ? View.VISIBLE : View.GONE);
        tvEmptyProjects.setVisibility(hasProjects ? View.GONE : View.VISIBLE);
    }

    private void abrirProyecto(File proyectoDir) {
        if (proyectoDir == null || !proyectoDir.exists()) {
            Toast.makeText(this, R.string.projects_storage_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, CapturaArbolActivity.class);
        i.putExtra("PROYECTO", proyectoDir.getName());
        i.putExtra("RUTA_CARPETA", proyectoDir.getAbsolutePath());
        startActivity(i);
        finish();
    }
}
