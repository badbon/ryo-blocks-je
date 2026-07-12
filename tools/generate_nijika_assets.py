from __future__ import annotations

import argparse
import shutil
import zipfile
from pathlib import Path

from PIL import Image, ImageMath, ImageOps


CHEST_TEXTURES = (
    "christmas.png",
    "christmas_left.png",
    "christmas_right.png",
    "ender.png",
    "normal.png",
    "normal_left.png",
    "normal_right.png",
    "trapped.png",
    "trapped_left.png",
    "trapped_right.png",
)
CHEST_PREFIX = "assets/minecraft/textures/entity/chest/"
PLAYER_TEXTURE = "assets/ryo-blocks/textures/entity/player/nijika.png"
CHEST_FRONT_PANELS = ((14, 19, 28, 33), (28, 19, 42, 33))
CHEST_LATCH = (0, 0, 6, 5)


def verify_slim_skin(skin: Image.Image) -> None:
    if skin.size != (64, 64):
        raise ValueError(f"Nijika skin must be 64x64, got {skin.size}")
    alpha = skin.getchannel("A")
    markers = ((54, 20), (55, 20), (54, 31), (55, 31), (46, 52), (47, 52), (46, 63), (47, 63))
    if any(alpha.getpixel(point) != 0 for point in markers):
        raise ValueError("Nijika skin must use the slim-arm player layout")


def shade_with_template(portrait: Image.Image, template: Image.Image) -> Image.Image:
    themed = ImageOps.fit(portrait, template.size, method=Image.Resampling.NEAREST).convert("RGB")
    light = template.convert("L")
    channels = tuple(
        ImageMath.eval(
            "convert(channel * (155 + light * 55 / 100) / 255, 'L')",
            channel=channel,
            light=light,
        )
        for channel in themed.split()
    )
    return Image.merge("RGBA", (*channels, template.getchannel("A")))


def make_chest_texture(template: Image.Image, skin: Image.Image) -> Image.Image:
    portrait = skin.crop((8, 8, 16, 16))
    output = shade_with_template(portrait, template)

    # The single-chest UV uses these two panels for the front/back body. Using
    # a full, pixel-clean portrait on both makes every chest read as Nijika from
    # normal player angles without distorting the skin across hinges or the lid.
    for box in CHEST_FRONT_PANELS:
        panel_template = template.crop(box)
        panel = shade_with_template(portrait, panel_template)
        output.alpha_composite(panel, (box[0], box[1]))

    # Keep the tiny latch's vanilla contrast so opening direction remains clear.
    output.alpha_composite(template.crop(CHEST_LATCH), (0, 0))
    return output


def generate_assets(minecraft_jar: Path, skin_path: Path, output_dir: Path) -> dict[str, int]:
    if not minecraft_jar.exists():
        raise FileNotFoundError(f"Minecraft jar not found: {minecraft_jar}")
    if not skin_path.exists():
        raise FileNotFoundError(f"Nijika skin not found: {skin_path}")

    skin = Image.open(skin_path).convert("RGBA")
    skin.load()
    verify_slim_skin(skin)

    player_output = output_dir / PLAYER_TEXTURE
    player_output.parent.mkdir(parents=True, exist_ok=True)
    skin.save(player_output)

    chest_output = output_dir / CHEST_PREFIX
    if chest_output.exists():
        shutil.rmtree(chest_output)
    chest_output.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(minecraft_jar) as jar:
        for texture in CHEST_TEXTURES:
            with jar.open(f"{CHEST_PREFIX}{texture}") as raw:
                template = Image.open(raw).convert("RGBA")
                template.load()
            make_chest_texture(template, skin).save(chest_output / texture)

    return {"nijika_skin": 1, "nijika_chests": len(CHEST_TEXTURES)}


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Nijika player and chest assets.")
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--skin", default=Path("source/nijika-player-skin.png"), type=Path)
    parser.add_argument("--output-dir", default=Path("src/main/resources"), type=Path)
    args = parser.parse_args()
    print(generate_assets(args.minecraft_jar, args.skin, args.output_dir))


if __name__ == "__main__":
    main()
