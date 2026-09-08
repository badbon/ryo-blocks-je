# Kita feather combat video

- Artifact: `kita-feather-combat-20260909.mp4`, 15 seconds, 1280x720, 20 fps, silent H.264.
- Gameplay visuals: commit `74b1446`; this change adds the repeatable capture mode only.
- Command: Java 17, `gradlew.bat --no-daemon runClient -PbossAbilityProof -PbossCombatVideo`.
- Hidden, speaker-muted client; 300 consecutive client-tick framebuffer captures encoded with FFmpeg at 20 fps. No interpolated or generated frames.
- Normal spawn-egg boss and normal combat AI. Test target is an AI-disabled iron golem with 1000 health. Camera follows their midpoint; HUD hidden. Disposable elevated test platform, not a survival encounter.
- Observed repeated feather projectiles and impacts, target damage flashes, and moving wings. End observation: boss health 300, invulnerability timer 0, projectile present, target Wither effect active.
- Validation: runClient compiled and completed successfully; all 300 captures saved; encoded video decoded for a 15-frame overview inspection. FFprobe verified 300 frames and 15-second duration, with no audio stream.
