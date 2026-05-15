# TVideoPlayer Transform System

## Surface Decision

FramePilot requires rotation, flip, zoom, pan, and frame bitmap overlays to affect actual video pixels. The default `PlayerView` surface may be a `SurfaceView`, and `SurfaceView` is not reliable for normal View transforms.

The repo already contains a TextureView layout:

- `activity_player_textureview.xml`
- `app:surface_type="texture_view"`

Decision: use TextureView as the normal precision-viewer rendering path. `activity_player.xml` is updated to request `texture_view` too, so transforms are applied to the decoded video surface rather than only controls or overlays.

## Supported Transforms

- Rotation: applied with `View.setRotation(...)` on the video surface.
- Horizontal flip: applied by negating `scaleX`.
- Vertical flip: applied by negating `scaleY`.
- Pinch zoom: keeps using the existing scale gesture flow, routed through the transform state.
- Pan: represented in transform state as `panX/panY` and applied through `translationX/Y`; gesture polish can expand after frame stepping is stable.
- Resize: keeps using Media3 `AspectRatioFrameLayout` constants for fit/crop behavior.

## Metadata Rotation

`metadataRotationDegrees` and `userRotationDegrees` are tracked separately in `VideoTransformState`. Media3 generally handles container rotation for playback, so v1 does not double-apply metadata rotation to the playback surface. The user transform is the manual display transform.

## Overlay

Frame bitmap overlay uses the same `VideoTransformState` so paused precision frames visually follow playback transforms.
