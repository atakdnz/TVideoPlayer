package com.brouken.player.settings;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;

import java.io.Serializable;

public class PerVideoKey implements Serializable {
    public final String uriString;
    public final String displayName;
    public final Long sizeBytes;
    public final Long durationMs;
    public final Long lastModified;

    public PerVideoKey(String uriString, String displayName, Long sizeBytes, Long durationMs, Long lastModified) {
        this.uriString = uriString;
        this.displayName = displayName;
        this.sizeBytes = sizeBytes;
        this.durationMs = durationMs;
        this.lastModified = lastModified;
    }

    public static PerVideoKey from(Context context, Uri uri, Long durationMs) {
        if (uri == null) {
            return new PerVideoKey("", null, null, durationMs, null);
        }
        String displayName = null;
        Long sizeBytes = null;
        Long lastModified = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex);
                }
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
            if (documentFile != null) {
                if (displayName == null) {
                    displayName = documentFile.getName();
                }
                long length = documentFile.length();
                if (sizeBytes == null && length > 0) {
                    sizeBytes = length;
                }
                long modified = documentFile.lastModified();
                if (modified > 0) {
                    lastModified = modified;
                }
            }
        } catch (Exception ignored) {
        }
        return new PerVideoKey(uri.toString(), displayName, sizeBytes, durationMs, lastModified);
    }

    public String exactKey() {
        return uriString;
    }

    public String layeredKey() {
        return uriString + "|" + clean(displayName) + "|" + clean(sizeBytes) + "|" + clean(durationMs) + "|" + clean(lastModified);
    }

    private static String clean(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
