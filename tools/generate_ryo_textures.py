from __future__ import annotations

import argparse
import json
import shutil
import time
import zipfile
from pathlib import Path

from PIL import Image, ImageMath, ImageOps


BLOCK_PREFIX = "assets/minecraft/textures/block/"
ITEM_PREFIX = "assets/minecraft/textures/item/"
GUI_PREFIX = "assets/minecraft/textures/gui/"

EARLY_SURVIVAL_ITEMS = {
    *(f"{material}_{tool}.png" for material in ("wooden", "stone", "iron", "diamond")
      for tool in ("sword", "pickaxe", "axe", "shovel", "hoe")),
    *(f"{material}_{piece}.png" for material in ("leather", "iron", "diamond")
      for piece in ("helmet", "chestplate", "leggings", "boots")),
    "arrow.png", "bow.png", "bow_pulling_0.png", "bow_pulling_1.png", "bow_pulling_2.png",
    "stick.png", "coal.png", "charcoal.png",
    "raw_iron.png", "iron_ingot.png", "diamond.png", "flint.png", "string.png",
    "bucket.png", "water_bucket.png", "lava_bucket.png", "milk_bucket.png",
    "oak_door.png", "oak_boat.png", "oak_chest_boat.png", "minecart.png",
    "chest_minecart.png", "furnace_minecart.png", "apple.png", "golden_apple.png",
    "bread.png", "wheat.png", "wheat_seeds.png", "carrot.png", "potato.png",
    "baked_potato.png", "beetroot.png", "beetroot_seeds.png", "melon_slice.png",
    "sweet_berries.png", "beef.png", "cooked_beef.png", "porkchop.png",
    "cooked_porkchop.png", "chicken.png", "cooked_chicken.png", "mutton.png",
    "cooked_mutton.png", "rabbit.png", "cooked_rabbit.png", "cod.png", "cooked_cod.png",
    "salmon.png", "cooked_salmon.png", "rotten_flesh.png", "egg.png", "bowl.png",
    "mushroom_stew.png", "flint_and_steel.png", "shears.png", "fishing_rod.png",
}

HUD_TEXTURES = ("icons.png",)
SHIELD_TEXTURES = ("shield_base.png", "shield_base_nopattern.png")


def reset_directory(path: Path) -> None:
    """Remove generated output despite short-lived Windows file release races."""
    if path.exists():
        last_error: OSError | None = None
        for attempt in range(6):
            try:
                shutil.rmtree(path)
                last_error = None
                break
            except OSError as error:
                last_error = error
                time.sleep(0.15 * (attempt + 1))
        if last_error is not None:
            raise last_error
    path.mkdir(parents=True, exist_ok=True)


def trim_export_border(source: Image.Image) -> Image.Image:
    """Remove a contiguous, near-solid black export strip from the bottom edge."""
    rgb = source.convert("RGB")
    width, bottom = rgb.size
    while bottom > 1:
        row = (rgb.getpixel((x, bottom - 1)) for x in range(width))
        black_fraction = sum(max(pixel) < 20 for pixel in row) / width
        if black_fraction < 0.95:
            break
        bottom -= 1
    return source.crop((0, 0, width, bottom))


def square_source(source_path: Path, target_size: int) -> Image.Image:
    source = trim_export_border(Image.open(source_path).convert("RGBA"))
    # Solid block faces must remain solid at their borders. Letterboxing a
    # non-square source with transparent pixels exposes the sky between blocks.
    tile = ImageOps.fit(
        source,
        (target_size, target_size),
        method=Image.Resampling.LANCZOS,
        centering=(0.5, 0.5),
    )
    if tile.getchannel("A").getextrema() != (255, 255):
        opaque = Image.new("RGBA", tile.size, (0, 0, 0, 255))
        opaque.alpha_composite(tile)
        tile = opaque
    return tile


def ryo_masked_texture(tile: Image.Image, vanilla: Image.Image) -> Image.Image:
    """Put the one Ryo image inside a vanilla silhouette while retaining edge detail."""
    vanilla = vanilla.convert("RGBA")
    ryo = ImageOps.fit(tile, vanilla.size, method=Image.Resampling.LANCZOS)
    luminance = vanilla.convert("L")
    themed_channels = ryo.convert("RGB").split()
    shaded = tuple(
        ImageMath.eval(
            "convert(channel * (140 + light * 65 / 100) / 255, 'L')",
            channel=channel,
            light=luminance,
        )
        for channel in themed_channels
    )
    return Image.merge("RGBA", (*shaded, vanilla.getchannel("A")))


