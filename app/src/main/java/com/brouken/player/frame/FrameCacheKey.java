package com.brouken.player.frame;

import java.util.Objects;

public class FrameCacheKey {
    public final String videoKey;
    public final int frameIndex;

    public FrameCacheKey(String videoKey, int frameIndex) {
        this.videoKey = videoKey;
        this.frameIndex = frameIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FrameCacheKey)) return false;
        FrameCacheKey that = (FrameCacheKey) o;
        return frameIndex == that.frameIndex && Objects.equals(videoKey, that.videoKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(videoKey, frameIndex);
    }
}
