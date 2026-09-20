# Nijika and Kikuri Angel Boss Proof - 2026-09-20

## Build and Runtime

- Java 17, Minecraft 1.20.1, Fabric Loader 0.19.3.
- `gradlew.bat --no-daemon build runClient -PbossAbilityProof -PbossVariantsProof` succeeded.
- Packaged assets, original Kikuri skin bytes, slim-arm layouts, names, spawn-egg models, common projectile mixin refmap, existing timing contracts, common entrypoint and bundled Fabric modules passed build checks.
- Final hidden-client run: `VARIANT CHECKS 12/12 frames=960/960` at 12:32:33 local time.
- Audio guard logged `Proof audio device disabled before startup`; no OpenAL or sound-engine startup. No visible game window was opened.
- Each angel was spawned through its actual spawn egg. Type, 300 max health, zero initial invulnerability timer and entity NBT round-trip were checked.

| Observation | Final result |
| --- | --- |
| Nijika / Kikuri portraits | Correct skins, slim arms, full-name boss bars, white articulated wings and gold halos; inspected screenshots |
| Nijika combat, 320 ticks | 31 observed chip shots; target damaged; no clearing shots in open space |
| Kikuri combat, 320 ticks | 29 observed potion shots; actual status effects and damage; poison, weakness and harming observed; no skulls |
| Nijika five-block-thick stone wall | Escaped to X=14.49, with 21 clearing shots |
| Kikuri same wall | Escaped to X=13.78, with 36 clearing bottles |
| Kikuri bedrock wall | No escape, zero clearing shots |
| Kikuri stone wall, mobGriefing=false | No escape, zero clearing shots |
| Kita control | 30 shots, target damaged, feather rendering retained |
| Ordinary Wither control | 20 shots, target damaged, original skull rendering retained |
| Chip and potion close-ups | One correctly owned projectile each; inspected actual framebuffer captures |

Shot counts are observations over these runs, not deterministic rate specifications. Cooldown constants are unchanged from Kita's previous build. Slowness selection is implemented from the vanilla Witch logic but this short combat run closed range before that branch was observed.

## Media

- `nijika-angel-20260920.png` and `kikuri-angel-20260920.png`: unedited 1280x720 framebuffer screenshots.
- `nijika-dorito-20260920.png` and `kikuri-potion-20260920.png`: staged stationary projectile close-ups, not claims of live combat frames.
- `angel-variants-combat-20260920.mp4`: actual AI combat and escape capture, H.264, 1280x720, 20 fps, 960 frames, 48 seconds, no audio stream. Decoded 16-frame contact sheet inspected.
- Video sequence: 0-12s Nijika combat; 12-24s Kikuri combat; 24-36s Nijika wall; 36-48s Kikuri wall. Fixed camera partly obscures passage through the opaque wall; server position and clearing-shot observations confirm escape.
- Disposable proof world only. A sea-lantern floor provides ordinary in-world lighting so the characters remain readable; no renderer brightness override. The ground retains the mod's existing Ryo terrain imagery, while the wings use the Minecraft snow texture.

## Installation Boundary

Installed tested JAR: `C:/Users/Go4No/AppData/Roaming/.minecraft/ryo-blocks/mods/ryo-blocks-1.3.0.jar`.

SHA256: `F9503AD32406B02BE48F81DC152B2D485629D88AAF56F2854CFD60816D509208`.

Previous build backed up under `C:/Users/Go4No/Documents/Codex/ryo-blocks/install-backups/20260920-angel-variants/`. Launcher profile and installed hash were verified. No worlds, settings or other mods changed. The actual profile was not launched, so these captures prove the development client, not compatibility with every installed mod/shader.
