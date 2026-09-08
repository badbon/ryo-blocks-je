# Hidden Visual Proof

Player-visible terrain work can be captured through Minecraft's own framebuffer without opening a visible game window. The lane is opt-in and modifies only a disposable copied world under the ignored `run/` directory.

1. Place a disposable Minecraft 1.20.1 world at `run/saves/KitaLavaProof`.
2. Run with the documented JDK 17:

```powershell
.\gradlew.bat --no-daemon runClient -PvisualProof --args="--quickPlaySingleplayer KitaLavaProof"
```

The `visualProof` property hides the GLFW window before creation. Once the integrated world is ready, the client-only proof hook stages a lava pool and lavafall, waits for chunks and fluid propagation, writes a uniquely named native framebuffer image under `run/screenshots/`, and exits. The hook is gated by the `ryoBlocks.visualProof` system property and never runs during ordinary play.

For the Kita Angel boss proof, run:

```powershell
.\gradlew.bat --no-daemon runClient -PbossVisualProof
```

The boss proof uses the same hidden-window route, creates the disposable `KitaAngelBossProof` world when needed, disables clouds, stages a quartz platform, gives the player `ryo-blocks:kita_angel_boss_spawn_egg`, summons the custom `ryo-blocks:kita_angel_boss` beside an untouched `minecraft:wither`, captures front/side/back `run/screenshots/kita-angel-boss-proof-*.png`, and exits. The side-by-side frame is the required evidence that the custom boss is a unique entity and the vanilla Wither renderer has not been replaced.
