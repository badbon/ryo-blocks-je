from __future__ import annotations

import argparse
import zipfile
from pathlib import Path

from PIL import Image, ImageChops

from build_ryo_shaderpack import REQUIRED_SHADER_FILES
from generate_ryo_textures import square_source


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate the low-memory Iris Ryo terrain shader pack.")
    parser.add_argument("--shaderpack", default=Path("build/Ryo-Vanilla-Tint.zip"), type=Path)
    parser.add_argument("--source-image", default=Path("source/ryo.png"), type=Path)
    parser.add_argument("--target-size", default=512, type=int)
    args = parser.parse_args()

    failures: list[str] = []
    if not args.shaderpack.exists():
        failures.append(f"missing shader pack: {args.shaderpack}")
    if not args.source_image.exists():
        failures.append(f"missing Ryo source image: {args.source_image}")
    if failures:
        raise SystemExit("\n".join(failures))

    with zipfile.ZipFile(args.shaderpack) as archive:
        names = set(archive.namelist())
        expected = {path.as_posix() for path in REQUIRED_SHADER_FILES} | {"shaders/textures/ryo.png"}
        missing = sorted(expected - names)
        if missing:
            failures.extend(f"missing archive entry: {name}" for name in missing)

        properties = archive.read("shaders/shaders.properties").decode("utf-8")
        fragment = archive.read("shaders/gbuffers_terrain.fsh").decode("utf-8")
        if "customTexture.ryoTexture = textures/ryo.png" not in properties:
            failures.append("shader pack does not bind the shared Ryo sampler")
        for required in ("texture(gtexture, atlasCoord)", "texture(ryoTexture, ryoCoord)", "RYO_OVERLAY_STRENGTH"):
            if required not in fragment:
                failures.append(f"terrain shader is missing required expression: {required}")

        with archive.open("shaders/textures/ryo.png") as raw:
            generated = Image.open(raw).convert("RGBA")
            generated.load()
        expected_tile = square_source(args.source_image, args.target_size)
        if generated.size != expected_tile.size:
            failures.append(f"shared Ryo texture has wrong dimensions: {generated.size}")
        elif ImageChops.difference(generated, expected_tile).getbbox():
            failures.append("shared Ryo texture does not match the authoritative source")

    result = {"shader_entries": len(expected), "failures": failures}
    print(result)
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
