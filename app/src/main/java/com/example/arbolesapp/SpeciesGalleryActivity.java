package com.example.arbolesapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SpeciesGalleryActivity extends AppCompatActivity {

    private static final String EXTRA_TITLE = "extra_gallery_title";
    private static final String EXTRA_PHOTOS = "extra_gallery_photos";

    public static Intent crearIntent(Context context, ArrayList<SpeciesPhoto> fotos, String titulo) {
        Intent intent = new Intent(context, SpeciesGalleryActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_PHOTOS, fotos);
        intent.putExtra(EXTRA_TITLE, titulo);
        return intent;
    }

    private TextView textTitulo;
    private SpeciesGalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_species_gallery);

        RecyclerView recyclerView = findViewById(R.id.recyclerGallery);
        Button btnVolver = findViewById(R.id.btnVolverGaleria);
        textTitulo = findViewById(R.id.textTituloGaleria);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setHasFixedSize(true);
        adapter = new SpeciesGalleryAdapter(photo -> abrirVisor(photo));
        recyclerView.setAdapter(adapter);

        btnVolver.setOnClickListener(v -> finish());

        ArrayList<SpeciesPhoto> fotos = getIntent().getParcelableArrayListExtra(EXTRA_PHOTOS);
        String titulo = getIntent().getStringExtra(EXTRA_TITLE);
        if (titulo != null) {
            textTitulo.setText(titulo);
        }
        if (fotos != null) {
            adapter.submitList(fotos);
        }
    }

    private void abrirVisor(SpeciesPhoto photo) {
        ArrayList<SpeciesPhoto> fotos = new ArrayList<>(adapter.getItems());
        int index = fotos.indexOf(photo);
        Intent intent = FullScreenGalleryActivity.crearIntent(this, fotos, Math.max(index, 0));
        startActivity(intent);
    }

    private static class SpeciesGalleryAdapter extends RecyclerView.Adapter<SpeciesGalleryAdapter.GalleryViewHolder> {

        interface OnPhotoClickListener {
            void onPhotoClick(SpeciesPhoto photo);
        }

        private final List<SpeciesPhoto> items = new ArrayList<>();
        private final OnPhotoClickListener listener;

        SpeciesGalleryAdapter(OnPhotoClickListener listener) {
            this.listener = listener;
        }

        void submitList(List<SpeciesPhoto> fotos) {
            items.clear();
            if (fotos != null) {
                items.addAll(fotos);
            }
            notifyDataSetChanged();
        }

        List<SpeciesPhoto> getItems() {
            return items;
        }

        @NonNull
        @Override
        public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_photo, parent, false);
            return new GalleryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class GalleryViewHolder extends RecyclerView.ViewHolder {

            private final ImageView imageView;
            private final TextView textView;

            GalleryViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imagePhoto);
                textView = itemView.findViewById(R.id.textPhotoName);
            }

            void bind(SpeciesPhoto photo) {
                textView.setText(photo.species + "\n" + photo.projectName);
                File file = photo.asFile();
                if (file != null && file.exists()) {
                    imageView.setImageURI((Uri) null);
                    imageView.setImageURI(Uri.fromFile(file));
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder);
                }
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPhotoClick(photo);
                    }
                });
            }
        }
    }
}
