package com.brouken.player.frame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

public class FrameStepEngine {
    private static final float FALLBACK_FPS = 30f;
    private static final int MAX_PREVIEW_DIMENSION = 1920;

    private final Context context;
    private final VideoMetadataReader metadataReader = new VideoMetadataReader();
    private final FrameCache frameCache = new FrameCache(64L * 1024L * 1024L, 7);
    private final FrameStepState state = new FrameStepState();
    private Uri uri;
    private VideoMetadata metadata;
    private String videoKey;
    private long estimatedPositionMs;
    private boolean indexedExtractionFailed;

    public FrameStepEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public FrameStepState getState() {
        return state;
    }

    public VideoMetadata getMetadata() {
        return metadata;
    }

    public void prepare(Uri uri) {
        release();
        this.uri = uri;
        this.videoKey = uri == null ? "" : uri.toString();
        if (uri == null) {
            state.mode = FrameStepMode.Unavailable;
            state.message = "Frame extraction unavailable for this source.";
            return;
        }
        metadata = metadataReader.read(context, uri);
        indexedExtractionFailed = false;
        state.totalFrames = metadata.frameCount;
        state.estimatedFps = metadata.estimatedFps;
        frameCache.setMaxEntries(isLargeVideo(metadata) ? 3 : 9);
        if (metadata.frameIndexAvailable) {
            state.mode = FrameStepMode.IndexedExactBestEffort;
            state.message = null;
        } else if (metadata.retrieverAccessMode != RetrieverAccessMode.None) {
            state.mode = FrameStepMode.TimestampEstimated;
            state.message = "Estimated frame step";
        } else {
            state.mode = FrameStepMode.SeekBased;
            state.message = "Seek-based frame step\nRendered by player";
        }
    }

    public Bitmap enterFrameMode(long currentPositionMs) {
        if (state.mode == FrameStepMode.Unavailable) {
            return null;
        }
        estimatedPositionMs = Math.max(0L, currentPositionMs);
        state.error = null;
        if (state.mode == FrameStepMode.IndexedExactBestEffort) {
            state.currentFrameIndex = timestampMsToFrameIndex(currentPositionMs);
            return jumpToFrame(state.currentFrameIndex);
        }
        if (state.mode == FrameStepMode.SeekBased) {
            state.message = "Seek-based frame step\nRendered by player";
            return null;
        }
        return getFrameAtTimeUs(msToUs(estimatedPositionMs));
    }

    public Bitmap nextFrame() {
        if (state.mode == FrameStepMode.IndexedExactBestEffort) {
            int next = state.currentFrameIndex == null ? 0 : state.currentFrameIndex + 1;
            return jumpToFrame(next);
        }
        estimatedPositionMs = clampPositionMs(estimatedPositionMs + estimatedFrameDurationMs());
        if (state.mode == FrameStepMode.SeekBased) {
            state.message = "Seek-based frame step\nRendered by player";
            state.error = null;
            return null;
        }
        return getFrameAtTimeUs(msToUs(estimatedPositionMs));
    }

    public Bitmap previousFrame() {
        if (state.mode == FrameStepMode.IndexedExactBestEffort) {
            int previous = state.currentFrameIndex == null ? 0 : state.currentFrameIndex - 1;
            return jumpToFrame(previous);
        }
        estimatedPositionMs = Math.max(0L, estimatedPositionMs - estimatedFrameDurationMs());
        if (state.mode == FrameStepMode.SeekBased) {
            state.message = "Seek-based frame step\nRendered by player";
            state.error = null;
            return null;
        }
        return getFrameAtTimeUs(msToUs(estimatedPositionMs));
    }

    public Bitmap jumpToFrame(int index) {
        if (state.mode != FrameStepMode.IndexedExactBestEffort || metadata == null || metadata.frameCount == null) {
            return null;
        }
        if (indexedExtractionFailed) {
            state.mode = FrameStepMode.TimestampEstimated;
            estimatedPositionMs = frameIndexToTimestampMs(index);
            state.currentFrameIndex = null;
            state.totalFrames = null;
            state.message = "Estimated frame step";
            return getFrameAtTimeUs(msToUs(estimatedPositionMs));
        }
        int clamped = Math.max(0, Math.min(index, metadata.frameCount - 1));
        state.currentFrameIndex = clamped;
        FrameCacheKey key = new FrameCacheKey(videoKey, clamped);
        Bitmap cached = frameCache.get(key);
        if (cached != null && !cached.isRecycled()) {
            state.currentPreviewBitmap = cached;
            return cached;
        }
        OpenedRetriever openedRetriever = openRetriever();
        if (openedRetriever == null) {
            state.mode = FrameStepMode.SeekBased;
            estimatedPositionMs = frameIndexToTimestampMs(clamped);
            state.currentFrameIndex = null;
            state.totalFrames = null;
            state.message = "Seek-based frame step\nRendered by player";
            state.error = null;
            return null;
        }
        try {
            Bitmap bitmap = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? openedRetriever.retriever.getFrameAtIndex(clamped) : null;
            bitmap = downscalePreview(bitmap);
            state.currentPreviewBitmap = bitmap;
            frameCache.put(key, bitmap);
            return bitmap;
        } catch (Throwable e) {
            indexedExtractionFailed = true;
            state.mode = FrameStepMode.TimestampEstimated;
            state.currentFrameIndex = null;
            state.totalFrames = null;
            state.message = "Estimated frame step";
            state.error = null;
            estimatedPositionMs = frameIndexToTimestampMs(clamped);
            return getFrameAtTimeUs(msToUs(estimatedPositionMs));
        } finally {
            openedRetriever.close();
        }
    }

