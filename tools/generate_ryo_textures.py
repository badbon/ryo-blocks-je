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
RYO_OVERLAY_STRENGTH = 0.50
VANILLA_SPRITE_SIZE = 16

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

ITEM_OVERLAY_STRENGTH = 0.50
HIGH_RISK_ITEM_OVERLAY_STRENGTH = 0.20

# These are renderer/UI layers rather than standalone inventory items. Keeping
# them native preserves dynamically tinted leather, potion, egg, and recipe-slot cues.
ITEM_TEMPLATE_TEXTURES = {
    "empty_armor_slot_boots.png", "empty_armor_slot_chestplate.png",
    "empty_armor_slot_helmet.png", "empty_armor_slot_leggings.png",
    "empty_armor_slot_shield.png", "empty_slot_amethyst_shard.png",
    "empty_slot_axe.png", "empty_slot_diamond.png", "empty_slot_emerald.png",
    "empty_slot_hoe.png", "empty_slot_ingot.png", "empty_slot_lapis_lazuli.png",
    "empty_slot_pickaxe.png", "empty_slot_quartz.png", "empty_slot_redstone_dust.png",
    "empty_slot_shovel.png", "empty_slot_smithing_template_armor_trim.png",
    "empty_slot_smithing_template_netherite_upgrade.png", "empty_slot_sword.png",
    "firework_star_overlay.png", "leather_boots_overlay.png",
    "leather_chestplate_overlay.png", "leather_helmet_overlay.png",
    "leather_leggings_overlay.png", "potion_overlay.png", "spawn_egg_overlay.png",
}

HIGH_RISK_ITEM_TEXTURES = {
    "glass_bottle.png", "experience_bottle.png", "honey_bottle.png", "dragon_breath.png",
    "filled_map.png", "filled_map_markings.png", "map.png",
    "arrow.png", "spectral_arrow.png", "tipped_arrow_base.png", "tipped_arrow_head.png",
    "redstone.png", "bone_meal.png", "light.png",
    "beef.png", "cooked_beef.png", "chicken.png", "cooked_chicken.png",
    "mutton.png", "cooked_mutton.png", "porkchop.png", "cooked_porkchop.png",
    "rabbit.png", "cooked_rabbit.png", "rabbit_stew.png", "mushroom_stew.png",
    "suspicious_stew.png", "cod.png", "cooked_cod.png", "salmon.png", "cooked_salmon.png",
    "sweet_berries.png", "glow_berries.png", "dried_kelp.png", "kelp.png",
    # Ore drops and the resources refined from them retain their familiar colour cues.
    "coal.png", "charcoal.png", "diamond.png", "emerald.png", "lapis_lazuli.png",
    "raw_copper.png", "raw_gold.png", "raw_iron.png", "copper_ingot.png",
    "gold_ingot.png", "iron_ingot.png", "gold_nugget.png", "iron_nugget.png",
    "redstone.png", "quartz.png", "amethyst_shard.png", "netherite_scrap.png",
    "netherite_ingot.png", "clay_ball.png", "flint.png", "prismarine_shard.png",
    "prismarine_crystals.png", "echo_shard.png",
}

HIGH_RISK_ITEM_PREFIXES = ("clock_", "compass_", "recovery_compass_", "crossbow_", "light_")
HIGH_RISK_ITEM_SUFFIXES = (
    "_bucket.png", "_spawn_egg.png", "_dye.png", "_smithing_template.png",
    "_pottery_sherd.png", "_banner_pattern.png",
)

HUD_TEXTURES = ("icons.png",)
SHIELD_TEXTURES = ("shield_base.png", "shield_base_nopattern.png")

KITA_LAVA_TEXTURES = {
    "lava_still.png",
    "lava_flow.png",
}
KITA_LAVA_FRAME_SIZE = 64
KITA_LAVA_OVERLAY_STRENGTH = 0.68
COMPAT_EXCLUDED_BLOCK_PREFIXES = ("lava_", "water_")
COMPAT_EXCLUDED_BLOCK_SUBSTRINGS = ("glass",)


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


def shader_overlay_source(source_path: Path, target_size: int) -> Image.Image:
    """Fit the shader overlay while preserving transparent background pixels."""
    source = trim_export_border(Image.open(source_path).convert("RGBA"))
    return ImageOps.fit(
        source,
        (target_size, target_size),
        method=Image.Resampling.LANCZOS,
        centering=(0.5, 0.5),
    )


