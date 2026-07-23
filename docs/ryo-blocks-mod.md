# Ryo Blocks Mod

Private Fabric mod that force-enables a bundled resource pack overriding vanilla Minecraft 1.20.1 block textures with the supplied Ryo image. Install Fabric API alongside the jar.

All vanilla item textures are Ryo-themed while keeping their native dimensions, alpha silhouettes, animation metadata, and light/dark edge structure. The transparent Ryo character cutout blends over ordinary items at 50%; stateful, colour-coded, ambiguous-food, mining-resource, ore-drop, and template/pattern items use 20% coverage where appropriate. Dynamic UI layers such as empty slots, potion liquid overlays, spawn-egg overlays, leather colour overlays, and firework-star overlays remain vanilla so their gameplay cues stay readable. This adds no custom items, names, recipes, menus, commands, or world data. HUD icons preserve their vanilla state silhouettes and dimensions; only the hotbar regions of `widgets.png` are changed, so menu buttons and container screens remain vanilla.

## Forced Player Skin

While the mod is active, every rendered player uses the bundled 64x64 Ryo skin with the slim-arm model. The client override covers in-world players, the local inventory preview, and player-list faces. It does not change the player's Mojang/Microsoft account skin, write to Minecraft's skin cache, modify profiles or UUIDs, send skin data to servers, replace capes, or affect how the player appears when the mod is removed. The source asset lives at `source/ryo-player-skin.png`; its packaged runtime copy lives under the mod namespace at `assets/ryo-blocks/textures/entity/player/ryo.png`.

The production mixin configuration must declare `client-ryo-blocks-refmap.json`. Development mappings resolve named injection targets without it, but an installed obfuscated Minecraft client cannot; omitting the declaration causes a startup crash before the title screen.

## Nijika Villagers And Chests

The bundled Nijika skin is rendered on a slim player model for villagers and wandering traders. This is client-only: professions, trades, AI, hitboxes, sounds, and world data remain vanilla. Zombie villagers are intentionally not replaced so hostile mobs remain immediately readable.

All vanilla chest variants use generated Nijika chest textures. The chest generator preserves each exact vanilla UV layout, transparency edge, and latch. It applies a clean Nijika-toned material treatment, then places one upright Nijika face only on the rendered front body panel. Single chests and each double-chest half use their own verified UV coordinates, so the face never lands on a lid, underside, or overlapping side panel. Generate the assets with:

```powershell
python tools/generate_nijika_assets.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
```

## Pa-san Endermen

Every Enderman is rendered client-side as the supplied Pa-san slim-player model. Its AI, hitbox, teleporting, block pickup and placement, sounds, particles, aggression, and world data are still Minecraft's native Enderman behavior. The custom renderer preserves vanilla angry-camera jitter and the exact native carried-block transform; Pa-san's arms use the matching carrying pose, so a picked-up block sits in her raised hands rather than clipping through the torso. Standard biped combat movement remains active and the player model intentionally has no Enderman mouth or glowing-eye overlay. The source skin is `source/pa-san-player-skin.png`; its packaged copy is `assets/ryo-blocks/textures/entity/player/pa_san.png`.

## Bocchi Wolves

Wild, angry, and tamed wolves retain Minecraft 1.20.1's native model, UV layout, facial details, state differences, resolution, and transparency. Their white/grey fur shading is remapped to Bocchi's tracksuit pink, anchored at `#F6A9AF`, the dominant unshaded jacket fill sampled from the official TV anime character artwork. Dark eyes, nose, mouth, angry red eyes, and other protected details stay vanilla. `wolf_collar.png` is intentionally not overridden, so tamed-wolf collars remain independently dyeable.

Generate and validate the three fur textures with:

```powershell
python tools/generate_bocchi_wolf_assets.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
python tools/validate_bocchi_wolf_assets.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
```

## Ryo Edition Title

The main menu keeps Minecraft's original logo and replaces only the native `edition.png` subtitle directly under it with the approved generated title. Its transparent source is stored at `source/ryo-edition-title.png`; compose it into Minecraft's fixed 512x64 title-subtitle slot with:

```powershell
python tools/compose_ryo_edition_title.py
```

## Source Image

Place the source art at:

```text
source/ryo.png
```

The generator keeps `source/ryo.png` readable for the Ryo item/HUD assets. Block tinting is rendered separately by the bundled Iris shader pack using the transparent character cutout at `source/ryo-block-overlay.png`: Minecraft keeps every native block texture in its normal atlas, while the shader samples one shared 512x512 Ryo texture. Character pixels blend at 50% strength; transparent background pixels leave the native material fully visible. This retains wood grain, ore flecks, stone pattern, and similar material cues without allocating a 512px texture for every block. Animated and transparent blocks retain their native frame behavior and alpha shapes. The important proof path is Minecraft's own framebuffer screenshots, because external desktop/window capture can show a false white OpenGL surface on this machine.

The shader samples Minecraft's untouched native atlas, so doors, trapdoors, glass, stained glass, panes, plants, rails, and similar cutout/translucent assets retain their native silhouettes, shading, and animation behavior. Opaque doors, fences, fence gates, walls, and other shaped blocks retain their original models and texture cues while receiving the shared Ryo overlay at render time.

Clear full glass is the intentional exception: `glass.png` has no opaque alpha frame and uses a uniform alpha of 56/255 across the Ryo image. Vanilla glass's original light/dark pixel structure is retained as color shading blended into the Ryo colors, so the material still reads as Minecraft glass without becoming a framed window. Stained glass and panes retain their vanilla alpha structures.

## Generate Textures

```powershell
python tools/generate_ryo_textures.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar" --source-image source\ryo.png --target-size 512
python tools/validate_ryo_textures.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
python tools/build_ryo_shaderpack.py
python tools/validate_ryo_shaderpack.py
```

Validation rejects residual block texture overrides or model redirects, missing item/HUD assets, altered item silhouettes, and an invalid shader pack. The shader archive validator also proves it binds exactly one shared Ryo sampler from the authoritative source while retaining access to Minecraft's native block atlas.

## Low-Memory Block Tint

`source/ryo-vanilla-tint` contains a minimal Iris shader pack for Minecraft 1.20.1 with Sodium 0.5.13 and Indium 1.0.36. Its terrain pass samples `gtexture` (Minecraft's native block atlas) and `ryoTexture` (one generated 512px Ryo image) on the GPU. No high-resolution per-block asset is shipped in the Ryo resource pack. Build it with `tools/build_ryo_shaderpack.py`, then install the resulting `build/Ryo-Vanilla-Tint.zip` in the profile's `shaderpacks` folder and enable it through Iris.

## Build

Use JDK 17.

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.6.10-hotspot"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat build
```

The mod jar is written to:

```text
build/libs/ryo-blocks-1.0.0.jar
```

## Install

Install Fabric Loader and Fabric API for Minecraft 1.20.1, then copy the built jar into the Minecraft `mods` folder. The mod registers its bundled Ryo resource pack as always enabled.

This project bundles a user-supplied character image and generated derivatives. Keep it private unless you have redistribution rights.
