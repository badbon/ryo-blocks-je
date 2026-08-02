from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageChops

from generate_ryo_textures import (
    GUI_PREFIX,
    HUD_TEXTURES,
    ITEM_PREFIX,
    ITEM_TEMPLATE_TEXTURES,
    KITA_LAVA_RESOURCE,
    KITA_LAVA_TEXTURES,
    SHIELD_TEXTURES,
    is_high_risk_item_texture,
    ryo_overlay_texture,
    shader_overlay_source,
    vanilla_item_texture_names,
)


BLOCK_PREFIX = "assets/minecraft/textures/block/"
CORE_SHADER_PREFIX = Path("assets/minecraft/shaders/core")
CORE_SHADER_STEMS = (
    "rendertype_solid",
    "rendertype_cutout",
    "rendertype_cutout_mipped",
    "rendertype_translucent",
)


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate generated Ryo block texture coverage.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--resources-dir", default=Path("src/main/resources/resourcepacks/ryo_blocks"), type=Path)
    parser.add_argument("--item-overlay-source", default=Path("source/ryo-block-overlay.png"), type=Path)
    parser.add_argument("--kita-lava-source", default=Path("source/kita-lava-cutout.png"), type=Path)
    args = parser.parse_args()

    generated_root = args.resources_dir / "assets" / "minecraft" / "textures" / "block"
    failures: list[str] = []
    vanilla_block_textures = 0
    checked_items = 0
    checked_high_risk_items = 0
    checked_hud = 0
    checked_shields = 0
    checked_kita_lava = 0
    if not args.item_overlay_source.exists():
        failures.append(f"missing item overlay source: {args.item_overlay_source}")
        item_overlay_tile = None
    else:
        item_overlay_tile = shader_overlay_source(args.item_overlay_source, 512)

    with zipfile.ZipFile(args.minecraft_jar) as jar:
        names = set(jar.namelist())
        vanilla_block_textures = sum(
            name.startswith(BLOCK_PREFIX) and name.endswith(".png")
            for name in names
        )
        generated_block_files = {
            path.relative_to(generated_root).as_posix()
            for path in generated_root.rglob("*.png")
        }
        expected_block_files = set(KITA_LAVA_TEXTURES)
        if generated_block_files != expected_block_files:
            failures.append(
                "block texture overrides must contain only Kita's marked lava sprites: "
                f"expected={sorted(expected_block_files)} actual={sorted(generated_block_files)}"
            )
        for relative, marker_alpha in KITA_LAVA_TEXTURES.items():
            name = f"{BLOCK_PREFIX}{relative}"
            generated_path = generated_root / relative
            if not generated_path.exists():
                continue
            with jar.open(name) as raw:
                vanilla = Image.open(raw).convert("RGBA")
                vanilla.load()
            generated = Image.open(generated_path).convert("RGBA")
            generated.load()
            if generated.size != vanilla.size:
                failures.append(f"Kita lava dimensions changed: {relative}")
            if ImageChops.difference(generated.convert("RGB"), vanilla.convert("RGB")).getbbox():
                failures.append(f"Kita lava changed vanilla animation colors: {relative}")
            if generated.getchannel("A").getextrema() != (marker_alpha, marker_alpha):
                failures.append(f"Kita lava marker alpha is wrong: {relative}")
            metadata_name = f"{name}.mcmeta"
            metadata_path = generated_root / f"{relative}.mcmeta"
            if not metadata_path.exists() or metadata_path.read_bytes() != jar.read(metadata_name):
                failures.append(f"Kita lava animation metadata changed: {relative}")
            checked_kita_lava += 1

        model_root = args.resources_dir / "assets" / "minecraft" / "models"
        if any(model_root.rglob("*.json")):
            failures.append("legacy shared-texture model redirects remain")

        shader_root = args.resources_dir / CORE_SHADER_PREFIX
        for stem in CORE_SHADER_STEMS:
            for suffix in (".json", ".vsh", ".fsh"):
                shader_path = shader_root / f"{stem}{suffix}"
                if not shader_path.exists():
                    failures.append(f"missing core shader override: {stem}{suffix}")
                    continue
                text = shader_path.read_text(encoding="utf-8")
                if suffix == ".json":
                    for sampler in ("RyoSampler", "KitaSampler"):
                        if sampler not in text:
                            failures.append(f"core shader JSON does not declare {sampler}: {stem}{suffix}")
                if suffix == ".fsh":
                    for required in (
                        "RyoSampler",
                        "KitaSampler",
                        "RYO_OVERLAY_STRENGTH = 0.50",
                        "VANILLA_SPRITE_SIZE = 16.0",
                        "KITA_LAVA_OVERLAY_STRENGTH = 0.80",
                        "KITA_STILL_MARKER_ALPHA = 254.0 / 255.0",
                        "KITA_FLOW_MARKER_ALPHA = 253.0 / 255.0",
                        "KITA_FACE_WORLD_SCALE = 1.0",
                    ):
                        if required not in text:
                            failures.append(f"core shader fragment is missing {required}: {stem}{suffix}")

        overlay_path = args.resources_dir / "assets" / "ryo-blocks" / "textures" / "terrain" / "ryo_overlay.png"
        if not overlay_path.exists():
            failures.append("missing shared terrain overlay texture")

        kita_path = args.resources_dir / KITA_LAVA_RESOURCE
        if not args.kita_lava_source.exists():
            failures.append(f"missing Kita lava source: {args.kita_lava_source}")
        elif not kita_path.exists():
            failures.append("missing packaged Kita lava portrait")
        elif kita_path.read_bytes() != args.kita_lava_source.read_bytes():
            failures.append("packaged Kita lava portrait differs from its accepted source")

        item_root = args.resources_dir / "assets" / "minecraft" / "textures" / "item"
        item_hashes: set[bytes] = set()
        for relative in vanilla_item_texture_names(names):
            name = f"{ITEM_PREFIX}{relative}"
            generated_path = item_root / relative
            if name not in names:
                failures.append(f"required vanilla item does not exist: {relative}")
                continue
            if not generated_path.exists():
                failures.append(f"missing generated item: {relative}")
                continue
            with jar.open(name) as raw:
                vanilla = Image.open(raw).convert("RGBA")
                vanilla.load()
            generated = Image.open(generated_path).convert("RGBA")
            generated.load()
            if generated.size != vanilla.size:
                failures.append(f"item dimensions changed: {relative} {vanilla.size} -> {generated.size}")
            if ImageChops.difference(vanilla.getchannel("A"), generated.getchannel("A")).getbbox():
                failures.append(f"item silhouette changed: {relative}")
            if generated.getbbox() is None:
                failures.append(f"generated item is blank: {relative}")
            if ImageChops.difference(vanilla.convert("RGB"), generated.convert("RGB")).getbbox() is None:
                failures.append(f"generated item has no Ryo overlay: {relative}")
            if item_overlay_tile is not None:
                expected = ryo_overlay_texture(
                    item_overlay_tile,
                    vanilla,
                    0.20 if is_high_risk_item_texture(relative) else 0.50,
                )
                if ImageChops.difference(expected, generated).getbbox():
                    failures.append(f"item overlay strength/composition mismatch: {relative}")
            metadata = f"{name}.mcmeta"
            generated_metadata = item_root / f"{relative}.mcmeta"
            if metadata in names and not generated_metadata.exists():
                failures.append(f"missing item animation metadata: {relative}")
            if metadata not in names and generated_metadata.exists():
                failures.append(f"unexpected item animation metadata: {relative}")
            item_hashes.add(generated.tobytes())
            checked_items += 1
            checked_high_risk_items += is_high_risk_item_texture(relative)
        if len(item_hashes) < checked_items // 2:
            failures.append("too many item textures are pixel-identical")
        for relative in ITEM_TEMPLATE_TEXTURES:
            if (item_root / relative).exists():
                failures.append(f"UI/template texture should remain vanilla: {relative}")

        gui_root = args.resources_dir / "assets" / "minecraft" / "textures" / "gui"
        for relative in (*HUD_TEXTURES, "widgets.png"):
            name = f"{GUI_PREFIX}{relative}"
            path = gui_root / relative
            if not path.exists():
                failures.append(f"missing generated HUD texture: {relative}")
                continue
            with jar.open(name) as raw:
                vanilla = Image.open(raw).convert("RGBA")
                vanilla.load()
            generated = Image.open(path).convert("RGBA")
            generated.load()
            if generated.size != vanilla.size:
                failures.append(f"HUD dimensions changed: {relative}")
            if relative == "icons.png":
                if ImageChops.difference(vanilla.getchannel("A"), generated.getchannel("A")).getbbox():
                    failures.append("HUD icon state silhouettes changed")
            else:
                difference = ImageChops.difference(vanilla, generated)
                allowed = Image.new("L", vanilla.size, 0)
                for box in ((0, 0, 182, 22), (0, 22, 53, 46)):
                    allowed.paste(255, box)
                outside = ImageChops.multiply(difference.convert("RGB"), Image.eval(allowed, lambda value: 255 - value).convert("RGB"))
                if outside.getbbox():
                    failures.append("widgets.png changed outside approved HUD regions")
            checked_hud += 1

        entity_root = args.resources_dir / "assets" / "minecraft" / "textures" / "entity"
        for relative in SHIELD_TEXTURES:
            name = f"assets/minecraft/textures/entity/{relative}"
            path = entity_root / relative
            if not path.exists():
                failures.append(f"missing generated shield texture: {relative}")
                continue
            with jar.open(name) as raw:
                vanilla = Image.open(raw).convert("RGBA")
                vanilla.load()
            generated = Image.open(path).convert("RGBA")
            generated.load()
            if generated.size != vanilla.size:
                failures.append(f"shield dimensions changed: {relative}")
            if ImageChops.difference(vanilla.getchannel("A"), generated.getchannel("A")).getbbox():
                failures.append(f"shield silhouette changed: {relative}")
            checked_shields += 1

    result = {
        "vanilla_block_textures": vanilla_block_textures,
        "model_redirects": 0,
        "checked_item_textures": checked_items,
        "checked_high_risk_item_textures": checked_high_risk_items,
        "checked_hud_textures": checked_hud,
        "checked_shield_textures": checked_shields,
        "checked_kita_lava_textures": checked_kita_lava,
        "failures": failures,
    }
    print(json.dumps(result, indent=2))
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
