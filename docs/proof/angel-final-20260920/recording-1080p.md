# 1080p Audio Recording - 2026-09-20

`combat-1080p-audio.mp4` is a fresh hidden-client recording of the same two combat scenes as `combat.mp4`: Nijika, then Kikuri. Production AI is unchanged; individual attacks differ naturally between runs.

- Native framebuffer: 1920x1080. H.264, 20 fps, 490 encoded frames, 24.50 seconds of video.
- Actual Minecraft audio: AAC stereo, 48000 Hz, 192 kb/s, 24.52 seconds. No added audio or upscaling.
- Decoded audio mean -21.7 dBFS, peak -0.8 dBFS. Decoded six-frame contact sheet inspected.
- Runtime at 12:56:26: both combat checks passed; all 480 requested source frames saved. Nijika 25 shots with damage, Kikuri 30 potion shots with damage and status effects.
- Log confirmed `OpenAL initialized on device Wave File Writer`. `ALSOFT_DRIVERS=wave` contains no fallback backend. The window remained hidden. No speaker playback was used.
- Frame monotonic timestamps preserve real elapsed time, including initial loading-related tick jitter. Audio segments start at the corresponding PCM byte positions; file buffering can introduce tens of milliseconds of sync uncertainty. Segment timing is retained in `combat-1080p-audio.timing.json`.
- This is the development proof client, not a new installation or actual-profile capture. No gameplay, skins, animation, or installed game files changed.

## Reproduce

Use Java 17 and the existing hidden proof launcher. Before another recording, move prior `run/proof-recording.csv` and `run/screenshots/angel-recording-*.png` to an unused temporary capture folder, so screenshot names and timing cannot accidentally mix. The PCM WAV is regenerated. Do not run two proof clients simultaneously.

```powershell
$env:ALSOFT_DRIVERS = 'wave'
$env:ALSOFT_CONF = (Resolve-Path tools/proof-audio.conf).Path
.\gradlew.bat --no-daemon --no-parallel runClient -PbossAbilityProof -PbossVariantsProof -PproofRecording
python tools/encode_angel_recording.py run docs/proof/angel-final-20260920/combat-1080p-audio.mp4
```

Normal proof mode still cancels sound-device startup. The recording exception requires an exact file-only backend environment; failure cannot fall back to a speaker. Backend behavior follows [OpenAL Soft's configuration](https://github.com/kcat/openal-soft/blob/master/alsoftrc.sample) and [wave backend](https://github.com/kcat/openal-soft/blob/master/alc/backends/wave.cpp).
