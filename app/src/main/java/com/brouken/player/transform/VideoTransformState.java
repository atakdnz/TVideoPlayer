package com.brouken.player.transform;

public class VideoTransformState {
    public int metadataRotationDegrees;
    public int userRotationDegrees;
    public boolean flipHorizontal;
    public boolean flipVertical;
    public float zoom = 1f;
    public float panX;
    public float panY;
    public ResizeMode resizeMode = ResizeMode.Fit;

    public int effectiveRotationDegrees() {
        int value = (metadataRotationDegrees + userRotationDegrees) % 360;
        return value < 0 ? value + 360 : value;
    }

    public void resetZoom() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    public void resetAll() {
        userRotationDegrees = 0;
        flipHorizontal = false;
        flipVertical = false;
        resetZoom();
        resizeMode = ResizeMode.Fit;
    }
}