def ryo_masked_texture(tile: Image.Image, vanilla: Image.Image) -> Image.Image:
    """Put the one Ryo image inside a vanilla silhouette while retaining edge detail."""
    vanilla = vanilla.convert("RGBA")
    ryo = ImageOps.fit(tile, vanilla.size, method=Image.Resampling.LANCZOS)
    luminance = vanilla.convert("L")
    themed_channels = ryo.convert("RGB").split()
    shaded = tuple(
        ImageMath.lambda_eval(
            lambda args: args["convert"](
                args["channel"] * (140 + args["light"] * 65 / 100) / 255,
                "L",
            ),
            channel=channel,
            light=luminance,
        )
        for channel in themed_channels
    )
    return Image.merge("RGBA", (*shaded, vanilla.getchannel("A")))


def vanilla_item_texture_names(names: set[str]) -> list[str]:
    return sorted(
        Path(name).name
        for name in names
        if name.startswith(ITEM_PREFIX)
        and name.endswith(".png")
        and Path(name).name not in ITEM_TEMPLATE_TEXTURES
    )


def is_high_risk_item_texture(relative: str) -> bool:
    return (
        relative in HIGH_RISK_ITEM_TEXTURES
        or relative.startswith(HIGH_RISK_ITEM_PREFIXES)
        or relative.endswith(HIGH_RISK_ITEM_SUFFIXES)
    )


def ryo_overlay_texture(tile: Image.Image, vanilla: Image.Image, strength: float) -> Image.Image:
    """Blend Ryo's cutout over a vanilla item without changing its silhouette."""
    vanilla = vanilla.convert("RGBA")
    ryo = ImageOps.fit(tile, vanilla.size, method=Image.Resampling.LANCZOS)
    luminance = vanilla.convert("L")
    shaded = tuple(
        ImageMath.lambda_eval(
            lambda args: args["convert"](
                args["channel"] * (140 + args["light"] * 65 / 100) / 255,
                "L",
            ),
            channel=channel,
            light=luminance,
        )
        for channel in ryo.convert("RGB").split()
    )
    overlay_alpha = ryo.getchannel("A").point(lambda value: round(value * strength))
    blended = Image.composite(Image.merge("RGB", shaded), vanilla.convert("RGB"), overlay_alpha)
    return Image.merge("RGBA", (*blended.split(), vanilla.getchannel("A")))


def is_renderer_compat_block_texture(relative: str) -> bool:
    return (
        relative.endswith(".png")
        and not relative.startswith(COMPAT_EXCLUDED_BLOCK_PREFIXES)
        and not any(excluded in relative for excluded in COMPAT_EXCLUDED_BLOCK_SUBSTRINGS)
    )


def ryo_shader_equivalent_block_texture(tile: Image.Image, vanilla: Image.Image) -> Image.Image:
    vanilla = vanilla.convert("RGBA")
    overlay_sprite = ImageOps.fit(
        tile,
        (VANILLA_SPRITE_SIZE, VANILLA_SPRITE_SIZE),
        method=Image.Resampling.LANCZOS,
        centering=(0.5, 0.5),
    )
    overlay = Image.new("RGBA", vanilla.size, (0, 0, 0, 0))
    for y in range(0, vanilla.height, VANILLA_SPRITE_SIZE):
        for x in range(0, vanilla.width, VANILLA_SPRITE_SIZE):
            overlay.alpha_composite(overlay_sprite, (x, y))

    vanilla_rgb = vanilla.convert("RGB")
    overlay_alpha = overlay.getchannel("A").point(
        lambda value: round(value * RYO_OVERLAY_STRENGTH)
    )
    blended = Image.composite(overlay.convert("RGB"), vanilla_rgb, overlay_alpha)
    return Image.merge("RGBA", (*blended.split(), vanilla.getchannel("A")))


def generate_renderer_compat_block_textures(
    jar: zipfile.ZipFile,
    names: set[str],
    output_dir: Path,
    tile: Image.Image,
) -> int:
    block_root = output_dir / "assets" / "minecraft" / "textures" / "block"
    reset_directory(block_root)
    generated = 0
    for name in sorted(names):
        if not name.startswith(BLOCK_PREFIX) or not name.endswith(".png"):
            continue
        relative = name.removeprefix(BLOCK_PREFIX)
        if not is_renderer_compat_block_texture(relative):
            continue
        output_path = block_root / relative
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with jar.open(name) as raw:
            vanilla = Image.open(raw).convert("RGBA")
            vanilla.load()
        ryo_shader_equivalent_block_texture(tile, vanilla).save(output_path)
        metadata = f"{name}.mcmeta"
        if metadata in names:
            (block_root / f"{relative}.mcmeta").write_bytes(jar.read(metadata))
        generated += 1
    return generated


