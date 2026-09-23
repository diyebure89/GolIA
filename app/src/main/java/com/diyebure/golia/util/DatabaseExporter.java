package com.diyebure.golia.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utilidad SOLO para builds debug: copia la base de datos Room desde el
 * almacenamiento privado de la app hacia la carpeta pública de Descargas,
 * para poder abrirla con apps externas como "SQLite Editor" (sin root).
 *
 * <p>Copia el archivo principal y, si existen, los sidecars {@code -wal} y
 * {@code -shm} del modo WAL, de modo que la copia refleje los datos más
 * recientes que aún no se han fusionado en el archivo principal.</p>
 *
 * <p>Solo debe invocarse en builds debug (a través de {@link DebugTools}, que
 * comprueba {@code BuildConfig.DEBUG}). Usa exclusivamente APIs estándar de
 * Android, por lo que su presencia en el APK es inofensiva mientras no se
 * llame en release.</p>
 */
public final class DatabaseExporter {

    private static final String TAG = "DatabaseExporter";
    private static final String DATABASE_NAME = "golia_database";

    private DatabaseExporter() {
        // Utilidad no instanciable.
    }

    /**
     * Exporta la base de datos a la carpeta pública de Descargas y muestra un
     * Toast con el resultado.
     *
     * @param context contexto de la app (se usa {@code getApplicationContext()}).
     */
    public static void exportToDownloads(Context context) {
        Context appContext = context.getApplicationContext();
        File dbFile = appContext.getDatabasePath(DATABASE_NAME);

        if (!dbFile.exists()) {
            Log.w(TAG, "La base de datos no existe todavía: " + dbFile.getAbsolutePath());
            toast(appContext, "La base de datos aún no existe");
            return;
        }

        try {
            // Copia el archivo principal y los sidecars WAL si están presentes.
            copyOne(appContext, dbFile, DATABASE_NAME + ".db");
            copyOptional(appContext, new File(dbFile.getPath() + "-wal"), DATABASE_NAME + ".db-wal");
            copyOptional(appContext, new File(dbFile.getPath() + "-shm"), DATABASE_NAME + ".db-shm");

            Log.i(TAG, "Base de datos exportada a Descargas");
            toast(appContext, "DB exportada a Descargas: " + DATABASE_NAME + ".db");
        } catch (IOException e) {
            Log.e(TAG, "Error exportando la base de datos", e);
            toast(appContext, "Error exportando la DB: " + e.getMessage());
        }
    }

    private static void copyOptional(Context context, File source, String targetName)
            throws IOException {
        if (source.exists()) {
            copyOne(context, source, targetName);
        }
    }

    private static void copyOne(Context context, File source, String targetName)
            throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyViaMediaStore(context, source, targetName);
        } else {
            copyToLegacyDownloads(source, targetName);
        }
    }

    /**
     * Android 10+ (scoped storage): escribe en Descargas vía MediaStore.
     */
    private static void copyViaMediaStore(Context context, File source, String targetName)
            throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, targetName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

        // Elimina una versión previa con el mismo nombre para evitar duplicados "(1)".
        context.getContentResolver().delete(
                collection,
                MediaStore.Downloads.DISPLAY_NAME + "=? AND "
                        + MediaStore.Downloads.RELATIVE_PATH + "=?",
                new String[]{targetName, Environment.DIRECTORY_DOWNLOADS + "/"});

        Uri itemUri = context.getContentResolver().insert(collection, values);
        if (itemUri == null) {
            throw new IOException("No se pudo crear el archivo en Descargas: " + targetName);
        }

        try (InputStream in = new FileInputStream(source);
             OutputStream out = context.getContentResolver().openOutputStream(itemUri)) {
            if (out == null) {
                throw new IOException("No se pudo abrir el destino: " + targetName);
            }
            copyStream(in, out);
        }
    }

    /**
     * Android 9 e inferior: copia directa al directorio público de Descargas.
     */
    private static void copyToLegacyDownloads(File source, String targetName) throws IOException {
        File downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloads.exists() && !downloads.mkdirs()) {
            throw new IOException("No se pudo acceder a la carpeta de Descargas");
        }
        File target = new File(downloads, targetName);
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            copyStream(in, out);
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    private static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
