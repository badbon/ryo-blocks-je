# Kita "Aura" Ikuyo Angel Boss Visual Proof - 2026-09-08

Repository: `/Users/bolko/Dev/ryo-blocks-je`

Proof command:

```sh
JAVA_HOME=/Users/bolko/.vscode/extensions/redhat.java-1.31.0-darwin-arm64/jre/17.0.11-macosx-aarch64 sh gradlew --no-daemon runClient -PbossVisualProof
```

Result: pass. The hidden client proof created/staged `KitaAngelBossProof`, gave the player `ryo-blocks:kita_angel_boss_spawn_egg`, spawned `ryo-blocks:kita_angel_boss` as `Kita "Aura" Ikuyo` beside an untouched `minecraft:wither`, captured front/side/back framebuffer screenshots, and shut down cleanly.

Accepted captures:

- `docs/proof/kita-aura-ikuyo-front-entity-separation-20260908-212450.png`
- `docs/proof/kita-aura-ikuyo-side-wing-attachment-20260908-212458.png`
- `docs/proof/kita-aura-ikuyo-back-wing-attachment-20260908-212506.png`

Visual check:

- The custom boss is a separate entity from vanilla Wither in the front capture.
- Kita has a human silhouette with normal legs, not a dangling Wither-like lower taper.
- The wings attach from the upper back/shoulder area, not from the head.
- Left and right wings mirror correctly and read as layered feather structures rather than flat abstract quads.
- Client-side Wither smoke particles are suppressed for Kita only; the separate vanilla Wither still emits normal particles in the comparison capture.