    public long frameIndexToTimestampMs(int index) {
        if (metadata == null || metadata.frameCount == null || metadata.frameCount <= 0 || metadata.durationMs <= 0) {
            return 0L;
        }
        return Math.max(0L, Math.min(metadata.durationMs, Math.round(index * (metadata.durationMs / (float) metadata.frameCount))));
    }

    public int timestampMsToFrameIndex(long positionMs) {
        if (metadata == null || metadata.frameCount == null || metadata.frameCount <= 0 || metadata.durationMs <= 0) {
            return 0;
        }
        int index = Math.round(positionMs / (metadata.durationMs / (float) metadata.frameCount));
        return Math.max(0, Math.min(index, metadata.frameCount - 1));
    }

    public Bitmap getFrameAtTimeUs(long timeUs) {
        OpenedRetriever openedRetriever = openRetriever();
        if (openedRetriever == null) {
            state.error = "Frame extraction unavailable for this source.";
            return null;
        }
        try {
            Bitmap bitmap = downscalePreview(openedRetriever.retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST));
            state.currentPreviewBitmap = bitmap;
            return bitmap;
        } catch (Throwable e) {
            if (state.mode == FrameStepMode.TimestampEstimated) {
                state.message = "Estimated frame step\nFrame preview unavailable for this source.";
                state.error = null;
            } else {
                state.error = "Frame extraction unavailable for this source.";
            }
            return null;
        } finally {
            openedRetriever.close();
        }
    }

    public long selectedPositionMs() {
        if (state.mode == FrameStepMode.IndexedExactBestEffort && state.currentFrameIndex != null) {
            return frameIndexToTimestampMs(state.currentFrameIndex);
        }
        return estimatedPositionMs;
    }

    public void release() {
        frameCache.clear();
        state.mode = FrameStepMode.Unavailable;
        state.currentPreviewBitmap = null;
        state.currentFrameIndex = null;
        state.totalFrames = null;
        state.error = null;
        state.message = null;
    }

    public static long msToUs(long ms) {
        return ms * 1000L;
    }

    public static long usToMs(long us) {
        return us / 1000L;
    }

    private long estimatedFrameDurationMs() {
        float fps = metadata != null && metadata.estimatedFps != null && metadata.estimatedFps > 0 ? metadata.estimatedFps : FALLBACK_FPS;
        return Math.max(1L, Math.round(1000f / fps));
    }

    private long clampPositionMs(long positionMs) {
        long max = metadata == null || metadata.durationMs <= 0 ? Long.MAX_VALUE : metadata.durationMs;
        return Math.max(0L, Math.min(positionMs, max));
    }

    private boolean isLargeVideo(VideoMetadata metadata) {
        if (metadata == null || metadata.width == null || metadata.height == null) {
            return true;
        }
        return metadata.width >= 3840 || metadata.height >= 2160 || ((long) metadata.width * metadata.height) > (1920L * 1080L);
    }

    private Bitmap downscalePreview(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return bitmap;
        }
        int maxSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (maxSide <= MAX_PREVIEW_DIMENSION) {
            return bitmap;
        }
        float scale = MAX_PREVIEW_DIMENSION / (float) maxSide;
        int width = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(bitmap.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
        return scaled;
    }

    private OpenedRetriever openRetriever() {
        if (uri == null) {
            return null;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return new OpenedRetriever(retriever, null);
        } catch (Exception firstError) {
            try {
                AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                if (afd == null) {
                    retriever.release();
                    return null;
                }
                retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return new OpenedRetriever(retriever, afd);
            } catch (Exception secondError) {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
                return null;
            }
        }
    }

    private static class OpenedRetriever {
        final MediaMetadataRetriever retriever;
        final AssetFileDescriptor assetFileDescriptor;

        OpenedRetriever(MediaMetadataRetriever retriever, AssetFileDescriptor assetFileDescriptor) {
            this.retriever = retriever;
            this.assetFileDescriptor = assetFileDescriptor;
        }

        void close() {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
            try {
                if (assetFileDescriptor != null) {
                    assetFileDescriptor.close();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
