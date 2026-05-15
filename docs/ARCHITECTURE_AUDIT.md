# TVideoPlayer Architecture Audit

## Phase 1 Build

- Fork target: `https://github.com/eneim/exobase`
- Local fork remote: `https://github.com/atakdnz/TVideoPlayer.git`
- Branch: `feature/precision-viewer`
- Baseline build command: `./gradlew assembleLatestUniversalDebug`
- Baseline result: build successful.

## Current Architecture

- Media3/ExoPlayer is created in `app/src/main/java/com/brouken/player/PlayerActivity.java`, inside `initializePlayer()`.
- The player is a static `ExoPlayer` assigned to `CustomPlayerView` through `playerView.setPlayer(player)`.
- The player view is `com.brouken.player.dtpv.DoubleTapPlayerView`, which extends the app's Media3 `PlayerView` stack.
- The main layout is `app/src/main/res/layout/activity_player.xml`.
- A TextureView variant already exists at `app/src/main/res/layout/activity_player_textureview.xml` and uses `app:surface_type="texture_view"`.
- Existing controls are built from `exo_player_control_view.xml` plus dynamic buttons inserted in `PlayerActivity.onCreate()`.
- Existing gestures live mainly in `CustomPlayerView.java`: horizontal seek, vertical brightness/volume, and pinch zoom.
- Current zoom uses `getVideoSurfaceView().setScaleX/Y(...)`.
- Resize mode is persisted in `Prefs` and uses Media3 `AspectRatioFrameLayout` constants.
- File opening uses SAF through `ACTION_OPEN_DOCUMENT` in `PlayerActivity.openFile()`.
- Persistable URI permissions are taken in `onActivityResult()`.
- Settings are persisted in `Prefs` through SharedPreferences plus an internal serialized recent-position map.

## Implementation Placement

- `TransformController` belongs next to the existing player surface logic and is integrated into `CustomPlayerView`.
- `FrameStepEngine` belongs in a separate `frame` package and is driven by `PlayerActivity` only at UI boundaries.
- `VideoMetadataReader` belongs in the frame package because retriever access and indexed frame availability are coupled.
- Custom playback speed UI belongs in `PlayerActivity`, next to the dynamic bottom control buttons.
- Per-video transform persistence can extend `Prefs` initially; a richer per-video key store can replace it later.

## Notes

- This app is Java, not Kotlin. The first implementation keeps Java to minimize churn.
- The codebase is mature but concentrated in `PlayerActivity`, so new precision systems are separated into packages where possible.
- The current app already supports local video, SAF, speed persistence, pinch zoom, fit/crop resize, audio/subtitle tracks, PiP, and broad Media3 playback.
