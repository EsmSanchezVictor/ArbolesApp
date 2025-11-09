package com.example.arbolesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arbolesapp.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditarProyectoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyState;
    private ProjectsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_proyecto);

        recyclerView = findViewById(R.id.recyclerProjects);
        emptyState = findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectsAdapter(this::onProjectClick);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void loadProjects() {
        File baseDir = FileUtils.obtenerCarpetaBase(getApplicationContext());
        List<File> proyectos = new ArrayList<>();
        if (baseDir == null) {
            Toast.makeText(this, R.string.projects_storage_error, Toast.LENGTH_SHORT).show();
        } else {
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
        adapter.submitList(proyectos);
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

    private void onProjectClick(File projectDir) {
        CharSequence[] options = {
                getString(R.string.project_action_export),
                getString(R.string.project_action_edit)
        };
        new AlertDialog.Builder(this)
                .setTitle(projectDir.getName())
                .setItems(options, (dialog, which) -> handleProjectAction(which, projectDir))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleProjectAction(int which, File projectDir) {
        switch (which) {
            case 0:
                exportProject(projectDir);
                break;
            case 1:
                editProject(projectDir);
                break;
            default:
                break;
        }
    }

    private void exportProject(File projectDir) {
        Toast.makeText(this,
                getString(R.string.project_export_message, projectDir.getName()),
                Toast.LENGTH_SHORT).show();

    }

    private void editProject(File projectDir) {
        Toast.makeText(this,
                getString(R.string.project_edit_message, projectDir.getName()),
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, CapturaArbolActivity.class);
        intent.putExtra("PROYECTO", projectDir.getName());
        intent.putExtra("RUTA_CARPETA", projectDir.getAbsolutePath());
        startActivity(intent);

    }

    private static class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {

        interface OnProjectClickListener {
            void onProjectClick(File projectDir);
        }

        private final List<File> projects = new ArrayList<>();
        private final OnProjectClickListener listener;

        ProjectsAdapter(OnProjectClickListener listener) {
            this.listener = listener;
        }

        void submitList(List<File> newProjects) {
            projects.clear();
            if (newProjects != null) {
                projects.addAll(newProjects);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_project_directory, parent, false);
            return new ProjectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
            File projectDir = projects.get(position);
            holder.bind(projectDir, listener);
        }

        @Override
        public int getItemCount() {
            return projects.size();
        }

        static class ProjectViewHolder extends RecyclerView.ViewHolder {

            private final TextView projectName;
            private final TextView projectPath;
            private final View moreActions;

            ProjectViewHolder(@NonNull View itemView) {
                super(itemView);
                projectName = itemView.findViewById(R.id.textProjectName);
                projectPath = itemView.findViewById(R.id.textProjectPath);
                moreActions = itemView.findViewById(R.id.imageMore);
            }

            void bind(File projectDir, OnProjectClickListener listener) {
                projectName.setText(projectDir.getName());
                projectPath.setText(projectDir.getAbsolutePath());
                View.OnClickListener clickListener = v -> listener.onProjectClick(projectDir);
                itemView.setOnClickListener(clickListener);
                moreActions.setOnClickListener(clickListener);
            }
        }
    }
}