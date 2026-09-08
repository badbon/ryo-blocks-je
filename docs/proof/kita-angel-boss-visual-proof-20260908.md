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
- `docs/proof/kita-aura-ikuyo-front-attached-texture-20260908-215923.png`
- `docs/proof/kita-aura-ikuyo-side-attached-texture-20260908-215931.png`
- `docs/proof/kita-aura-ikuyo-back-attached-texture-20260908-215939.png`
- `docs/proof/kita-aura-ikuyo-front-white-wings-20260908-221056.png`
- `docs/proof/kita-aura-ikuyo-side-white-wings-20260908-221104.png`
- `docs/proof/kita-aura-ikuyo-back-white-wings-20260908-221112.png`
- `docs/proof/kita-aura-ikuyo-front-sleeve-sync-20260908-221410.png`
- `docs/proof/kita-aura-ikuyo-side-sleeve-sync-20260908-221418.png`
- `docs/proof/kita-aura-ikuyo-back-sleeve-sync-20260908-221426.png`

Visual check:

- `source/kita-angel-player-skin.png` and `assets/ryo-blocks/textures/entity/player/kita_angel.png` match the attached `Kita_Ikuyo.png` source file with SHA-256 `B331ED363C698CBDD65CCCC72396AAECB98F175AEB62967C239ECD45AB44EC6B`.
- Kita's boss body uses Minecraft's slim `PlayerEntityModel` so the supplied 64x64 player skin maps through the standard player UVs.
- The halo is rendered with a dedicated gold texture as a flat ring above the head.
- The wing renderer draws the full transparent wing texture as broad angled planes rather than repeated cuboid strips.
- The wing texture uses ivory feather fill, soft inner feather lines, and a gold rim so it reads as angel wings instead of orange skeletal fins.
- The proof client mutes all sound categories before staging the world.
- The final proof captures do not include the comparison `minecraft:wither`.
- Kita has a human silhouette with normal legs, not a dangling Wither-like lower taper.
- The wings attach from the upper back/shoulder area, not from the head.
- Left and right wings mirror correctly and read as layered feather structures rather than flat abstract quads.
- The sleeve and pants overlay layers copy the final posed arm/leg transforms, so no separate leftover overlay floats in front of the base limbs.
- Client-side Wither smoke particles are suppressed for Kita.
