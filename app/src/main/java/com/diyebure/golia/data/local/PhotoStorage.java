package com.diyebure.golia.data.local;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.exifinterface.media.ExifInterface;

import com.diyebure.golia.domain.common.Result;
import com.diyebure.golia.domain.error.AuthError;
import com.diyebure.golia.domain.error.AuthException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Processes and stores the profile photo ({@code Profile_Photo}) inside the app's
 * internal storage ({@code filesDir}) and returns the absolute path of the written file.
 *
 * <p>Pipeline (R8.6, R8.7, R8.8, R9.2, R9.3, R9.9):
 * <ol>
 *   <li>Validate MIME type ({@code image/jpeg}, {@code image/png}, {@code image/webp})
 *       and size (&le; 10 MB) via {@link ContentResolver}, without decoding the full image.</li>
 *   <li>Read source dimensions with {@link BitmapFactory.Options#inJustDecodeBounds}, compute
 *       {@code inSampleSize} to bound memory usage (anti-OOM) and decode the bitmap bounded.</li>
 *   <li>Correct EXIF orientation with {@link ExifInterface}.</li>
 *   <li>Downscale to fit within 512&times;512 preserving aspect ratio using the pure
 *       {@link #computeTargetDimensions(int, int, int)} calculation, then re-compress to JPEG.</li>
 *   <li>Write to {@code filesDir} with the deterministic name {@code avatar_{userId}.jpg},
 *       deleting a stale previous file whose extension differs, and return the absolute path.</li>
 * </ol>
 *
 * <p>The origin {@code content://} URI is never persisted; only the absolute path of the
 * copied file inside {@code filesDir} is returned (R9.2).
 *
 * <p>Injected as an application-scoped singleton via Hilt (constructor injection).
 */
@Singleton
public class PhotoStorage {

    /** Target bounding box (both width and height) for the stored avatar (R8.6). */
    static final int TARGET_MAX = 512;

    /** Maximum accepted source size in bytes (10 MB, R8.8). */
    static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L;

    /** JPEG re-compression quality used when writing the stored avatar. */
    private static final int JPEG_QUALITY = 90;

    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final String MIME_WEBP = "image/webp";

    private final Context context;

    @Inject
    public PhotoStorage(@ApplicationContext Context context) {
        this.context = context;
    }

    /**
     * Validates, processes and stores the image referenced by {@code source} as the avatar
     * for {@code userId}.
     *
     * @param userId the id of the {@code Current_User}; used to build the deterministic file name
     * @param source the origin {@code content://} URI of the picked/captured image
     * @return {@code Result.Success} with the absolute path of the written file, or
     *         {@code Result.Error(new AuthException(VALIDATION_ERROR))} when the image is not
     *         a supported format or exceeds the size limit, or
     *         {@code Result.Error(new AuthException(PERSISTENCE_ERROR))} on a technical failure.
     */
    public Result<String> processAndStore(String userId, Uri source) {
        if (userId == null || userId.isEmpty() || source == null) {
            return new Result.Error(new AuthException(AuthError.VALIDATION_ERROR));
        }

        final ContentResolver resolver = context.getContentResolver();

        // 1. Validate format and size (R8.7, R8.8) without decoding the full image.
        final String mimeType = resolver.getType(source);
        if (!isSupportedMime(mimeType)) {
            return new Result.Error(new AuthException(AuthError.VALIDATION_ERROR));
        }
        final long size = readSize(resolver, source);
        if (size < 0 || size > MAX_SIZE_BYTES) {
            return new Result.Error(new AuthException(AuthError.VALIDATION_ERROR));
        }

        Bitmap decoded = null;
        Bitmap oriented = null;
        Bitmap scaled = null;
        try {
            // 2. Read dimensions with inJustDecodeBounds, compute inSampleSize (anti-OOM, R8.6).
            final BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream boundsStream = resolver.openInputStream(source)) {
                if (boundsStream == null) {
                    return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR));
                }
                BitmapFactory.decodeStream(boundsStream, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return new Result.Error(new AuthException(AuthError.VALIDATION_ERROR));
            }

            final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize =
                    computeInSampleSize(bounds.outWidth, bounds.outHeight, TARGET_MAX);

            try (InputStream decodeStream = resolver.openInputStream(source)) {
                if (decodeStream == null) {
                    return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR));
                }
                decoded = BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
            }
            if (decoded == null) {
                return new Result.Error(new AuthException(AuthError.VALIDATION_ERROR));
            }

            // 3. Correct EXIF orientation (R8.6).
            oriented = applyExifOrientation(resolver, source, decoded);

            // 4. Downscale to fit 512x512 preserving aspect ratio (R8.6).
            final int[] target =
                    computeTargetDimensions(oriented.getWidth(), oriented.getHeight(), TARGET_MAX);
            if (target[0] == oriented.getWidth() && target[1] == oriented.getHeight()) {
                scaled = oriented;
            } else {
                scaled = Bitmap.createScaledBitmap(oriented, target[0], target[1], true);
            }

            // 5. Write to filesDir with deterministic name; drop stale previous file (R9.3, R9.9).
            final File destination = new File(context.getFilesDir(), "avatar_" + userId + ".jpg");
            deleteStalePrevious(userId, destination);

            try (OutputStream out = new FileOutputStream(destination)) {
                final boolean ok = scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
                if (!ok) {
                    return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR));
                }
                out.flush();
            }

            return new Result.Success<>(destination.getAbsolutePath());
        } catch (IOException | SecurityException | OutOfMemoryError e) {
            return new Result.Error(new AuthException(AuthError.PERSISTENCE_ERROR));
        } finally {
            // Recycle intermediate bitmaps we own (avoid double-recycling shared references).
            if (scaled != null && scaled != oriented && scaled != decoded) {
                scaled.recycle();
            }
            if (oriented != null && oriented != decoded) {
                oriented.recycle();
            }
            if (decoded != null) {
                decoded.recycle();
            }
        }
    }

    /**
     * Pure calculation of target dimensions that fit within a {@code max}&times;{@code max}
     * bounding box while preserving the source aspect ratio.
     *
     * <p>Extracted as a static, side-effect-free method so it can be unit/property tested
     * without Android bitmap dependencies (Property 6).
     *
     * @param width  source width, must be &gt; 0
     * @param height source height, must be &gt; 0
     * @param max    bounding box side length, must be &gt; 0
     * @return a two-element array {@code {targetWidth, targetHeight}}; both &ge; 1 and &le; max
     */
    static int[] computeTargetDimensions(int width, int height, int max) {
        if (width <= 0 || height <= 0 || max <= 0) {
            throw new IllegalArgumentException("width, height and max must be > 0");
        }
        // Already within bounds: no upscaling.
        if (width <= max && height <= max) {
            return new int[] {width, height};
        }
        final double scale = Math.min((double) max / width, (double) max / height);
        int targetWidth = (int) Math.round(width * scale);
        int targetHeight = (int) Math.round(height * scale);
        // Guard against rounding to 0 or overflowing the bounding box by 1px.
        targetWidth = Math.max(1, Math.min(targetWidth, max));
        targetHeight = Math.max(1, Math.min(targetHeight, max));
        return new int[] {targetWidth, targetHeight};
    }

    /**
     * Computes the largest power-of-two {@code inSampleSize} that keeps both decoded
     * dimensions at or above the target bound, bounding decode memory usage (anti-OOM).
     */
    static int computeInSampleSize(int width, int height, int target) {
        int inSampleSize = 1;
        if (width > target || height > target) {
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;
            while ((halfWidth / inSampleSize) >= target
                    && (halfHeight / inSampleSize) >= target) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private boolean isSupportedMime(String mimeType) {
        return MIME_JPEG.equals(mimeType)
                || MIME_PNG.equals(mimeType)
                || MIME_WEBP.equals(mimeType);
    }

    /**
     * Reads the source size in bytes via the {@code OpenableColumns.SIZE} cursor column,
     * falling back to {@link AssetFileDescriptor} when the column is unavailable.
     *
     * @return the size in bytes, or {@code -1} when it cannot be determined
     */
    private long readSize(ContentResolver resolver, Uri source) {
        try (Cursor cursor = resolver.query(source, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
            // Fall through to the AssetFileDescriptor path below.
        }
        try (AssetFileDescriptor descriptor = resolver.openAssetFileDescriptor(source, "r")) {
            if (descriptor != null) {
                final long length = descriptor.getLength();
                if (length != AssetFileDescriptor.UNKNOWN_LENGTH) {
                    return length;
                }
            }
        } catch (IOException | SecurityException ignored) {
            // Unknown size.
        }
        return -1;
    }

    /**
     * Applies the EXIF orientation of the source to the decoded bitmap, returning a rotated/
     * flipped bitmap when needed or the original bitmap when no transform applies.
     */
    private Bitmap applyExifOrientation(ContentResolver resolver, Uri source, Bitmap bitmap) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream exifStream = resolver.openInputStream(source)) {
            if (exifStream != null) {
                final ExifInterface exif = new ExifInterface(exifStream);
                orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (IOException ignored) {
            // No EXIF data available; keep the original orientation.
        }

        final Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.postRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.postRotate(270);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Deletes a stale previous avatar file for {@code userId} whose extension differs from the
     * new destination, avoiding orphaned files when the stored extension changes (R9.9).
     */
    private void deleteStalePrevious(String userId, File destination) {
        final File[] files = context.getFilesDir().listFiles();
        if (files == null) {
            return;
        }
        final String prefix = "avatar_" + userId + ".";
        final String destName = destination.getName();
        for (File file : files) {
            final String name = file.getName();
            if (name.startsWith(prefix) && !name.equals(destName)) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }
}
