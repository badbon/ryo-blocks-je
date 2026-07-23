from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageChops

from generate_bocchi_wolf_assets import (
    BOCCHI_CLIP_BLUE,
    BOCCHI_CLIP_YELLOW,
    BOCCHI_COLLAR_TEXTURE,
    BOCCHI_TRACKSUIT_PINK,
    COLLAR_BLUE_PIXEL,
    COLLAR_YELLOW_PIXEL,
    VANILLA_COLLAR_TEXTURE,
    WOLF_PREFIX,
    WOLF_TEXTURES,
    is_fur_pixel,
    recolor_collar,
    recolor_fur,
)


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate Bocchi-pink vanilla wolf textures.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument(
        "--resources-dir",
        default=Path("src/main/resources/resourcepacks/ryo_blocks"),
        type=Path,
    )
    parser.add_argument(
        "--mod-resources-dir",
        default=Path("src/main/resources"),
        type=Path,
    )
    args = parser.parse_args()

    failures: list[str] = []
    checked_textures = 0
    checked_fur_pixels = 0
    exact_anchor_pixels = 0
    themed_fur_shades: set[tuple[int, int, int]] = set()
    wolf_root = args.resources_dir / WOLF_PREFIX

    expected_files = set(WOLF_TEXTURES)
    actual_files = {path.name for path in wolf_root.glob("*.png")} if wolf_root.exists() else set()
    if actual_files != expected_files:
        failures.append(
            f"wolf override set mismatch: expected {sorted(expected_files)}, got {sorted(actual_files)}"
        )
    if (wolf_root / "wolf_collar.png").exists():
        failures.append("vanilla wolf collar override would break non-red dye colors")

    with zipfile.ZipFile(args.minecraft_jar) as jar:
        for texture_name in WOLF_TEXTURES:
            generated_path = wolf_root / texture_name
            if not generated_path.exists():
                continue

            with jar.open(f"{WOLF_PREFIX}{texture_name}") as raw:
                vanilla = Image.open(raw).convert("RGBA")
                vanilla.load()
            generated = Image.open(generated_path).convert("RGBA")
            generated.load()
            expected = recolor_fur(vanilla)

            if generated.size != vanilla.size:
                failures.append(f"wolf dimensions changed: {texture_name}")
            if ImageChops.difference(vanilla.getchannel("A"), generated.getchannel("A")).getbbox():
                failures.append(f"wolf UV silhouette changed: {texture_name}")
            if ImageChops.difference(expected, generated).getbbox():
                failures.append(f"wolf recolor is not deterministic: {texture_name}")

            for vanilla_pixel, generated_pixel in zip(vanilla.getdata(), generated.getdata()):
                if is_fur_pixel(vanilla_pixel):
                    checked_fur_pixels += 1
                    exact_anchor_pixels += generated_pixel[:3] == BOCCHI_TRACKSUIT_PINK
                    themed_fur_shades.add(generated_pixel[:3])
                elif generated_pixel != vanilla_pixel:
                    failures.append(f"protected facial/detail pixel changed: {texture_name}")
                    break
            checked_textures += 1

        with jar.open(VANILLA_COLLAR_TEXTURE) as raw:
            vanilla_collar = Image.open(raw).convert("RGBA")
            vanilla_collar.load()

    collar_path = args.mod_resources_dir / BOCCHI_COLLAR_TEXTURE
    if not collar_path.exists():
        failures.append(f"missing Bocchi collar texture: {collar_path}")
        generated_collar = None
    else:
        generated_collar = Image.open(collar_path).convert("RGBA")
        generated_collar.load()
        expected_collar = recolor_collar(vanilla_collar)

        if generated_collar.size != vanilla_collar.size:
            failures.append("Bocchi collar dimensions changed")
        if ImageChops.difference(
            vanilla_collar.getchannel("A"), generated_collar.getchannel("A")
        ).getbbox():
            failures.append("Bocchi collar UV silhouette changed")
        if ImageChops.difference(expected_collar, generated_collar).getbbox():
            failures.append("Bocchi collar generation is not deterministic")
        if generated_collar.getpixel(COLLAR_BLUE_PIXEL)[:3] != BOCCHI_CLIP_BLUE:
            failures.append("Bocchi blue clip pixel is missing or has the wrong color")
        if generated_collar.getpixel(COLLAR_YELLOW_PIXEL)[:3] != BOCCHI_CLIP_YELLOW:
            failures.append("Bocchi yellow clip pixel is missing or has the wrong color")

    if checked_fur_pixels < 2_000:
        failures.append(f"too few fur pixels were recolored: {checked_fur_pixels}")
    if exact_anchor_pixels < 300:
        failures.append(f"Bocchi base pink is not established strongly enough: {exact_anchor_pixels}")
    if len(themed_fur_shades) < 10:
        failures.append(f"too few distinct pink fur shades remain: {len(themed_fur_shades)}")

    result = {
        "bocchi_tracksuit_pink": "#F6A9AF",
        "checked_wolf_textures": checked_textures,
        "checked_fur_pixels": checked_fur_pixels,
        "exact_base_pink_pixels": exact_anchor_pixels,
        "distinct_pink_fur_shades": len(themed_fur_shades),
        "bocchi_collar_checked": generated_collar is not None,
        "bocchi_clip_blue": "#5092BD",
        "bocchi_clip_yellow": "#B6973E",
        "other_collar_dyes_preserved": not (wolf_root / "wolf_collar.png").exists(),
        "failures": failures,
    }
    print(json.dumps(result, indent=2))
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
