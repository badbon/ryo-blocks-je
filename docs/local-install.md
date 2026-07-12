# Local Install Notes

Pulled from `desktop-main`:

```text
C:\Users\Go4No\Documents\Codex\2026-07-07\i-want-to-make-private-minecraft
```

Local runnable profile:

```text
Minecraft Launcher profile: Ryo Blocks Fabric 1.20.1
Version: fabric-loader-0.19.3-1.20.1
Game directory: C:\Users\komputer\AppData\Roaming\.minecraft\ryo-blocks
Mods directory: C:\Users\komputer\AppData\Roaming\.minecraft\ryo-blocks\mods
```

Installed mods:

```text
fabric-api-0.92.9+1.20.1.jar
iris-1.7.6+mc1.20.1.jar
indium-1.0.36+mc1.20.1.jar
ryo-blocks-1.0.0.jar
sodium-fabric-0.5.13+mc1.20.1.jar
```

## Low-Memory Ryo Block Tint

The profile uses Iris `1.7.6` with Sodium `0.5.13` and Indium `1.0.36`. This exact combination is required because Sodium `0.5.13` needs Iris `1.7.6` or newer and Indium `1.0.36` or newer.

Ryo block tinting is performed by the Iris shader archive at:

```text
C:\Users\komputer\AppData\Roaming\.minecraft\ryo-blocks\shaderpacks\Ryo-Vanilla-Tint.zip
```

`config\iris.properties` selects that archive and enables shaders on startup. The shader samples Minecraft's native block atlas plus one shared 512px Ryo texture, avoiding per-block 512px atlas allocation. The installed Ryo mod jar should therefore be small; it supplies the resource pack, item/HUD art, skin overrides, and entity assets, while terrain tinting happens in Iris.

The prior high-memory baked-texture build is retained as a non-loadable rollback in the mods directory:

```text
ryo-blocks-1.0.0.jar.pre-iris-shared-texture-tint
```

The source snapshot includes the remote Codex transcript here:

```text
chatlog\rollout-2026-07-07T16-13-01-019f3c7e-f4f2-7c43-a7d1-676b204b3bd8.jsonl
```

Local build verification succeeded with JDK 17:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.7.7-hotspot"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --no-daemon build
```

Do not use the machine-wide `JAVA_HOME` without overriding it for this project; it currently points at JDK 8, which is too old for Fabric Loom.

## NVIDIA Native Crash

On 2026-07-12, a gameplay session terminated outside Java in `nvapi64.dll` / `nvoglv64.dll` while the render thread was inside `GLFW.glfwSwapBuffers`. The fatal dump was written to `ryo-blocks/hs_err_pid19004.log`; the process peaked at 5.6 GB working set and the Ryo block atlas was `16384x8192`. There was no Minecraft crash report, Java exception, mixin failure, or world-save error.

ImmediatelyFast was conservatively disabled because its fast buffer upload and universal batching hooks were active in the affected render path. Its jar is preserved in the profile mods folder as:

```text
ImmediatelyFast-Fabric-1.5.5+1.20.4.jar.disabled-after-nvidia-crash
```

Keep Sodium, Lithium, FerriteCore, and EntityCulling active. Do not re-enable ImmediatelyFast unless a later controlled test proves the NVIDIA native crash is unrelated.
