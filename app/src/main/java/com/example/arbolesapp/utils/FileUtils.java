package com.example.arbolesapp.utils;

import android.content.Context;
import java.io.File;

public class FileUtils {
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
        File externalDir = ctx.getExternalFilesDir(null);
        if (externalDir == null) {
            return null;
        }
        File baseDir = new File(externalDir, PROJECTS_FOLDER_NAME);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return null;
        }
        return baseDir;
    }
}

