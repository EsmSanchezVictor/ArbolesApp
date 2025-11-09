package com.example.arbolesapp;
import android.text.TextUtils;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.arbolesapp.utils.ExcelHelper;
import com.example.arbolesapp.utils.GPSUtils;
import java.io.File;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import android.Manifest;
import com.example.arbolesapp.utils.Prefs;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CapturaArbolActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private String proyecto, rutaCarpeta;
    private String rutaFotoActual;
    private Location ubicacionActual;

    private ImageView imgPreview;
    private EditText etEspecie, etAltura, etRadioCopa;
    private Spinner spFormaCopa;
    private Button btnTomarFoto, btnMarcarGPS, btnAceptar, btnNuevaCaptura, btnTerminar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captura_arbol);

        proyecto = getIntent().getStringExtra("PROYECTO");
        rutaCarpeta = getIntent().getStringExtra("RUTA_CARPETA");

        if (TextUtils.isEmpty(proyecto) || TextUtils.isEmpty(rutaCarpeta)) {
            Toast.makeText(this, getString(R.string.project_missing_data), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        imgPreview = findViewById(R.id.imgPreview);
        etEspecie = findViewById(R.id.etEspecie);
        etAltura = findViewById(R.id.etAltura);
        etRadioCopa = findViewById(R.id.etRadioCopa);
        spFormaCopa = findViewById(R.id.spFormaCopa);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnMarcarGPS = findViewById(R.id.btnMarcarGPS);
        btnAceptar = findViewById(R.id.btnAceptar);
        btnNuevaCaptura = findViewById(R.id.btnNuevaCaptura);
        btnTerminar = findViewById(R.id.btnTerminar);

        btnAceptar.setVisibility(View.GONE);
        btnNuevaCaptura.setVisibility(View.GONE);
        btnTerminar.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.formas_copa, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFormaCopa.setAdapter(adapter);

        btnTomarFoto.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tomarFoto(); }
        });

        btnMarcarGPS.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { marcarPosicionGPS(); }
        });

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { guardarDatos(); }
        });

        btnNuevaCaptura.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { nuevaCaptura(); }
        });

        btnTerminar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
    }

    private void tomarFoto() {
        // Solicitar permisos de cámara y almacenamiento (si aplica)
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            lanzarCamara();
                        } else {
                            Toast.makeText(CapturaArbolActivity.this, getString(R.string.perm_cam_msg), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(java.util.List<PermissionRequest> list, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void lanzarCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = crearArchivoFoto();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.arbolesapp.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "No se pudo crear el archivo de foto", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay app de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFoto() {
        if (TextUtils.isEmpty(rutaCarpeta)) {
            Toast.makeText(this, getString(R.string.project_capture_storage_error), Toast.LENGTH_SHORT).show();
            return null;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "ARBOL_" + timeStamp + "_";
        File storageDir = new File(rutaCarpeta);
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Toast.makeText(this, getString(R.string.project_capture_storage_error), Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            rutaFotoActual = image.getAbsolutePath();
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void marcarPosicionGPS() {
        if (!com.example.arbolesapp.utils.Prefs.isUseGps(this)) {
            ubicacionActual = null;
            Toast.makeText(this, "GPS desactivado en Configuraciones", Toast.LENGTH_SHORT).show();
            return;
        }
        Dexter.withContext(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            obtenerYMostrarGPS();
                        } else {
                            Toast.makeText(CapturaArbolActivity.this, getString(R.string.perm_loc_msg), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(java.util.List<PermissionRequest> list, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void obtenerYMostrarGPS() {
        ubicacionActual = GPSUtils.obtenerUbicacion(this);
        if (ubicacionActual != null) {
            Toast.makeText(this,
                    "Posición: " + ubicacionActual.getLatitude() + ", " + ubicacionActual.getLongitude(),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarDatos() {
        String especie = etEspecie.getText().toString().trim();
        String altura = etAltura.getText().toString().trim();
        String radioCopa = etRadioCopa.getText().toString().trim();
        String formaCopa = spFormaCopa.getSelectedItem().toString();

        boolean gpsRequerido = com.example.arbolesapp.utils.Prefs.isUseGps(this);
        if (especie.isEmpty() || altura.isEmpty() || radioCopa.isEmpty() || rutaFotoActual == null || (gpsRequerido && ubicacionActual == null)) {
            Toast.makeText(this, "Complete todos los campos y capture foto y GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        String nuevoNombre = com.example.arbolesapp.utils.Prefs.getPhotoPrefix(this) + "_" + especie + "_" + System.currentTimeMillis() + ".jpg";
        File fotoOriginal = new File(rutaFotoActual);
        File fotoRenombrada = new File(fotoOriginal.getParent(), nuevoNombre);
        boolean renamed = fotoOriginal.renameTo(fotoRenombrada);
        if (!renamed) {
            Toast.makeText(this, getString(R.string.project_photo_rename_error), Toast.LENGTH_SHORT).show();
        }

        File fotoFinal = renamed ? fotoRenombrada : fotoOriginal;
        rutaFotoActual = fotoFinal.getAbsolutePath();
        boolean ok = com.example.arbolesapp.utils.ExcelHelper.agregarRegistro(
                new File(rutaCarpeta, proyecto + ".xlsx"),
                especie, altura, radioCopa, formaCopa,
                (ubicacionActual==null?0.0:ubicacionActual.getLatitude()), (ubicacionActual==null?0.0:ubicacionActual.getLongitude()),
                fotoFinal.getName()
        );

        if (ok) {
            Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
            mostrarOpcionesPostCaptura();
        } else {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }

    private void nuevaCaptura() {
        etEspecie.setText("");
        etAltura.setText("");
        etRadioCopa.setText("");
        spFormaCopa.setSelection(0);
        imgPreview.setImageURI(null);
        rutaFotoActual = null;
        ubicacionActual = null;

        btnTomarFoto.setVisibility(View.VISIBLE);
        btnMarcarGPS.setVisibility(View.VISIBLE);
        btnAceptar.setVisibility(View.GONE);
        btnNuevaCaptura.setVisibility(View.GONE);
        btnTerminar.setVisibility(View.GONE);
    }

    private void mostrarOpcionesPostCaptura() {
        btnTomarFoto.setVisibility(View.GONE);
        btnMarcarGPS.setVisibility(View.GONE);
        btnAceptar.setVisibility(View.GONE);
        btnNuevaCaptura.setVisibility(View.VISIBLE);
        btnTerminar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imgPreview.setImageURI(Uri.fromFile(new File(rutaFotoActual)));
            btnAceptar.setVisibility(View.VISIBLE);
        }
    }
}
