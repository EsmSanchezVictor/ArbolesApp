
package com.example.arbolesapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String FILE = "prefs_arboles";
    private static final String KEY_USE_GPS = "use_gps";
    private static final String KEY_PHOTO_PREFIX = "photo_prefix";

    public static boolean isUseGps(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_USE_GPS, true);
    }

    public static void setUseGps(Context ctx, boolean value) {
        SharedPreferences.Editor ed = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        ed.putBoolean(KEY_USE_GPS, value).apply();
    }

    public static String getPhotoPrefix(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_PHOTO_PREFIX, "ARBOL");
    }

    public static void setPhotoPrefix(Context ctx, String prefix) {
        SharedPreferences.Editor ed = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        ed.putString(KEY_PHOTO_PREFIX, prefix == null ? "ARBOL" : prefix).apply();
    }
}
