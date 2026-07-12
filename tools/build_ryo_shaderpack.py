from __future__ import annotations

import argparse
import zipfile
from pathlib import Path

from generate_ryo_textures import square_source


SHADER_SOURCE = Path("source/ryo-vanilla-tint")
REQUIRED_SHADER_FILES = (
    Path("pack.mcmeta"),
    Path("shaders/shaders.properties"),
    Path("shaders/gbuffers_terrain.vsh"),
    Path("shaders/gbuffers_terrain.fsh"),
)


def build_shaderpack(source_image: Path, output: Path, target_size: int) -> dict[str, int]:
    missing = [path for path in REQUIRED_SHADER_FILES if not (SHADER_SOURCE / path).exists()]
    if missing:
        raise FileNotFoundError(f"Missing shader source files: {missing}")
    if not source_image.exists():
        raise FileNotFoundError(f"Ryo source image not found: {source_image}")

    output.parent.mkdir(parents=True, exist_ok=True)
    tile = square_source(source_image, target_size)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for relative in REQUIRED_SHADER_FILES:
            archive.write(SHADER_SOURCE / relative, relative.as_posix())
        with archive.open("shaders/textures/ryo.png", "w") as raw:
            tile.save(raw, "PNG")

    return {"shader_files": len(REQUIRED_SHADER_FILES), "ryo_texture_size": target_size}


def main() -> None:
    parser = argparse.ArgumentParser(description="Build the low-memory Iris Ryo terrain shader pack.")
    parser.add_argument("--source-image", default=Path("source/ryo.png"), type=Path)
    parser.add_argument("--output", default=Path("build/Ryo-Vanilla-Tint.zip"), type=Path)
    parser.add_argument("--target-size", default=512, type=int)
    args = parser.parse_args()
    print(build_shaderpack(args.source_image, args.output, args.target_size))


if __name__ == "__main__":
    main()
