package com.example.arbolesapp.utils;

import android.content.Context;
import java.io.File;

public class FileUtils {

    public static File crearCarpetaProyecto(Context ctx, String nombreProyecto) {
        File baseDir = new File(ctx.getExternalFilesDir(null), "Projects");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        File proyectoDir = new File(baseDir, nombreProyecto);
        if (!proyectoDir.exists()) {
            if (!proyectoDir.mkdirs()) return null;
        }
        return proyectoDir;
    }
}
