package com.brouken.player.frame;

public class VideoMetadata {
    public final long durationMs;
    public final Integer width;
    public final Integer height;
    public final Integer metadataRotationDegrees;
    public final Integer frameCount;
    public final Float estimatedFps;
    public final RetrieverAccessMode retrieverAccessMode;
    public final boolean frameIndexAvailable;

    public VideoMetadata(
            long durationMs,
            Integer width,
            Integer height,
            Integer metadataRotationDegrees,
            Integer frameCount,
            Float estimatedFps,
            RetrieverAccessMode retrieverAccessMode,
            boolean frameIndexAvailable
    ) {
        this.durationMs = durationMs;
        this.width = width;
        this.height = height;
        this.metadataRotationDegrees = metadataRotationDegrees;
        this.frameCount = frameCount;
        this.estimatedFps = estimatedFps;
        this.retrieverAccessMode = retrieverAccessMode;
        this.frameIndexAvailable = frameIndexAvailable;
    }
}
