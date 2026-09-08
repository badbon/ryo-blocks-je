# Hidden Visual Proof

Player-visible terrain work can be captured through Minecraft's own framebuffer without opening a visible game window. The lane is opt-in and modifies only a disposable copied world under the ignored `run/` directory.

1. Place a disposable Minecraft 1.20.1 world at `run/saves/KitaLavaProof`.
2. Run with the documented JDK 17:

```powershell
.\gradlew.bat --no-daemon runClient -PvisualProof --args="--quickPlaySingleplayer KitaLavaProof"
```

The `visualProof` property hides the GLFW window before creation. Once the integrated world is ready, the client-only proof hook stages a lava pool and lavafall, waits for chunks and fluid propagation, writes a uniquely named native framebuffer image under `run/screenshots/`, and exits. The hook is gated by the `ryoBlocks.visualProof` system property and never runs during ordinary play.

For the Kita Angel boss proof, place a disposable world at `run/saves/KitaAngelBossProof` and run:

```powershell
.\gradlew.bat --no-daemon runClient -PbossVisualProof --args="--quickPlaySingleplayer KitaAngelBossProof"
```

The boss proof uses the same hidden-window route, stages a quartz platform, summons a frozen vanilla Wither, captures `run/screenshots/kita-angel-boss-proof-*.png`, and exits.
