package com.example.arbolesapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GaleriaProyectoActivity extends AppCompatActivity {

    public static final String EXTRA_PROYECTO = "PROYECTO";
    public static final String EXTRA_RUTA_CARPETA = "RUTA_CARPETA";

    private RecyclerView recyclerView;
    private View emptyState;
    private PhotosAdapter adapter;

    private File projectDir;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galeria_proyecto);

        Intent intent = getIntent();
        projectName = intent.getStringExtra(EXTRA_PROYECTO);
        String rutaCarpeta = intent.getStringExtra(EXTRA_RUTA_CARPETA);

        if (projectName == null || projectName.isEmpty() || rutaCarpeta == null || rutaCarpeta.isEmpty()) {
            Toast.makeText(this, R.string.project_missing_data, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        projectDir = new File(rutaCarpeta);
        setTitle(getString(R.string.project_gallery_title, projectName));

        recyclerView = findViewById(R.id.recyclerPhotos);
        emptyState = findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setHasFixedSize(true);
        adapter = new PhotosAdapter(this::openRecordEditor);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }

    private void loadPhotos() {
        List<File> photos = new ArrayList<>();
        if (projectDir != null && projectDir.exists()) {
            File[] files = projectDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase(Locale.US);
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            });
            if (files != null) {
                Collections.addAll(photos, files);
                Collections.sort(photos, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            }
        }
        adapter.submitList(photos);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void openRecordEditor(File photo) {
        Intent intent = new Intent(this, EditarRegistroActivity.class);
        intent.putExtra(EXTRA_PROYECTO, projectName);
        intent.putExtra(EXTRA_RUTA_CARPETA, projectDir.getAbsolutePath());
        intent.putExtra(EditarRegistroActivity.EXTRA_NOMBRE_FOTO, photo.getName());
        startActivity(intent);
    }

    private static class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> {

        interface OnPhotoClickListener {
            void onPhotoClick(File photo);
        }

        private final List<File> photos = new ArrayList<>();
        private final OnPhotoClickListener listener;

        PhotosAdapter(OnPhotoClickListener listener) {
            this.listener = listener;
        }

        void submitList(List<File> newPhotos) {
            photos.clear();
            if (newPhotos != null) {
                photos.addAll(newPhotos);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_photo, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            File photo = photos.get(position);
            holder.bind(photo, listener);
        }

        @Override
        public int getItemCount() {
            return photos.size();
        }

        static class PhotoViewHolder extends RecyclerView.ViewHolder {

            private final ImageView imageView;
            private final TextView titleView;

            PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imagePhoto);
                titleView = itemView.findViewById(R.id.textPhotoName);
            }

            void bind(File photo, OnPhotoClickListener listener) {
                titleView.setText(photo.getName());
                if (photo.exists()) {
                    imageView.setImageURI(null);
                    imageView.setImageURI(Uri.fromFile(photo));
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder);
                }
                itemView.setOnClickListener(v -> listener.onPhotoClick(photo));
            }
        }
    }
}