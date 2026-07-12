from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageChops

from generate_ryo_textures import EARLY_SURVIVAL_ITEMS, GUI_PREFIX, HUD_TEXTURES, ITEM_PREFIX, SHIELD_TEXTURES


BLOCK_PREFIX = "assets/minecraft/textures/block/"


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate generated Ryo block texture coverage.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--resources-dir", default=Path("src/main/resources/resourcepacks/ryo_blocks"), type=Path)
    args = parser.parse_args()

    generated_root = args.resources_dir / "assets" / "minecraft" / "textures" / "block"
    failures: list[str] = []
    vanilla_block_textures = 0
    checked_items = 0
    checked_hud = 0
    checked_shields = 0

    with zipfile.ZipFile(args.minecraft_jar) as jar:
        names = set(jar.namelist())
        vanilla_block_textures = sum(
            name.startswith(BLOCK_PREFIX) and name.endswith(".png")
            for name in names
        )
        if any(generated_root.rglob("*.png")):
            failures.append("block texture overrides remain; Iris must sample vanilla's native atlas")

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
        "vanilla_block_textures": vanilla_block_textures,
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