def generate_item_textures(
    jar: zipfile.ZipFile,
    names: set[str],
    output_dir: Path,
    tile: Image.Image,
) -> tuple[int, int]:
    item_root = output_dir / "assets" / "minecraft" / "textures" / "item"
    reset_directory(item_root)

    generated = 0
    high_risk_generated = 0
    for relative in vanilla_item_texture_names(names):
        name = f"{ITEM_PREFIX}{relative}"
        with jar.open(name) as raw:
            vanilla = Image.open(raw).convert("RGBA")
            vanilla.load()
        high_risk = is_high_risk_item_texture(relative)
        themed = ryo_overlay_texture(
            tile,
            vanilla,
            HIGH_RISK_ITEM_OVERLAY_STRENGTH if high_risk else ITEM_OVERLAY_STRENGTH,
        )
        themed.save(item_root / relative)
        metadata = f"{name}.mcmeta"
        if metadata in names:
            with jar.open(metadata) as raw:
                (item_root / f"{relative}.mcmeta").write_bytes(raw.read())
        generated += 1
        high_risk_generated += high_risk
    return generated, high_risk_generated


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


def generate_kita_lava_assets(
    jar: zipfile.ZipFile,
    output_dir: Path,
    source_path: Path,
) -> int:
    """Bake Kita into Minecraft's native animated lava sprites."""
    block_root = output_dir / "assets" / "minecraft" / "textures" / "block"
    block_root.mkdir(parents=True, exist_ok=True)
    portrait = trim_export_border(Image.open(source_path).convert("RGBA"))
    alpha_bbox = portrait.getchannel("A").getbbox()
    if alpha_bbox is not None:
        portrait = portrait.crop(alpha_bbox)

    for relative in sorted(KITA_LAVA_TEXTURES):
        name = f"{BLOCK_PREFIX}{relative}"
        with jar.open(name) as raw:
            vanilla = Image.open(raw).convert("RGBA")
            vanilla.load()
        kita_lava_texture(vanilla, portrait).save(block_root / relative)

        metadata = f"{name}.mcmeta"
        with jar.open(metadata) as raw:
            (block_root / f"{relative}.mcmeta").write_bytes(raw.read())

    return len(KITA_LAVA_TEXTURES)


def kita_lava_texture(vanilla: Image.Image, portrait: Image.Image) -> Image.Image:
    vanilla_frame_size = vanilla.width
    if vanilla_frame_size <= 0 or vanilla.height % vanilla_frame_size != 0:
        raise ValueError(f"Unexpected vanilla lava animation dimensions: {vanilla.size}")
    frame_count = vanilla.height // vanilla_frame_size
    themed = Image.new("RGBA", (KITA_LAVA_FRAME_SIZE, KITA_LAVA_FRAME_SIZE * frame_count))
    for frame_index in range(frame_count):
        top = frame_index * vanilla_frame_size
        frame = vanilla.crop((0, top, vanilla_frame_size, top + vanilla_frame_size)).resize(
            (KITA_LAVA_FRAME_SIZE, KITA_LAVA_FRAME_SIZE),
            Image.Resampling.NEAREST,
        )
        sprite_portrait = ImageOps.contain(
            portrait,
            (KITA_LAVA_FRAME_SIZE, KITA_LAVA_FRAME_SIZE),
            method=Image.Resampling.LANCZOS,
        )
        sprite = Image.new("RGBA", (KITA_LAVA_FRAME_SIZE, KITA_LAVA_FRAME_SIZE), (0, 0, 0, 0))
        sprite.alpha_composite(
            sprite_portrait,
            (
                (KITA_LAVA_FRAME_SIZE - sprite_portrait.width) // 2,
                KITA_LAVA_FRAME_SIZE - sprite_portrait.height,
            ),
        )
        overlay_alpha = sprite.getchannel("A").point(
            lambda value: round(value * KITA_LAVA_OVERLAY_STRENGTH)
        )
        blended_rgb = Image.composite(sprite.convert("RGB"), frame.convert("RGB"), overlay_alpha)
        themed.paste(
            Image.merge("RGBA", (*blended_rgb.split(), frame.getchannel("A"))),
            (0, frame_index * KITA_LAVA_FRAME_SIZE),
        )
    return themed


