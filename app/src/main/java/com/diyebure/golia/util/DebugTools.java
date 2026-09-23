package com.diyebure.golia.util;

import android.content.Context;

import com.diyebure.golia.BuildConfig;

/**
 * Punto de entrada para herramientas de depuración. Todas las acciones se
 * ejecutan únicamente cuando {@code BuildConfig.DEBUG} es {@code true}; en
 * builds release los métodos son no-op.
 *
 * <p>El código de la app (por ejemplo {@code MainActivity}) puede llamar a
 * {@code DebugTools} sin condicionar manualmente: la propia clase se encarga de
 * no hacer nada en release.</p>
 */
public final class DebugTools {

    private DebugTools() {
        // No instanciable.
    }

    /** @return {@code true} solo en builds debug. */
    public static boolean isEnabled() {
        return BuildConfig.DEBUG;
    }

    /**
     * Exporta la base de datos a la carpeta pública de Descargas (solo en
     * debug). No-op en release.
     */
    public static void exportDatabase(Context context) {
        if (BuildConfig.DEBUG) {
            DatabaseExporter.exportToDownloads(context);
        }
    }
}
