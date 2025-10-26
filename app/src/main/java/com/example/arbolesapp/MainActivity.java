package com.example.arbolesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnNuevoProyecto, btnEditarProyecto, btnBancoEspecies, btnConfiguraciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnNuevoProyecto = findViewById(R.id.btnNuevoProyecto);
        btnEditarProyecto = findViewById(R.id.btnEditarProyecto);
        btnBancoEspecies = findViewById(R.id.btnBancoEspecies);
        btnConfiguraciones = findViewById(R.id.btnConfiguraciones);

        btnNuevoProyecto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CrearProyectoActivity.class));
            }
        });

        btnEditarProyecto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditarProyectoActivity.class));
            }
        });

        btnBancoEspecies.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BancoEspeciesActivity.class));
            }
        });

        btnConfiguraciones.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ConfiguracionesActivity.class));
            }
        });
    }
}
