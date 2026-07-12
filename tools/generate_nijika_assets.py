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
CHEST_LATCH = (0, 0, 6, 5)

# The bottom model starts at UV (0, 19). The old generator incorrectly used
# the two 14x14 top/bottom faces at (14, 19) and (28, 19), which put Nijika on
# horizontal surfaces. The actual outward face is the south face of the body:
# model Z=14, where the latch is placed. Double-chest halves are one pixel
# wider, so their front UV rectangle is shifted two pixels to the right.
SINGLE_CHEST_TEXTURES = frozenset({"christmas.png", "ender.png", "normal.png", "trapped.png"})
SINGLE_FRONT_PANEL = (42, 33, 56, 43)
DOUBLE_FRONT_PANEL = (44, 33, 58, 43)


def verify_slim_skin(skin: Image.Image) -> None:
    if skin.size != (64, 64):
        raise ValueError(f"Nijika skin must be 64x64, got {skin.size}")
    alpha = skin.getchannel("A")
    markers = ((54, 20), (55, 20), (54, 31), (55, 31), (46, 52), (47, 52), (46, 63), (47, 63))
    if any(alpha.getpixel(point) != 0 for point in markers):
        raise ValueError("Nijika skin must use the slim-arm player layout")


def tint_template(template: Image.Image) -> Image.Image:
    """Keep vanilla material shading while giving the chest Nijika's warm palette."""
    light = template.convert("L")
    base = (218, 174, 66)
    channels = tuple(
        ImageMath.eval("convert(light * value / 255, 'L')", light=light, value=value)
        for value in base
    )
    return Image.merge("RGBA", (*channels, template.getchannel("A")))


def shade_portrait(portrait: Image.Image, panel_template: Image.Image) -> Image.Image:
    """Render one upright face with the light/shadow detail of its body panel."""
    themed = ImageOps.fit(portrait, panel_template.size, method=Image.Resampling.NEAREST).convert("RGB")
    light = panel_template.convert("L")
    channels = tuple(
        ImageMath.eval(
            "convert(channel * (155 + light * 55 / 100) / 255, 'L')",
            channel=channel,
            light=light,
        )
        for channel in themed.split()
    )
    return Image.merge("RGBA", (*channels, panel_template.getchannel("A")))


def front_panel_for(texture_name: str) -> tuple[int, int, int, int]:
    return SINGLE_FRONT_PANEL if texture_name in SINGLE_CHEST_TEXTURES else DOUBLE_FRONT_PANEL


def make_chest_texture(template: Image.Image, skin: Image.Image, texture_name: str) -> Image.Image:
    portrait = skin.crop((8, 8, 16, 16))
    output = tint_template(template)

    box = front_panel_for(texture_name)
    panel_template = template.crop(box)
    # This vertical body face is rendered upright by Minecraft's chest model.
    # Do not rotate or mirror it: the UV itself handles the world-facing turns.
    output.alpha_composite(shade_portrait(portrait, panel_template), (box[0], box[1]))

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
            generated = make_chest_texture(template, skin, texture)
            if generated.size != template.size:
                raise ValueError(f"generated chest dimensions changed: {texture}")
            if generated.getchannel("A").tobytes() != template.getchannel("A").tobytes():
                raise ValueError(f"generated chest alpha changed: {texture}")
            generated.save(chest_output / texture)

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
