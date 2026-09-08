# Kita Angel Boss Visual Proof - 2026-09-08

Repository: `/Users/bolko/Dev/ryo-blocks-je`

Proof command:

```sh
JAVA_HOME=/Users/bolko/.vscode/extensions/redhat.java-1.31.0-darwin-arm64/jre/17.0.11-macosx-aarch64 sh gradlew --no-daemon runClient -PbossVisualProof
```

Result: pass. The hidden client proof created/staged `KitaAngelBossProof`, gave the player `ryo-blocks:kita_angel_boss_spawn_egg`, spawned `ryo-blocks:kita_angel_boss` beside an untouched `minecraft:wither`, captured front/side/back framebuffer screenshots, and shut down cleanly.

Accepted captures:

- `docs/proof/kita-angel-boss-front-entity-separation-20260908-204146.png`
- `docs/proof/kita-angel-boss-side-wing-attachment-20260908-204154.png`
- `docs/proof/kita-angel-boss-back-wing-attachment-20260908-204202.png`

Visual check:

- The custom boss is a separate entity from vanilla Wither in the front capture.
- The wings attach from the upper back/shoulder area, not from the head.
- Left and right wings mirror correctly and read as layered feather structures rather than flat abstract quads.