def generate_item_textures(jar: zipfile.ZipFile, names: set[str], output_dir: Path, tile: Image.Image) -> int:
    item_root = output_dir / "assets" / "minecraft" / "textures" / "item"
    reset_directory(item_root)

    generated = 0
    for relative in sorted(EARLY_SURVIVAL_ITEMS):
        name = f"{ITEM_PREFIX}{relative}"
        if name not in names:
            raise ValueError(f"Minecraft jar is missing required early-survival item: {relative}")
        with jar.open(name) as raw:
            vanilla = Image.open(raw).convert("RGBA")
            vanilla.load()
        themed = ryo_masked_texture(tile, vanilla)
        themed.save(item_root / relative)
        generated += 1
    return generated


def generate_hud_textures(jar: zipfile.ZipFile, output_dir: Path, tile: Image.Image) -> int:
    gui_root = output_dir / "assets" / "minecraft" / "textures" / "gui"
    gui_root.mkdir(parents=True, exist_ok=True)
    generated = 0

    for relative in HUD_TEXTURES:
        name = f"{GUI_PREFIX}{relative}"
        with jar.open(name) as raw:
            vanilla = Image.open(raw).convert("RGBA")
            vanilla.load()
        ryo_masked_texture(tile, vanilla).save(gui_root / relative)
        generated += 1

    widgets_name = f"{GUI_PREFIX}widgets.png"
    with jar.open(widgets_name) as raw:
        widgets = Image.open(raw).convert("RGBA")
        widgets.load()
    # Only the hotbar, selected slot, and offhand slot live in these regions.
    for box in ((0, 0, 182, 22), (0, 22, 53, 46)):
        vanilla_region = widgets.crop(box)
        widgets.alpha_composite(ryo_masked_texture(tile, vanilla_region), (box[0], box[1]))
    widgets.save(gui_root / "widgets.png")
    return generated + 1


def generate_shield_textures(jar: zipfile.ZipFile, output_dir: Path, tile: Image.Image) -> int:
    entity_root = output_dir / "assets" / "minecraft" / "textures" / "entity"
    entity_root.mkdir(parents=True, exist_ok=True)
    for relative in SHIELD_TEXTURES:
        name = f"assets/minecraft/textures/entity/{relative}"
        with jar.open(name) as raw:
            vanilla = Image.open(raw).convert("RGBA")
            vanilla.load()
        ryo_masked_texture(tile, vanilla).save(entity_root / relative)
    return len(SHIELD_TEXTURES)


def clear_model_redirects(output_dir: Path) -> None:
    """Remove old block texture model overrides before shader-time tinting."""
    model_root = output_dir / "assets" / "minecraft" / "models"
    reset_directory(model_root)


def generate_textures(minecraft_jar: Path, source_image: Path, output_dir: Path, target_size: int) -> dict[str, int | str]:
    if not minecraft_jar.exists():
        raise FileNotFoundError(f"Minecraft jar not found: {minecraft_jar}")
    if not source_image.exists():
        raise FileNotFoundError(f"Source image not found: {source_image}")

    texture_root = output_dir / "assets" / "minecraft" / "textures" / "block"
    reset_directory(texture_root)

    tile = square_source(source_image, target_size)

    with zipfile.ZipFile(minecraft_jar) as jar:
        names = set(jar.namelist())
        vanilla_block_textures = sum(
            name.startswith(BLOCK_PREFIX) and name.endswith(".png")
            for name in names
        )
        clear_model_redirects(output_dir)
        generated_items = generate_item_textures(jar, names, output_dir, tile)
        generated_hud = generate_hud_textures(jar, output_dir, tile)
        generated_shields = generate_shield_textures(jar, output_dir, tile)

    summary = {
        "generated_block_textures": 0,
        "vanilla_block_textures": vanilla_block_textures,
        "model_texture_redirects": 0,
        "block_tint_renderer": "iris_shaderpack",
        "generated_early_survival_items": generated_items,
        "generated_hud_textures": generated_hud,
        "generated_shield_textures": generated_shields,
        "target_texture_size": target_size,
    }
    summary_path = output_dir / "assets" / "minecraft" / "textures" / "ryo-blocks-summary.json"
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    return summary


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Ryo item/HUD assets while block tinting is handled by Iris.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--source-image", default=Path("source/ryo.png"), type=Path)
    parser.add_argument("--output-dir", default=Path("src/main/resources/resourcepacks/ryo_blocks"), type=Path)
    parser.add_argument("--target-size", default=512, type=int)
    args = parser.parse_args()

    summary = generate_textures(args.minecraft_jar, args.source_image, args.output_dir, args.target_size)
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
