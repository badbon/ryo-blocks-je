"""Rasterize the authored 16x16 single-chip sprite; no resampling or AI skin edits."""
from pathlib import Path
from PIL import Image

PIXELS = (
    "................",
    ".......bb.......",
    "......bhob......",
    "......hyoob.....",
    ".....bhyyoob....",
    ".....hyyoyob....",
    "....bhysyooob...",
    "....hyyyoyosb...",
    "...bhyyysoooob..",
    "...hysyyooyoob..",
    "..bhyyyoyosooob.",
    "..hyyosyyooyobb.",
    ".bhyyyoyoooobb..",
    ".bhhoooooobb....",
    "..bbbbbbbb......",
    "................",
)
COLORS = {".": (0, 0, 0, 0), "b": (168, 75, 19, 255), "h": (255, 211, 90, 255),
          "y": (246, 174, 43, 255), "o": (228, 132, 27, 255), "s": (192, 94, 23, 255)}

if __name__ == "__main__":
    assert len(PIXELS) == 16 and all(len(row) == 16 for row in PIXELS)
    image = Image.new("RGBA", (16, 16))
    image.putdata([COLORS[pixel] for row in PIXELS for pixel in row])
    path = Path(__file__).resolve().parents[1] / "src/main/resources/assets/ryo-blocks/textures/item/dorito.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)
