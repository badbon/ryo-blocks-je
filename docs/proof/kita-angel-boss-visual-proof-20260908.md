# Kita "Aura" Ikuyo Angel Boss Visual Proof - 2026-09-08

Repository: `D:\GitHub\ryo-blocks-je`

Proof command:

```sh
JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.6.10-hotspot" .\gradlew.bat --no-daemon runClient -PbossVisualProof
```

Result: pass. The hidden client proof created/staged `KitaAngelBossProof`, gave the player `ryo-blocks:kita_angel_boss_spawn_egg`, spawned only `ryo-blocks:kita_angel_boss` as `Kita "Aura" Ikuyo`, captured front/side/back framebuffer screenshots, and shut down cleanly.

Accepted captures:

- `docs/proof/kita-aura-ikuyo-front-clean-face-20260908-213253.png`
- `docs/proof/kita-aura-ikuyo-side-wing-attachment-20260908-213301.png`
- `docs/proof/kita-aura-ikuyo-back-wing-attachment-20260908-213309.png`
- `docs/proof/kita-aura-ikuyo-front-clean-face-gold-halo-20260908-215037.png`
- `docs/proof/kita-aura-ikuyo-side-gold-halo-20260908-215045.png`
- `docs/proof/kita-aura-ikuyo-back-gold-halo-20260908-215052.png`

Visual check:

- Kita's face uses the clean warm face pixels from the source skin; the white/olive zombie-looking face pixels are gone.
- The front head overlay cube is disabled for the boss model so the in-game face uses the source skin's base face without extra jumbled overlay geometry.
- The halo is rendered with a dedicated gold texture as a flat ring above the head.
- The final proof captures do not include the comparison `minecraft:wither`.
- Kita has a human silhouette with normal legs, not a dangling Wither-like lower taper.
- The wings attach from the upper back/shoulder area, not from the head.
- Left and right wings mirror correctly and read as layered feather structures rather than flat abstract quads.
- Client-side Wither smoke particles are suppressed for Kita.
