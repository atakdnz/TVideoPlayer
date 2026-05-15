# TVideoPlayer Frame Stepping Notes

## Retriever Access

Frame metadata and previews use `MediaMetadataRetriever`.

Access order:

1. `setDataSource(context, uri)`
2. `ContentResolver.openAssetFileDescriptor(uri, "r")`
3. `setDataSource(fileDescriptor, startOffset, length)`

Failure disables indexed mode but must not block normal ExoPlayer playback.

## Indexed Mode

Indexed best-effort mode is enabled only on API 28+ when:

- `METADATA_KEY_VIDEO_FRAME_COUNT` exists
- frame count is greater than zero
- `getFrameAtIndex(...)` succeeds for a probe frame

The UI may show `Frame N / Total` only in this mode.

## Estimated Mode

If indexed access is unavailable, the engine uses timestamp-based estimated stepping. It uses duration and frame count when available to estimate FPS; otherwise it falls back to 30 fps. This mode is labeled estimated and does not show a fake total frame count.

## Units

- ExoPlayer positions are milliseconds.
- `MediaMetadataRetriever.getFrameAtTime(...)` requires microseconds.
- Helpers are named with units: `msToUs`, `usToMs`, `frameIndexToTimestampMs`, `timestampMsToFrameIndex`, `getFrameAtTimeUs`.

## Sync Back

When leaving indexed frame mode, the selected frame index is converted back to milliseconds and ExoPlayer seeks near that frame before playback resumes.

## Current Implementation Status

- Frame extraction runs off the UI thread from `PlayerActivity`.
- Stale frame requests are ignored when the user exits frame mode or starts playback.
- Preview bitmaps are downscaled to a maximum side of 1920 px.
- Frame cache uses a 64 MB budget and reduces entry count for large/unknown videos.
- Asset file descriptors stay open for the lifetime of the associated retriever call.
