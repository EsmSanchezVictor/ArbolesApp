package com.example.arbolesapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
//import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arbolesapp.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private static final int BUFFER_SIZE = 4096;

    private void exportProject(File projectDir) {
        if (projectDir == null || !projectDir.exists()) {
            Toast.makeText(this, R.string.project_export_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this,
                getString(R.string.project_export_preparing, projectDir.getName()),
                Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            File zipFile = null;
            try {
                zipFile = createProjectExportZip(projectDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            File finalZipFile = zipFile;
            runOnUiThread(() -> {
                if (finalZipFile != null) {
                    Toast.makeText(this, R.string.project_export_success, Toast.LENGTH_SHORT).show();
                    shareZipFile(projectDir.getName(), finalZipFile);
                } else {
                    Toast.makeText(this, R.string.project_export_error, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private File createProjectExportZip(File projectDir) throws IOException {
        File exportDir = new File(getCacheDir(), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            return null;
        }

        File zipFile = new File(exportDir, projectDir.getName() + ".zip");
        if (zipFile.exists() && !zipFile.delete()) {
            return null;
        }

        final boolean[] addedEntries = {false};
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(projectDir, projectDir, zos, addedEntries);
        }

        if (!addedEntries[0]) {
            zipFile.delete();
            return null;
        }

        return zipFile;
    }

    private void zipDirectory(File currentDir, File baseDir, ZipOutputStream zos, boolean[] addedEntries) throws IOException {
        File[] files = currentDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, baseDir, zos, addedEntries);
            } else if (shouldIncludeInExport(file)) {
                String entryName = getRelativePath(baseDir, file);
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    addedEntries[0] = true;
                }
            }
        }
    }

    private boolean shouldIncludeInExport(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".xlsx");
    }

    private String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(basePath)) {
            String relative = filePath.substring(basePath.length());
            while (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }
            return relative.replace(File.separatorChar, '/');
        }
        return file.getName();
    }

    private void shareZipFile(String projectName, File zipFile) {
        Uri zipUri = FileProvider.getUriForFile(this,
                "com.example.arbolesapp.fileprovider",
                zipFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : activities) {
            grantUriPermission(resolveInfo.activityInfo.packageName, zipUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(Intent.createChooser(shareIntent,
                getString(R.string.project_share_title, projectName)));

    }

    private void editProject(File projectDir) {
        Toast.makeText(this,
                getString(R.string.project_edit_message, projectDir.getName()),
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, GaleriaProyectoActivity.class);
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