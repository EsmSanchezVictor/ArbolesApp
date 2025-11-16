package com.example.arbolesapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FullScreenGalleryActivity extends AppCompatActivity {

    private static final String EXTRA_PHOTOS = "extra_fullscreen_photos";
    private static final String EXTRA_INDEX = "extra_fullscreen_index";

    public static Intent crearIntent(Context context, ArrayList<SpeciesPhoto> fotos, int startIndex) {
        Intent intent = new Intent(context, FullScreenGalleryActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_PHOTOS, fotos);
        intent.putExtra(EXTRA_INDEX, startIndex);
        return intent;
    }

    private float startY;
    private float startX;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_gallery);

        viewPager = findViewById(R.id.viewPagerFotos);
        ImageButton btnCerrar = findViewById(R.id.btnCerrarVisor);

        ArrayList<SpeciesPhoto> fotos = getIntent().getParcelableArrayListExtra(EXTRA_PHOTOS);
        int startIndex = getIntent().getIntExtra(EXTRA_INDEX, 0);

        FullScreenPagerAdapter adapter = new FullScreenPagerAdapter(fotos != null ? fotos : new ArrayList<>());
        viewPager.setAdapter(adapter);
        if (startIndex >= 0 && startIndex < adapter.getItemCount()) {
            viewPager.setCurrentItem(startIndex, false);
        }

        btnCerrar.setOnClickListener(v -> finish());

        viewPager.setOnTouchListener((v, event) -> manejarGestos(event));
    }

    private boolean manejarGestos(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                startX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float diffY = event.getY() - startY;
                float diffX = Math.abs(event.getX() - startX);
                if (diffY > 200 && diffY > diffX) {
                    finish();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                startY = 0f;
                startX = 0f;
                break;
        }
        return false;
    }

    private static class FullScreenPagerAdapter extends RecyclerView.Adapter<FullScreenPagerAdapter.FullScreenViewHolder> {

        private final List<SpeciesPhoto> fotos;

        FullScreenPagerAdapter(List<SpeciesPhoto> fotos) {
            this.fotos = fotos;
        }

        @NonNull
        @Override
        public FullScreenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fullscreen_image, parent, false);
            return new FullScreenViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FullScreenViewHolder holder, int position) {
            holder.bind(fotos.get(position));
        }

        @Override
        public int getItemCount() {
            return fotos.size();
        }

        static class FullScreenViewHolder extends RecyclerView.ViewHolder {

            private final ImageView imageView;

            FullScreenViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageFullScreen);
            }

            void bind(SpeciesPhoto photo) {
                File file = photo.asFile();
                if (file != null && file.exists()) {
                    imageView.setImageURI((Uri) null);
                    imageView.setImageURI(Uri.fromFile(file));
                } else {
                    imageView.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
        }
    }
}