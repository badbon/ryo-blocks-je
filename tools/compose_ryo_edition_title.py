from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


TITLE_SIZE = (512, 64)
TITLE_PADDING = (8, 2)


def compose_title(source: Path, output: Path) -> None:
    image = Image.open(source).convert("RGBA")
    alpha_box = image.getchannel("A").getbbox()
    if alpha_box is None:
        raise ValueError("RYO EDITION source has no visible pixels")
    title = image.crop(alpha_box)
    max_width = TITLE_SIZE[0] - TITLE_PADDING[0] * 2
    max_height = TITLE_SIZE[1] - TITLE_PADDING[1] * 2
    scale = min(max_width / title.width, max_height / title.height)
    rendered_size = (max(1, round(title.width * scale)), max(1, round(title.height * scale)))
    title = title.resize(rendered_size, Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", TITLE_SIZE, (0, 0, 0, 0))
    canvas.alpha_composite(title, ((TITLE_SIZE[0] - title.width) // 2, (TITLE_SIZE[1] - title.height) // 2))
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output)


def main() -> None:
    parser = argparse.ArgumentParser(description="Fit the generated RYO EDITION title into Minecraft's subtitle slot.")
    parser.add_argument("--source", default=Path("source/ryo-edition-title.png"), type=Path)
    parser.add_argument(
        "--output",
        default=Path("src/main/resources/resourcepacks/ryo_blocks/assets/minecraft/textures/gui/title/edition.png"),
        type=Path,
    )
    args = parser.parse_args()
    compose_title(args.source, args.output)


if __name__ == "__main__":
    main()