def generate_textures(
    minecraft_jar: Path,
    source_image: Path,
    item_overlay_source: Path,
    kita_lava_source: Path,
    output_dir: Path,
    target_size: int,
    renderer_compat_output_dir: Path,
) -> dict[str, int | str | float]:
    if not minecraft_jar.exists():
        raise FileNotFoundError(f"Minecraft jar not found: {minecraft_jar}")
    if not source_image.exists():
        raise FileNotFoundError(f"Source image not found: {source_image}")
    if not item_overlay_source.exists():
        raise FileNotFoundError(f"Item overlay source not found: {item_overlay_source}")
    if not kita_lava_source.exists():
        raise FileNotFoundError(f"Kita lava source not found: {kita_lava_source}")

    texture_root = output_dir / "assets" / "minecraft" / "textures" / "block"
    reset_directory(texture_root)

    tile = square_source(source_image, target_size)
    item_overlay_tile = shader_overlay_source(item_overlay_source, target_size)
    renderer_compat_tile = shader_overlay_source(item_overlay_source, target_size)

    with zipfile.ZipFile(minecraft_jar) as jar:
        names = set(jar.namelist())
        vanilla_block_textures = sum(
            name.startswith(BLOCK_PREFIX) and name.endswith(".png")
            for name in names
        )
        clear_model_redirects(output_dir)
        generated_kita_lava = generate_kita_lava_assets(jar, output_dir, kita_lava_source)
        generated_items, high_risk_items = generate_item_textures(jar, names, output_dir, item_overlay_tile)
        generated_hud = generate_hud_textures(jar, output_dir, tile)
        generated_shields = generate_shield_textures(jar, output_dir, tile)
        generated_compat_blocks = generate_renderer_compat_block_textures(
            jar,
            names,
            renderer_compat_output_dir,
            renderer_compat_tile,
        )

    summary = {
        "generated_block_textures": generated_kita_lava,
        "vanilla_block_textures": vanilla_block_textures,
        "model_texture_redirects": 0,
        "block_tint_renderer": "minecraft_core_shader",
        "kita_lava_renderer": "baked_vanilla_lava_animation",
        "kita_lava_frame_size": KITA_LAVA_FRAME_SIZE,
        "kita_lava_overlay_strength": KITA_LAVA_OVERLAY_STRENGTH,
        "generated_item_textures": generated_items,
        "high_risk_item_textures": high_risk_items,
        "item_overlay_strength": ITEM_OVERLAY_STRENGTH,
        "high_risk_item_overlay_strength": HIGH_RISK_ITEM_OVERLAY_STRENGTH,
        "generated_hud_textures": generated_hud,
        "generated_shield_textures": generated_shields,
        "target_texture_size": target_size,
    }
    summary_path = output_dir / "assets" / "minecraft" / "textures" / "ryo-blocks-summary.json"
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    compat_summary = {
        "block_tint_renderer": "baked_block_textures_for_iris_sodium",
        "overlay_strength": RYO_OVERLAY_STRENGTH,
        "vanilla_sprite_size": VANILLA_SPRITE_SIZE,
        "excluded_block_prefixes": list(COMPAT_EXCLUDED_BLOCK_PREFIXES),
        "excluded_block_substrings": list(COMPAT_EXCLUDED_BLOCK_SUBSTRINGS),
        "generated_block_textures": generated_compat_blocks,
    }
    compat_summary_path = (
        renderer_compat_output_dir
        / "assets"
        / "minecraft"
        / "textures"
        / "ryo-blocks-renderer-compat-summary.json"
    )
    compat_summary_path.parent.mkdir(parents=True, exist_ok=True)
    compat_summary_path.write_text(json.dumps(compat_summary, indent=2), encoding="utf-8")
    return summary


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Ryo item/HUD assets while block tinting is handled by jar-contained core shaders.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--source-image", default=Path("source/ryo.png"), type=Path)
    parser.add_argument("--item-overlay-source", default=Path("source/ryo-block-overlay.png"), type=Path)
    parser.add_argument("--kita-lava-source", default=Path("source/kita-lava-cutout.png"), type=Path)
    parser.add_argument("--output-dir", default=Path("src/main/resources/resourcepacks/ryo_blocks"), type=Path)
    parser.add_argument(
        "--renderer-compat-output-dir",
        default=Path("src/main/resources/resourcepacks/ryo_blocks_renderer_compat"),
        type=Path,
    )
    parser.add_argument("--target-size", default=512, type=int)
    args = parser.parse_args()

    summary = generate_textures(
        args.minecraft_jar,
        args.source_image,
        args.item_overlay_source,
        args.kita_lava_source,
        args.output_dir,
        args.target_size,
        args.renderer_compat_output_dir,
    )
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
