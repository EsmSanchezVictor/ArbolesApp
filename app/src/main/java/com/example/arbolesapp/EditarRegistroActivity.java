package com.example.arbolesapp;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arbolesapp.utils.ExcelHelper;

import java.io.File;

public class EditarRegistroActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE_FOTO = "NOMBRE_FOTO";

    private ImageView imagePhoto;
    private EditText etEspecie;
    private EditText etAltura;
    private EditText etDiametroCota;
    private Spinner spFormaCopa;
    private Button btnAceptar;
    private Button btnCancelar;

    private ExcelHelper.TreeRecord treeRecord;
    private File excelFile;
    private String projectName;
    private String photoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_registro);

        imagePhoto = findViewById(R.id.imagePhotoPreview);
        etEspecie = findViewById(R.id.etEspecieEdit);
        etAltura = findViewById(R.id.etAlturaEdit);
        etDiametroCota = findViewById(R.id.etDiametroCotaEdit);
        spFormaCopa = findViewById(R.id.spFormaCopaEdit);
        btnAceptar = findViewById(R.id.btnAceptarEdicion);
        btnCancelar = findViewById(R.id.btnCancelarEdicion);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.formas_copa, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFormaCopa.setAdapter(adapter);

        projectName = getIntent().getStringExtra(GaleriaProyectoActivity.EXTRA_PROYECTO);
        String rutaCarpeta = getIntent().getStringExtra(GaleriaProyectoActivity.EXTRA_RUTA_CARPETA);
        photoName = getIntent().getStringExtra(EXTRA_NOMBRE_FOTO);

        if (TextUtils.isEmpty(projectName) || TextUtils.isEmpty(rutaCarpeta) || TextUtils.isEmpty(photoName)) {
            Toast.makeText(this, R.string.project_missing_data, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(getString(R.string.record_edit_title, photoName));

        File projectDir = new File(rutaCarpeta);
        excelFile = new File(projectDir, projectName + ".xlsx");

        if (!excelFile.exists()) {
            Toast.makeText(this, R.string.record_edit_excel_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadRecord(projectDir);

        btnAceptar.setOnClickListener(v -> saveChanges());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void loadRecord(File projectDir) {
        treeRecord = ExcelHelper.obtenerRegistroPorFoto(excelFile, photoName);
        if (treeRecord == null) {
            Toast.makeText(this, R.string.record_edit_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        File photoFile = new File(projectDir, photoName);
        if (photoFile.exists()) {
            imagePhoto.setImageURI(Uri.fromFile(photoFile));
        } else {
            imagePhoto.setImageDrawable(null);
        }

        etEspecie.setText(treeRecord.especie);
        etAltura.setText(treeRecord.altura);
        etDiametroCota.setText(treeRecord.diametroCopa);

        String[] formas = getResources().getStringArray(R.array.formas_copa);
        int selection = 0;
        for (int i = 0; i < formas.length; i++) {
            if (formas[i].equalsIgnoreCase(treeRecord.formaCopa)) {
                selection = i;
                break;
            }
        }
        spFormaCopa.setSelection(selection);
    }

    private void saveChanges() {
        String especie = etEspecie.getText().toString().trim();
        String altura = etAltura.getText().toString().trim();
        String diametro = etDiametroCota.getText().toString().trim();
        String formaCopa = spFormaCopa.getSelectedItem() != null ? spFormaCopa.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(especie) || TextUtils.isEmpty(altura) || TextUtils.isEmpty(diametro) || TextUtils.isEmpty(formaCopa) || treeRecord == null) {
            Toast.makeText(this, R.string.record_edit_validation_error, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean updated = ExcelHelper.actualizarRegistro(excelFile, treeRecord.rowIndex, especie, altura, diametro, formaCopa);
        if (updated) {
            Toast.makeText(this, R.string.record_edit_success, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.record_edit_error, Toast.LENGTH_LONG).show();
        }
    }
}
