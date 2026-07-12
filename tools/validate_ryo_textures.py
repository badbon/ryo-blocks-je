from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageChops

from generate_ryo_textures import build_clear_glass, build_frame, CLEAR_GLASS_ALPHA, EARLY_SURVIVAL_ITEMS, GUI_PREFIX, HUD_TEXTURES, ITEM_PREFIX, SHIELD_TEXTURES


BLOCK_PREFIX = "assets/minecraft/textures/block/"


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate generated Ryo block texture coverage.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--resources-dir", default=Path("src/main/resources/resourcepacks/ryo_blocks"), type=Path)
    args = parser.parse_args()

    generated_root = args.resources_dir / "assets" / "minecraft" / "textures" / "block"
    failures: list[str] = []
    checked = 0
    animated = 0
    transparent = 0
    static_opaque = 0
    checked_items = 0
    checked_hud = 0
    checked_shields = 0

    with zipfile.ZipFile(args.minecraft_jar) as jar:
        names = set(jar.namelist())
        texture_names = sorted(
            name
            for name in names
            if name.startswith(BLOCK_PREFIX) and name.endswith(".png")
        )

        for name in texture_names:
            relative = name.removeprefix(BLOCK_PREFIX)
            with jar.open(name) as raw:
                vanilla = Image.open(raw).convert("RGBA")
                vanilla.load()

            has_mcmeta = f"{name}.mcmeta" in names
            is_opaque = not has_mcmeta and vanilla.getchannel("A").getextrema() == (255, 255)
            generated_path = generated_root / relative
            if is_opaque:
                static_opaque += 1

            if not generated_path.exists():
                failures.append(f"missing generated texture: {relative}")
                continue

            generated = Image.open(generated_path).convert("RGBA")
            generated.load()
            tile = Image.open(generated_root / "ryo.png").convert("RGBA")
            if relative == "glass.png":
                expected = build_clear_glass(tile, vanilla, generated.width)
            elif has_mcmeta:
                frame_size = vanilla.width
                frames = [
                    build_frame(tile, vanilla.crop((0, index * frame_size, vanilla.width, (index + 1) * frame_size)), generated.width)
                    for index in range(vanilla.height // frame_size)
                ]
                expected = Image.new("RGBA", (generated.width, generated.width * len(frames)))
                for index, frame in enumerate(frames):
                    expected.alpha_composite(frame, (0, index * generated.width))
            else:
                expected = build_frame(tile, vanilla, generated.width)
            if ImageChops.difference(expected, generated).getbbox():
                failures.append(f"vanilla/Ryo blend mismatch: {relative}")
            if has_mcmeta:
                animated += 1
                if not generated_path.with_suffix(generated_path.suffix + ".mcmeta").exists():
                    failures.append(f"missing animation metadata: {relative}.mcmeta")
                vanilla_frames = vanilla.height // vanilla.width if vanilla.width else 1
                generated_frames = generated.height // generated.width if generated.width else 1
                if vanilla_frames != generated_frames:
                    failures.append(f"frame count mismatch: {relative} vanilla={vanilla_frames} generated={generated_frames}")

            vanilla_alpha = vanilla.getchannel("A").getextrema()
            generated_alpha = generated.getchannel("A").getextrema()
            if vanilla_alpha != (255, 255):
                transparent += 1
                if generated_alpha == (255, 255):
                    failures.append(f"lost transparency: {relative}")
                if relative == "glass.png":
                    if generated_alpha != (CLEAR_GLASS_ALPHA, CLEAR_GLASS_ALPHA):
                        failures.append(f"clear glass is not uniformly tinted: {generated_alpha}")
                    expected_glass = build_clear_glass(tile, vanilla, generated.width)
                    if ImageChops.difference(expected_glass, generated).getbbox():
                        failures.append("clear glass lost vanilla color shading")
                else:
                    expected_alpha = vanilla.getchannel("A").resize(
                        generated.size,
                        Image.Resampling.NEAREST,
                    )
                    if ImageChops.difference(expected_alpha, generated.getchannel("A")).getbbox():
                        failures.append(f"transparent silhouette changed: {relative}")

            checked += 1

        shared_path = generated_root / "ryo.png"
        if not shared_path.exists():
            failures.append("missing shared opaque texture: ryo.png")
        else:
            shared = Image.open(shared_path).convert("RGBA")
            if shared.size[0] != shared.size[1]:
                failures.append("shared opaque texture is not square")
            shared_alpha = shared.getchannel("A")
            if shared_alpha.getextrema() != (255, 255):
                failures.append("shared opaque texture contains transparent pixels")
            width, height = shared.size
            edge_alpha = (
                shared_alpha.crop((0, 0, width, 1)),
                shared_alpha.crop((0, height - 1, width, height)),
                shared_alpha.crop((0, 0, 1, height)),
                shared_alpha.crop((width - 1, 0, width, height)),
            )
            if any(edge.getextrema() != (255, 255) for edge in edge_alpha):
                failures.append("shared opaque texture has a transparent edge")

        model_root = args.resources_dir / "assets" / "minecraft" / "models"
        if any(model_root.rglob("*.json")):
            failures.append("legacy shared-texture model redirects remain")

        item_root = args.resources_dir / "assets" / "minecraft" / "textures" / "item"
        item_hashes: set[bytes] = set()
        for relative in sorted(EARLY_SURVIVAL_ITEMS):
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
            item_hashes.add(generated.tobytes())
            checked_items += 1
        if len(item_hashes) < checked_items // 2:
            failures.append("too many early-survival item textures are pixel-identical")

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
        "checked_png": checked,
        "animated_with_mcmeta": animated,
        "transparent_vanilla_textures": transparent,
        "static_opaque_textures": static_opaque,
        "model_redirects": 0,
        "checked_early_survival_items": checked_items,
        "checked_hud_textures": checked_hud,
        "checked_shield_textures": checked_shields,
        "failures": failures,
    }
    print(json.dumps(result, indent=2))
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
