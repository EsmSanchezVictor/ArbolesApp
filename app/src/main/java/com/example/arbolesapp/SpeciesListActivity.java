package com.example.arbolesapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpeciesListActivity extends AppCompatActivity {

    private static final String EXTRA_PHOTOS = "extra_list_photos";

    public static Intent crearIntent(Context context, ArrayList<SpeciesPhoto> fotos) {
        Intent intent = new Intent(context, SpeciesListActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_PHOTOS, fotos);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_species_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerSpecies);
        Button btnVolver = findViewById(R.id.btnVolverLista);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<SpeciesPhoto> fotos = getIntent().getParcelableArrayListExtra(EXTRA_PHOTOS);
        List<SpeciesBucket> agrupadas = agruparPorEspecie(fotos);
        if (agrupadas.isEmpty()) {
            Toast.makeText(this, R.string.species_bank_no_species, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        SpeciesListAdapter adapter = new SpeciesListAdapter(agrupadas);
        recyclerView.setAdapter(adapter);

        adapter.setOnSpeciesClickListener(bucket -> {
            String mensaje = getString(R.string.species_bank_results_placeholder) + ": " + bucket.displayName;
            startActivity(SpeciesGalleryActivity.crearIntent(this, new ArrayList<>(bucket.photos), mensaje));
        });

        btnVolver.setOnClickListener(v -> finish());
    }

    private List<SpeciesBucket> agruparPorEspecie(ArrayList<SpeciesPhoto> fotos) {
        Map<String, SpeciesBucket> mapa = new HashMap<>();
        if (fotos == null) {
            return new ArrayList<>();
        }
        for (SpeciesPhoto photo : fotos) {
            String clave = TextUtils.isEmpty(photo.species) ? getString(R.string.especie) : photo.species;
            SpeciesBucket bucket = mapa.get(clave.toLowerCase(Locale.ROOT));
            if (bucket == null) {
                bucket = new SpeciesBucket(clave);
                mapa.put(clave.toLowerCase(Locale.ROOT), bucket);
            }
            bucket.photos.add(photo);
        }
        List<SpeciesBucket> lista = new ArrayList<>(mapa.values());
        Collections.sort(lista, (o1, o2) -> o1.displayName.compareToIgnoreCase(o2.displayName));
        return lista;
    }

    private static class SpeciesListAdapter extends RecyclerView.Adapter<SpeciesListAdapter.SpeciesViewHolder> {

        interface OnSpeciesClickListener {
            void onSpeciesClick(SpeciesBucket bucket);
        }

        private final List<SpeciesBucket> especies;
        private OnSpeciesClickListener listener;

        SpeciesListAdapter(List<SpeciesBucket> especies) {
            this.especies = especies;
        }

        void setOnSpeciesClickListener(OnSpeciesClickListener listener) {
            this.listener = listener;
        }

        @NonNull
        @Override
        public SpeciesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_species_entry, parent, false);
            return new SpeciesViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SpeciesViewHolder holder, int position) {
            holder.bind(especies.get(position));
        }

        @Override
        public int getItemCount() {
            return especies.size();
        }

        class SpeciesViewHolder extends RecyclerView.ViewHolder {

            private final TextView textNombre;
            private final TextView textCantidad;

            SpeciesViewHolder(@NonNull View itemView) {
                super(itemView);
                textNombre = itemView.findViewById(R.id.textNombreEspecie);
                textCantidad = itemView.findViewById(R.id.textCantidadFotos);
            }

            void bind(SpeciesBucket bucket) {
                textNombre.setText(bucket.displayName);
                textCantidad.setText(String.format(Locale.getDefault(), "%d", bucket.photos.size()));
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSpeciesClick(bucket);
                    }
                });
            }
        }
    }

    private static class SpeciesBucket {
        final String displayName;
        final List<SpeciesPhoto> photos = new ArrayList<>();

        SpeciesBucket(String displayName) {
            this.displayName = displayName;
        }
    }
}