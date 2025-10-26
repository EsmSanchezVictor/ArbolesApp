package com.example.arbolesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.arbolesapp.utils.ExcelHelper;
import com.example.arbolesapp.utils.FileUtils;
import java.io.File;

public class CrearProyectoActivity extends AppCompatActivity {

    private EditText etNombreProyecto;
    private Button btnAceptar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_proyecto);
        etNombreProyecto = findViewById(R.id.etNombreProyecto);
        btnAceptar = findViewById(R.id.btnAceptar);

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { crearProyecto(); }
        });
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
}
