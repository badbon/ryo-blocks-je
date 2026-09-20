"""Mux hidden 1080p proof frames with their matching file-only Minecraft audio."""
import csv
import json
from pathlib import Path
import struct
import subprocess
import sys

run = Path(sys.argv[1]).resolve()
output = Path(sys.argv[2]).resolve()
audio = run / "proof-audio.wav"
with audio.open("rb") as stream:
    assert stream.read(4) == b"RIFF"
    stream.seek(12)
    while True:
        chunk, size = struct.unpack("<4sI", stream.read(8))
        if chunk == b"fmt ":
            fmt = stream.read(size)
            _, channels, rate, byte_rate, _, bits = struct.unpack_from("<HHIIHH", fmt)
            assert (channels, rate, bits) == (2, 48000, 16)
        elif chunk == b"data":
            data_offset = stream.tell()
            break
        else:
            stream.seek(size, 1)
        if size % 2:
            stream.seek(1, 1)

with (run / "proof-recording.csv").open() as stream:
    rows = [tuple(map(int, row)) for row in csv.reader(stream)]
concat = ["ffconcat version 1.0"]
segments = []
for stage in (2, 3):
    captured = [row for row in rows if row[0] == stage]
    assert len(captured) == 241 and captured[-1][1] == -1
    first, last = captured[0], captured[-1]
    start = (first[3] - data_offset) / byte_rate
    duration = (last[2] - first[2]) / 1e9
    segments.append({"stage": stage, "audio_start": start, "duration": duration})
    for row, following in zip(captured, captured[1:]):
        frame = run / "screenshots" / f"angel-recording-{row[1]:04d}.png"
        assert frame.is_file(), frame
        concat.extend([f"file '{frame.as_posix()}'", f"duration {(following[2] - row[2]) / 1e9:.9f}"])
concat.append(concat[-2])
manifest = run / "proof-recording.ffconcat"
manifest.write_text("\n".join(concat) + "\n")
filters = []
for index, segment in enumerate(segments):
    filters.append(f"[1:a]atrim=start={segment['audio_start']}:duration={segment['duration']},asetpts=PTS-STARTPTS[a{index}]")
filters.append("[a0][a1]concat=n=2:v=0:a=1[a]")
output.parent.mkdir(parents=True, exist_ok=True)
subprocess.run(["ffmpeg", "-hide_banner", "-y", "-threads", "2", "-safe", "0", "-f", "concat", "-i", str(manifest),
    "-i", str(audio), "-filter_complex", ";".join(filters), "-map", "0:v", "-map", "[a]",
    "-c:v", "libx264", "-threads", "2", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
    "-r", "20", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", "-shortest", str(output)], check=True)
output.with_suffix(".timing.json").write_text(json.dumps({"segments": segments,
    "audio": "OpenAL Soft Wave File Writer, 48000 Hz stereo PCM",
    "sync": "Frame monotonic timestamps; audio starts from PCM byte positions (file buffering may add tens of milliseconds)."}, indent=2) + "\n")
