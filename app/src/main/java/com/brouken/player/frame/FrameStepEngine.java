package com.brouken.player.frame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

public class FrameStepEngine {
    private static final float FALLBACK_FPS = 30f;

    private final Context context;
    private final VideoMetadataReader metadataReader = new VideoMetadataReader();
    private final FrameCache frameCache = new FrameCache(64L * 1024L * 1024L, 7);
    private final FrameStepState state = new FrameStepState();
    private Uri uri;
    private VideoMetadata metadata;
    private String videoKey;
    private long estimatedPositionMs;

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
        state.totalFrames = metadata.frameCount;
        state.estimatedFps = metadata.estimatedFps;
        if (metadata.frameIndexAvailable) {
            state.mode = FrameStepMode.IndexedExactBestEffort;
            state.message = null;
        } else if (metadata.retrieverAccessMode != RetrieverAccessMode.None) {
            state.mode = FrameStepMode.TimestampEstimated;
            state.message = "Estimated frame step";
        } else {
            state.mode = FrameStepMode.Unavailable;
            state.message = "Frame extraction unavailable for this source.";
        }
    }

    public Bitmap enterFrameMode(long currentPositionMs) {
        if (state.mode == FrameStepMode.Unavailable) {
            return null;
        }
        estimatedPositionMs = Math.max(0L, currentPositionMs);
        if (state.mode == FrameStepMode.IndexedExactBestEffort) {
            state.currentFrameIndex = timestampMsToFrameIndex(currentPositionMs);
            return jumpToFrame(state.currentFrameIndex);
        }
        return getFrameAtTimeUs(msToUs(estimatedPositionMs));
    }

    public Bitmap nextFrame() {
        if (state.mode == FrameStepMode.IndexedExactBestEffort) {
            int next = state.currentFrameIndex == null ? 0 : state.currentFrameIndex + 1;
            return jumpToFrame(next);
        }
        estimatedPositionMs += estimatedFrameDurationMs();
        return getFrameAtTimeUs(msToUs(estimatedPositionMs));
    }

    public Bitmap previousFrame() {
        if (state.mode == FrameStepMode.IndexedExactBestEffort) {
            int previous = state.currentFrameIndex == null ? 0 : state.currentFrameIndex - 1;
            return jumpToFrame(previous);
        }
        estimatedPositionMs = Math.max(0L, estimatedPositionMs - estimatedFrameDurationMs());
        return getFrameAtTimeUs(msToUs(estimatedPositionMs));
    }

    public Bitmap jumpToFrame(int index) {
        if (state.mode != FrameStepMode.IndexedExactBestEffort || metadata == null || metadata.frameCount == null) {
            return null;
        }
        int clamped = Math.max(0, Math.min(index, metadata.frameCount - 1));
        state.currentFrameIndex = clamped;
        FrameCacheKey key = new FrameCacheKey(videoKey, clamped);
        Bitmap cached = frameCache.get(key);
        if (cached != null && !cached.isRecycled()) {
            state.currentPreviewBitmap = cached;
            return cached;
        }
        MediaMetadataRetriever retriever = openRetriever();
        if (retriever == null) {
            state.error = "Frame extraction unavailable for this source.";
            return null;
        }
        try {
            Bitmap bitmap = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? retriever.getFrameAtIndex(clamped) : null;
            state.currentPreviewBitmap = bitmap;
            frameCache.put(key, bitmap);
            return bitmap;
        } catch (Exception e) {
            state.error = "Frame extraction unavailable for this source.";
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
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
        MediaMetadataRetriever retriever = openRetriever();
        if (retriever == null) {
            state.error = "Frame extraction unavailable for this source.";
            return null;
        }
        try {
            Bitmap bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
            state.currentPreviewBitmap = bitmap;
            return bitmap;
        } catch (Exception e) {
            state.error = "Frame extraction unavailable for this source.";
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
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

    private MediaMetadataRetriever openRetriever() {
        if (uri == null) {
            return null;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            return retriever;
        } catch (Exception firstError) {
            try {
                AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(uri, "r");
                if (afd == null) {
                    retriever.release();
                    return null;
                }
                retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                return retriever;
            } catch (Exception secondError) {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
                return null;
            }
        }
    }
}
