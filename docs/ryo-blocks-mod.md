# Ryo Blocks Mod

Private Fabric mod that force-enables a bundled resource pack overriding vanilla Minecraft 1.20.1 block textures with the supplied Ryo image. Install Fabric API alongside the jar.

The first inventory pass also themes the survival HUD and common early-game item textures. Items keep their vanilla 16x16 alpha silhouettes and light/dark edge structure, with the same Ryo source image sampled inside those silhouettes. This keeps tools, food, armor, buckets, and resources recognizable without adding custom items, names, recipes, menus, commands, or world data. HUD icons preserve their vanilla state silhouettes and dimensions; only the hotbar regions of `widgets.png` are changed, so menu buttons and container screens remain vanilla.

## Forced Player Skin

While the mod is active, every rendered player uses the bundled 64x64 Ryo skin with the slim-arm model. The client override covers in-world players, the local inventory preview, and player-list faces. It does not change the player's Mojang/Microsoft account skin, write to Minecraft's skin cache, modify profiles or UUIDs, send skin data to servers, replace capes, or affect how the player appears when the mod is removed. The source asset lives at `source/ryo-player-skin.png`; its packaged runtime copy lives under the mod namespace at `assets/ryo-blocks/textures/entity/player/ryo.png`.

The production mixin configuration must declare `client-ryo-blocks-refmap.json`. Development mappings resolve named injection targets without it, but an installed obfuscated Minecraft client cannot; omitting the declaration causes a startup crash before the title screen.

## Nijika Villagers And Chests

The bundled Nijika skin is rendered on a slim player model for villagers and wandering traders. This is client-only: professions, trades, AI, hitboxes, sounds, and world data remain vanilla. Zombie villagers are intentionally not replaced so hostile mobs remain immediately readable.

All vanilla chest variants use generated Nijika chest textures. The chest generator preserves each exact vanilla UV layout, transparency edge, and latch. It applies a clean Nijika-toned material treatment, then places one upright Nijika face only on the rendered front body panel. Single chests and each double-chest half use their own verified UV coordinates, so the face never lands on a lid, underside, or overlapping side panel. Generate the assets with:

```powershell
python tools/generate_nijika_assets.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
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

The generator keeps the source readable by creating 512x512 block textures instead of shrinking the image to vanilla 16x16. It removes a near-solid black export strip at the source image's bottom edge, then center-crops the non-square art to a fully opaque square instead of surrounding it with transparent letterboxing. This prevents both sky-colored gaps and a false black seam between solid blocks. Static opaque blocks share that one 512x512 Ryo texture through generated vanilla-model redirects, so Minecraft does not allocate hundreds of identical atlas entries. Transparent and animated textures remain separate 512px files: plants, panes, rails, dust, overlays, and animated materials keep their vanilla cutout shapes and frame behavior. The important proof path is Minecraft's own framebuffer screenshots, because external desktop/window capture can show a false white OpenGL surface on this machine.

Transparent block masks use nearest-neighbor scaling, preserving every vanilla alpha value without filtered fringe pixels. Doors, trapdoors, glass, stained glass, panes, plants, rails, and similar cutout/translucent assets therefore show Ryo inside their exact native silhouettes. Opaque doors, fences, fence gates, walls, and other shaped blocks use Minecraft's original model geometry while their face references redirect to the shared Ryo tile.

Clear full glass is the intentional exception: `glass.png` has no opaque alpha frame and uses a uniform alpha of 56/255 across the Ryo image. Vanilla glass's original light/dark pixel structure is retained as color shading blended into the Ryo colors, so the material still reads as Minecraft glass without becoming a framed window. Stained glass and panes retain their vanilla alpha structures.

## Generate Textures

```powershell
python tools/generate_ryo_textures.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar" --source-image source\ryo.png --target-size 512
python tools/validate_ryo_textures.py --minecraft-jar "$env:APPDATA\.minecraft\versions\1.20.1\1.20.1.jar"
```

Validation rejects missing coverage, altered item/HUD dimensions, changed alpha silhouettes, blank assets, excessive duplicate item output, and edits outside the approved hotbar regions.

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
