package com.brouken.player.frame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

public class VideoMetadataReader {
    public VideoMetadata read(Context context, Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        AssetFileDescriptor afd = null;
        RetrieverAccessMode accessMode = RetrieverAccessMode.None;
        try {
            try {
                retriever.setDataSource(context, uri);
                accessMode = RetrieverAccessMode.ContextUri;
            } catch (Exception firstError) {
                afd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                if (afd == null) {
                    throw firstError;
                }
                retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                accessMode = RetrieverAccessMode.AssetFileDescriptor;
            }

            Long durationMs = parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            Integer width = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            Integer height = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            Integer rotation = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            Integer frameCount = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                frameCount = parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
            }
            Float estimatedFps = null;
            if (frameCount != null && frameCount > 0 && durationMs != null && durationMs > 0) {
                estimatedFps = frameCount / (durationMs / 1000f);
            }
            boolean indexed = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && frameCount != null && frameCount > 0) {
                try {
                    indexed = retriever.getFrameAtIndex(Math.min(1, frameCount - 1)) != null;
                } catch (Exception ignored) {
                    indexed = false;
                }
            }
            return new VideoMetadata(
                    durationMs == null ? 0L : durationMs,
                    width,
                    height,
                    rotation,
                    frameCount,
                    estimatedFps,
                    accessMode,
                    indexed
            );
        } catch (Exception e) {
            return new VideoMetadata(0L, null, null, null, null, null, accessMode, false);
        } finally {
            try {
                if (afd != null) {
                    afd.close();
                }
            } catch (Exception ignored) {
            }
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    static Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }
}
