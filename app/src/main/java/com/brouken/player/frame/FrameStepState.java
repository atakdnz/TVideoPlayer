package com.brouken.player.frame;

import android.graphics.Bitmap;

public class FrameStepState {
    public FrameStepMode mode = FrameStepMode.Unavailable;
    public Integer currentFrameIndex;
    public Integer totalFrames;
    public Float estimatedFps;
    public Bitmap currentPreviewBitmap;
    public boolean loadingFrame;
    public String message;
    public String error;

    public boolean isFrameMode() {
        return mode == FrameStepMode.IndexedExactBestEffort || mode == FrameStepMode.TimestampEstimated;
    }
}
