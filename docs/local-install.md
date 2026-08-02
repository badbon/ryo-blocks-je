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
ryo-blocks-1.1.0.jar
```

## Standalone Ryo Block Tint

The mod jar contains the Ryo terrain tint and the four Fabric API modules it uses. Install Fabric Loader for Minecraft 1.20.1, then place only `ryo-blocks-1.1.0.jar` in the profile's `mods` folder. Iris, Sodium, Indium, OptiFine, a separate Fabric API jar, and a separate shaderpack are not required for normal play.

Ryo block tinting is performed by Minecraft 1.20.1 core terrain shader overrides inside the always-enabled bundled resource pack. The shaders sample Minecraft's native block atlas plus one shared Ryo texture, avoiding per-block texture allocation while keeping the jar drop-in for normal Fabric installs.

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
