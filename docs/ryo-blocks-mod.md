# Ryo Blocks Mod

Private Fabric mod that force-enables a bundled resource pack tinting vanilla Minecraft 1.20.1 blocks with the supplied Ryo image. The product jar bundles its four required Fabric API modules; install Fabric Loader, then the one Ryo Blocks jar.

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

No custom entity or server behavior is registered. Vanilla and unmodded Fabric servers remain compatible, and dedicated servers do not need the mod.

## Bocchi Wolves

Wild, angry, and tamed wolves retain Minecraft 1.20.1's native model, UV layout, facial details, state differences, resolution, and transparency. Their white/grey fur shading is remapped to Bocchi's tracksuit pink, anchored at `#F6A9AF`, the dominant unshaded jacket fill sampled from the official TV anime character artwork. Dark eyes, nose, mouth, angry red eyes, and other protected details stay vanilla.

The default red tamed-wolf collar becomes a shaded Bocchi-pink band with one centered blue pixel (`#5092BD`) and one centered yellow pixel (`#B6973E`), sampled from the supplied hair-clip reference. The mod applies this as a client-only render substitution so Minecraft's red dye multiplication cannot destroy the accent colors. Dyeing the collar any non-red color restores the untouched vanilla collar texture and dye behavior.

Generate and validate the three fur textures with:

```powershell
python tools/generate_bocchi_wolf_assets.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
python tools/validate_bocchi_wolf_assets.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
```

## Kita Lava

Lava and flowing lava retain Minecraft's native animated fluid behavior: lighting, damage, flow, collision, particles, and sounds. Their two native atlas sprites carry otherwise-unused alpha markers so the terrain shaders can identify lava without hardcoding unstable atlas coordinates. On those marked pixels, the Ryo overlay is skipped and the accepted transparent Kita portrait from `source/kita-lava-cutout.png` is composited at 80% strength over the moving lava, leaving 20% of the native animated lava visible. The portrait remains sharp because it is sampled from one high-resolution texture rather than baked into Minecraft's 16x16 lava frames.

`source/kita-lava.png` is the exact user-supplied frame. `source/kita-lava-chroma.png` is the accepted background-replacement output and `source/kita-lava-cutout.png` is its locally background-removed derivative. The packaged portrait must remain byte-identical to that accepted cutout.

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

The generator keeps `source/ryo.png` readable for the Ryo item/HUD assets. Block tinting is rendered by jar-contained Minecraft 1.20.1 core terrain shader overrides using the transparent character cutout at `source/ryo-block-overlay.png`: Minecraft keeps every native block texture in its normal atlas, while the shader samples one shared Ryo texture. Character pixels blend at 50% strength; transparent background pixels leave the native material fully visible. This retains wood grain, ore flecks, stone pattern, and similar material cues without allocating a high-resolution texture for every block. Animated and transparent blocks retain their native frame behavior and alpha shapes. Kita lava is the intentional lava-only exception described above. The important proof path is Minecraft's own framebuffer screenshots, because external desktop/window capture can show a false white OpenGL surface on this machine.

The shader samples Minecraft's untouched native atlas, so doors, trapdoors, glass, stained glass, panes, plants, rails, and similar cutout/translucent assets retain their native silhouettes, shading, and animation behavior. Opaque doors, fences, fence gates, walls, and other shaped blocks retain their original models and texture cues while receiving the shared Ryo overlay at render time.

Clear full glass is the intentional exception: `glass.png` has no opaque alpha frame and uses a uniform alpha of 56/255 across the Ryo image. Vanilla glass's original light/dark pixel structure is retained as color shading blended into the Ryo colors, so the material still reads as Minecraft glass without becoming a framed window. Stained glass and panes retain their vanilla alpha structures.

## Generate Textures

```powershell
python tools/generate_ryo_textures.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar" --source-image source\ryo.png --target-size 512
python tools/validate_ryo_textures.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
```

Validation rejects block texture overrides other than the two byte-checked Kita lava markers, model redirects, missing item/HUD assets, altered item silhouettes, missing core shader overrides, or missing shared Ryo/Kita terrain textures.

## Low-Memory Block Tint

The built-in resource pack overrides Minecraft 1.20.1's terrain core shaders for solid, cutout, cutout-mipped, and translucent block render types. Each pass samples `Sampler0` (Minecraft's native block atlas) and `RyoSampler` (one bundled Ryo overlay texture) on the GPU. No Iris shaderpack, Sodium, OptiFine, or high-resolution per-block asset is required.

## Shared Terrain Submission

On OpenGL 3.2 or newer, solid, cutout-mipped, and cutout chunk uploads are also mirrored into persistent layer-owned vertex-buffer arenas. Complete visible runs that share an arena and a precision-safe 256-block coordinate page are submitted with core `glMultiDrawElementsBaseVertex`. Runs remain in Minecraft's original visible-section order, and their page-relative vertex position plus page camera offset is algebraically identical to Minecraft's section-local position plus section camera offset.

Minecraft's own per-section buffers remain alive and authoritative. A whole layer uses the vanilla render path whenever OpenGL 3.2 is unavailable, an upload is pending or incompatible, a visible section lacks a committed mirror, or the batch cache is otherwise incomplete. Translucent and tripwire layers always use vanilla rendering so their camera sorting and fabulous-transparency behavior are unchanged. World changes, renderer reloads, chunk-origin changes, and renderer shutdown invalidate or release the shared storage. The batch path does not change shaders, fog, lighting, frustum results, chunk rebuild cadence, draw distance, block content, or visual fidelity.

`WorldRenderer.updateChunks` also memoizes its lighting-enabled query by X/Z column and the owning `LightingProvider` generation. Minecraft's block and sky light storage both convert the supplied section to `ChunkSectionPos.withZeroY` before consulting their private `enabledColumns` sets. Exact 1.20.1 source inspection confirms those sets mutate only through `LightStorage.setColumnEnabled`; the client provider's enable/disable and propagation entry points increment a volatile generation after their storage mutations return. A provider or generation change invalidates all cached columns before the next query, which then calls the unchanged `LightingProvider.isLightingEnabled` and stores that exact boolean. World changes and renderer reload/close also reset the provider identity and cache. This owns no lighting state and changes no rebuild decision, scheduling, or light invalidation behavior.

## Build

Use JDK 17.

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.6.10-hotspot"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat build
```

The mod jar is written to:

```text
build/libs/ryo-blocks-1.2.0.jar
```

## Install

Install Fabric Loader for Minecraft 1.20.1, then copy the built Ryo Blocks jar into the Minecraft `mods` folder. Do not add a separate Fabric API jar for normal play: the product jar bundles exactly the Fabric API modules it needs. The mod registers its bundled Ryo resource pack as always enabled.

This project bundles a user-supplied character image and generated derivatives. Keep it private unless you have redistribution rights.
