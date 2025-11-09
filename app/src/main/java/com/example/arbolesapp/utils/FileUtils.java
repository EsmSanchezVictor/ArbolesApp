package com.example.arbolesapp.utils;

import android.content.Context;
import java.io.File;
import android.os.Environment;
public class FileUtils {
    private static final String DOCUMENTS_FOLDER_NAME = Environment.DIRECTORY_DOCUMENTS;
    private static final String PROJECTS_FOLDER_NAME = "Projects";
    public static File crearCarpetaProyecto(Context ctx, String nombreProyecto) {
        File baseDir = obtenerCarpetaBase(ctx);
        if (baseDir == null) {
            return null;
        }
        File proyectoDir = new File(baseDir, nombreProyecto);
        if (!proyectoDir.exists() && !proyectoDir.mkdirs()) {
            return null;
        }

        return proyectoDir;
    }
    public static File obtenerCarpetaBase(Context ctx) {
        File internalDir = ctx.getFilesDir();
        if (internalDir == null) {
            return null;
        }
        File documentsDir = new File(internalDir, DOCUMENTS_FOLDER_NAME);
        if (!documentsDir.exists() && !documentsDir.mkdirs()) {
            return null;
        }
        File baseDir = new File(documentsDir, PROJECTS_FOLDER_NAME);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return null;
        }
        return baseDir;
    }
}

