# TVideoPlayer v1 Testing Checklist

## Build

- Build variant: `assembleLatestUniversalDebug`
- Expected result: successful APK build.

## Core Playback

- Open a local video through the system picker.
- Open a video through Android "Open with".
- Open/share a text URL to the app.
- Play, pause, seek, skip forward, and skip backward.
- Confirm subtitles and audio track selection still work for files that have them.

## Precision Controls

- Pause playback and confirm frame previous/next controls appear.
- Tap next frame on a supported MP4 and confirm the overlay updates.
- Confirm indexed mode shows `Frame N / Total` only when frame count is available.
- Test a source where retriever access fails and confirm playback still works.
- Confirm fallback mode is labeled `Estimated frame step`.
- Press play from frame mode and confirm the overlay hides and playback resumes near the selected frame.

## Transforms

- Rotate left and right while playing.
- Rotate left and right while paused.
- Flip horizontally and vertically while playing.
- Flip horizontally and vertically while paused.
- Pinch zoom and confirm the zoom percentage appears.
- Pan while paused and zoomed.
- Reset zoom and confirm rotation/flip are preserved.
- Reset all transforms and confirm rotation, flip, zoom, pan, and resize return to defaults.

## Speed

- Select each preset from `0.1x` through `10.0x`.
- Enter a custom speed such as `1.7x`.
- Try invalid custom values and confirm the app does not crash.

## Persistence

- Open a video, rotate, flip, zoom, pan, change speed, and close the app.
- Reopen the same video and confirm state is restored.
- Open a different video and confirm it does not inherit unrelated transform state.

## Info Sheet

- Open Display transforms > Info.
- Confirm it reports surface type as `TextureView`.
- Confirm it shows metadata, frame mode, speed, zoom, flips, rotation, and retriever access.

## Stress Files

- Portrait phone video.
- Landscape phone video.
- Front-camera mirrored video.
- Screen recording with text.
- 24/30/60 fps MP4.
- Variable frame-rate video.
- MKV file.
- 4K video.
- HDR video if available.
- `content://` URI from a cloud/file provider.
