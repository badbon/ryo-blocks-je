# Kita obstacle-clearing bursts

## Behavior

Server-side `KitaAngelBossEntity.move` compares requested SELF movement with actual collision-limited movement on each horizontal axis and upward movement. It probes actual block collision shapes at the blocked side of the body, not tree tags or a search for nearby scenery. Ordinary downward ground contact is excluded.

After 20 consecutive blocked movement steps, Kita fires three charged feather projectiles: one at the blocking block and two at adjacent solid blocks, falling back to the main block when there are no eligible neighbors. Bursts have a 30-tick cooldown. There is no attempt limit; the obstruction is reevaluated each time, and the loop stops when movement resumes. Existing pursuit, slow hover, normal attacks, and feather visuals remain.

The shots use real vanilla charged Wither-skull collision and explosion rules. `mobGriefing=false`, Wither-immune blocks, disabled AI, and the spawn invulnerability phase prevent clearing bursts. The diagnostic entity tag `ryo_blocks_obstacle_clearing` distinguishes clearing shots from vanilla's occasional charged attack/idle shots.

## Runtime Verification

Java 17, `gradlew.bat --no-daemon build runClient -PbossAbilityProof -PbossObstacleProof`, with `ALSOFT_DRIVERS=null` as an additional launch precaution. The client window is hidden. The new proof-only SoundSystem mixin skips sound-engine startup before any speaker device is initialized; ordinary game launches are unaffected. Final logs confirmed the guard and contained no OpenAL initialization or sound-engine-start message. This replaces reliance on changing volume options after startup.

Each case used an actual spawn-egg boss with normal AI and an AI-disabled, 1000-health target on a disposable platform. No blocks were removed by the harness after staging a case. All destruction was from projectiles.

| Case | Clearing shots | Observed result |
| --- | ---: | --- |
| Two-layer log ceiling; target directly below | 3 | Broke an opening and rose above Y=184 |
| Five-block-thick stone wall | 18 | Six bursts; crossed the wall and resumed pursuit |
| Open space | 0 | Approached target without clearing bursts |
| Bedrock wall | 0 | No clearing bursts; barrier respected |
| Stone wall, mobGriefing=false | 0 | No clearing bursts; rule respected |

Final result: 5/5 checks passed. In the wall case, X advanced from 2.55 at tick 20 to 12.91 at tick 220, beyond the wall ending at X=8. The clearing-shot count stayed at 18 for the rest of the 320-tick case. Full build and packaged common-entrypoint, module, and refmap checks passed.

## Media

`kita-obstacle-clearing-20260915.mp4`: original framebuffer recording, 1280x720, 20 fps, 620 frames, 31 seconds, H.264, no audio track. First half is the ceiling test; second half is the thick wall. Each case omits the first ten setup ticks, then plays at normal speed. The opaque wall obscures part of the passage; server-position measurements above verify that she crossed it. Decoded frames across both segments were inspected before delivery.
