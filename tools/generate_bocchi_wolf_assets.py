from __future__ import annotations

import argparse
import shutil
import zipfile
from pathlib import Path

from PIL import Image


WOLF_TEXTURES = ("wolf.png", "wolf_angry.png", "wolf_tame.png")
WOLF_PREFIX = "assets/minecraft/textures/entity/wolf/"
VANILLA_COLLAR_TEXTURE = f"{WOLF_PREFIX}wolf_collar.png"
BOCCHI_COLLAR_TEXTURE = Path(
    "assets/ryo-blocks/textures/entity/wolf/bocchi_collar.png"
)

# Dominant unshaded tracksuit fill sampled from the official TV anime artwork:
# https://bocchi.rocks/tv/assets/img/common/pagechara/ph_ch_1.png
BOCCHI_TRACKSUIT_PINK = (246, 169, 175)
BOCCHI_CLIP_BLUE = (80, 146, 189)
BOCCHI_CLIP_YELLOW = (182, 151, 62)
# Stack the hair-clip pair on the center of one visible collar side.
COLLAR_BLUE_PIXEL = (35, 3)
COLLAR_YELLOW_PIXEL = (35, 4)
VANILLA_FUR_ANCHOR = (211, 207, 207)
VANILLA_COLLAR_ANCHOR = 184.0
FUR_LUMA_MIN = 80.0
FUR_LUMA_MAX = 240.0
FUR_CHANNEL_SPREAD_MAX = 12


def pixel_luma(pixel: tuple[int, int, int, int]) -> float:
    return sum(pixel[:3]) / 3.0


def is_fur_pixel(pixel: tuple[int, int, int, int]) -> bool:
    red, green, blue, alpha = pixel
    luma = pixel_luma(pixel)
    return (
        alpha > 0
        and FUR_LUMA_MIN <= luma <= FUR_LUMA_MAX
        and max(red, green, blue) - min(red, green, blue) <= FUR_CHANNEL_SPREAD_MAX
    )


def recolor_fur(template: Image.Image) -> Image.Image:
    """Map vanilla fur shading onto Bocchi pink without changing any UV or alpha."""
    anchor_luma = sum(VANILLA_FUR_ANCHOR) / 3.0
    output_pixels: list[tuple[int, int, int, int]] = []

    for pixel in template.convert("RGBA").getdata():
        if not is_fur_pixel(pixel):
            output_pixels.append(pixel)
            continue

        shade = pixel_luma(pixel) / anchor_luma
        themed = tuple(min(255, round(channel * shade)) for channel in BOCCHI_TRACKSUIT_PINK)
        output_pixels.append((*themed, pixel[3]))

    output = Image.new("RGBA", template.size)
    output.putdata(output_pixels)
    return output


def recolor_collar(template: Image.Image) -> Image.Image:
    """Preserve the collar UV and shading while applying Bocchi's three colors."""
    output_pixels: list[tuple[int, int, int, int]] = []

    for pixel in template.convert("RGBA").getdata():
        if pixel[3] == 0:
            output_pixels.append(pixel)
            continue

        shade = pixel_luma(pixel) / VANILLA_COLLAR_ANCHOR
        themed = tuple(min(255, round(channel * shade)) for channel in BOCCHI_TRACKSUIT_PINK)
        output_pixels.append((*themed, pixel[3]))

    output = Image.new("RGBA", template.size)
    output.putdata(output_pixels)

    blue_alpha = output.getpixel(COLLAR_BLUE_PIXEL)[3]
    yellow_alpha = output.getpixel(COLLAR_YELLOW_PIXEL)[3]
    if blue_alpha == 0 or yellow_alpha == 0:
        raise ValueError("Bocchi clip pixels must land on opaque vanilla collar pixels")
    output.putpixel(COLLAR_BLUE_PIXEL, (*BOCCHI_CLIP_BLUE, blue_alpha))
    output.putpixel(COLLAR_YELLOW_PIXEL, (*BOCCHI_CLIP_YELLOW, yellow_alpha))
    return output


def generate_assets(minecraft_jar: Path, output_dir: Path) -> dict[str, object]:
    if not minecraft_jar.exists():
        raise FileNotFoundError(f"Minecraft jar not found: {minecraft_jar}")

    wolf_output = output_dir / WOLF_PREFIX
    if wolf_output.exists():
        shutil.rmtree(wolf_output)
    wolf_output.mkdir(parents=True, exist_ok=True)

    recolored_pixels = 0
    with zipfile.ZipFile(minecraft_jar) as jar:
        for texture_name in WOLF_TEXTURES:
            with jar.open(f"{WOLF_PREFIX}{texture_name}") as raw:
                template = Image.open(raw).convert("RGBA")
                template.load()

            themed = recolor_fur(template)
            if themed.size != template.size:
                raise ValueError(f"generated wolf dimensions changed: {texture_name}")
            if themed.getchannel("A").tobytes() != template.getchannel("A").tobytes():
                raise ValueError(f"generated wolf alpha changed: {texture_name}")

            recolored_pixels += sum(is_fur_pixel(pixel) for pixel in template.getdata())
            themed.save(wolf_output / texture_name)

        with jar.open(VANILLA_COLLAR_TEXTURE) as raw:
            vanilla_collar = Image.open(raw).convert("RGBA")
            vanilla_collar.load()

    bocchi_collar = recolor_collar(vanilla_collar)
    collar_output = output_dir.parent.parent / BOCCHI_COLLAR_TEXTURE
    collar_output.parent.mkdir(parents=True, exist_ok=True)
    bocchi_collar.save(collar_output)

    return {
        "bocchi_tracksuit_pink": "#F6A9AF",
        "bocchi_clip_blue": "#5092BD",
        "bocchi_clip_yellow": "#B6973E",
        "generated_wolf_textures": len(WOLF_TEXTURES),
        "recolored_fur_pixels": recolored_pixels,
        "generated_bocchi_collar": str(collar_output),
        "other_collar_dyes_preserved": True,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Bocchi-pink vanilla wolf textures.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument(
        "--output-dir",
        default=Path("src/main/resources/resourcepacks/ryo_blocks"),
        type=Path,
    )
    args = parser.parse_args()
    print(generate_assets(args.minecraft_jar, args.output_dir))


if __name__ == "__main__":
    main()
