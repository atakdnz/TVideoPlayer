package com.brouken.player.transform;

import android.view.View;

public class TransformController {
    private final VideoTransformState state = new VideoTransformState();

    public VideoTransformState getState() {
        return state;
    }

    public void setMetadataRotationDegrees(int degrees) {
        state.metadataRotationDegrees = normalizeRotation(degrees);
    }

    public void rotateLeft() {
        state.userRotationDegrees = normalizeRotation(state.userRotationDegrees - 90);
    }

    public void rotateRight() {
        state.userRotationDegrees = normalizeRotation(state.userRotationDegrees + 90);
    }

    public void toggleHorizontalFlip() {
        state.flipHorizontal = !state.flipHorizontal;
    }

    public void toggleVerticalFlip() {
        state.flipVertical = !state.flipVertical;
    }

    public void setZoom(float zoom) {
        state.zoom = Math.max(0.1f, Math.min(10f, zoom));
        if (state.zoom <= 1f) {
            state.panX = 0f;
            state.panY = 0f;
        }
    }

    public void setUserRotationDegrees(int degrees) {
        state.userRotationDegrees = normalizeRotation(degrees);
    }

    public void setFlipHorizontal(boolean flipHorizontal) {
        state.flipHorizontal = flipHorizontal;
    }

    public void setFlipVertical(boolean flipVertical) {
        state.flipVertical = flipVertical;
    }

    public void setPan(float panX, float panY) {
        state.panX = panX;
        state.panY = panY;
    }

    public void panBy(float dx, float dy, int containerWidth, int containerHeight) {
        if (state.zoom <= 1f) {
            state.panX = 0f;
            state.panY = 0f;
            return;
        }
        float maxX = Math.max(0f, containerWidth * (state.zoom - 1f) / 2f);
        float maxY = Math.max(0f, containerHeight * (state.zoom - 1f) / 2f);
        state.panX = clamp(state.panX + dx, -maxX, maxX);
        state.panY = clamp(state.panY + dy, -maxY, maxY);
    }

    public void resetZoom() {
        state.resetZoom();
    }

    public void resetAll() {
        state.resetAll();
    }

    public void applyTo(View view) {
        if (view == null) {
            return;
        }
        float scaleX = state.zoom * (state.flipHorizontal ? -1f : 1f);
        float scaleY = state.zoom * (state.flipVertical ? -1f : 1f);
        view.setRotation(state.userRotationDegrees);
        view.setScaleX(scaleX);
        view.setScaleY(scaleY);
        view.setTranslationX(state.panX);
        view.setTranslationY(state.panY);
    }

    private static int normalizeRotation(int degrees) {
        int normalized = degrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
