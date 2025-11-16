
package com.example.arbolesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.arbolesapp.utils.Prefs;

public class ConfiguracionesActivity extends AppCompatActivity {

    private Switch swGps;
    private EditText etPrefijo;
    private Button btnGuardar;
    private Button btnAcercaDe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuraciones);

        swGps = findViewById(R.id.swGps);
        etPrefijo = findViewById(R.id.etPrefijo);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnAcercaDe = findViewById(R.id.btnAcercaDe);

        // Load current values
        swGps.setChecked(Prefs.isUseGps(this));
        etPrefijo.setText(Prefs.getPhotoPrefix(this));

        btnGuardar.setOnClickListener(v -> {
            Prefs.setUseGps(this, swGps.isChecked());
            Prefs.setPhotoPrefix(this, etPrefijo.getText().toString().trim());
            Toast.makeText(this, getString(R.string.guardado_ok), Toast.LENGTH_SHORT).show();
            finish();
        });

        btnAcercaDe.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
    }
}
