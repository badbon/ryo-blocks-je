# Final Angel Style Verification - 2026-09-20

Finishing pass after `9db8002`. References and design decisions are in `docs/angel-boss-variants.md`.

## Changes

- Removed the chip's custom item-display override. It now inherits vanilla inventory, hand and ground transforms.
- Matched Minecraft 1.20.1's `FlyingItemEntityRenderer` orientation and normal scale. Charged chips retain a readable 1.35x size. Reused one render-only ItemStack rather than allocating one every rendered frame.
- Preserved the exact skins, approved wings, gold halo, hover and all combat AI.
- Added hard-alpha, limited-palette and native-model-transform build guards, plus three-quarter/side/back proof views for each boss.

## Validation

- `gradlew.bat --no-daemon --no-parallel build`: passed, including packaged-asset, mixin, timing and common-entrypoint checks.
- `gradlew.bat --no-daemon --no-parallel runClient -PbossAbilityProof -PbossVariantsProof`: passed at 12:44:19 local time, `VARIANT CHECKS 12/12 frames=960/960 styleViews=6/6`.
- Nijika combat: 31 observed shots and target damage. Kikuri: 30 actual potion shots with status effects and damage; no skulls.
- Nijika wall escape: 24 clearing shots, final X=14.49. Kikuri: 36 clearing bottles, final X=13.77. Bedrock and mobGriefing=false cases produced no clearing shots or escape.
- Kita and ordinary Wither controls passed with 31 and 21 observed shots respectively. Spawn-egg type, health and entity NBT round-trip checks passed.
- Eight character screenshots and the two projectile close-ups inspected. Shoulder attachment, matching sleeve transforms, original faces, readable wing outline and halo checked from multiple angles. The side view naturally presents a narrow wing profile.
- `combat.mp4`: fresh Nijika then Kikuri AI combat, 12 seconds each, H.264, 1280x720, 20 fps, 480 frames, no audio stream. Decoded contact sheet inspected. Original framebuffers are not retouched; Ryo floor imagery belongs to the existing terrain mod, not the wings.
- Speaker device startup was cancelled by the proof audio mixin; no OpenAL/sound-engine startup occurred. No visible game window was launched.

## Installation

Installed JAR: `C:/Users/Go4No/AppData/Roaming/.minecraft/ryo-blocks/mods/ryo-blocks-1.3.0.jar`.

SHA256: `1D818BA1D9A83F7C3CCEFC4ABA913FF5D8E519EC3E48F8E323B128655CE6D5D7`.

Previous JAR backed up under `C:/Users/Go4No/Documents/Codex/ryo-blocks/install-backups/20260920-angel-style-final/`. Source/installed hashes matched. No game was running during replacement. Other mods, settings and worlds were not changed.

Proof is from the hidden development client, not an actual-profile launch or an exhaustive third-party shader compatibility test. The requested features are implemented; this pass does not imply default-branch integration.
