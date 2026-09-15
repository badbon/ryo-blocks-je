# Kita 50-percent faster firing

Requested firing delays are divided by 1.5 and rounded to the nearest Minecraft tick, not halved. All timing values are centralized in `KitaAttackTiming`. Kita replaces only her main projectile goal and rescales newly scheduled secondary cooldowns once after inherited AI execution. Ordinary Wither timing, damage, projectile speed, flight animation, and spawn-charge duration remain unchanged.

| Case | Before | After |
| --- | --- | --- |
| Main attack | 40 ticks / 2 s | 27 ticks / 1.35 s |
| Each secondary stream | 40-59 ticks / 2-2.95 s | 27-39 ticks / 1.35-1.95 s |
| Idle secondary scheduling checks | 10-19 ticks | 7-13 ticks |
| Initial sustained-blockage wait | 20 ticks / 1 s | 13 ticks / 0.65 s |
| Repeating three-shot clearing burst | 30 ticks / 1.5 s | 20 ticks / 1 s |
| Within a clearing burst | 3 simultaneous shots | Unchanged |

The idle charged-shot counter is unchanged: without damage or successful attacks resetting/accelerating it, each recurring charged shot requires 17 idle checks. Rounded check delays yield a theoretical recurring range of 119-221 ticks (5.95-11.05 seconds), with a mean around 8.25 seconds. This differs slightly from simply dividing the old overall interval because each individual check is rounded to a whole tick. Normal/Hard difficulty gates and damage-driven acceleration remain inherited.

## Validation

`gradlew.bat --no-daemon build runClient -PbossAbilityProof -PbossCadenceProof`, Java 17. Hidden client with sound-engine initialization disabled and `ALSOFT_DRIVERS=null`. Normal AI fights a stationary high-health golem; the Kita case changes health from 300 to 120 halfway through. A separate Kita case has no target. The third case uses an ordinary Wither.

Recorded at the real vanilla `shootSkullAt` entry point, keyed by entity, head, and entity age. The probe is inactive outside the opt-in cadence proof.

| Observed case | Shots | Consecutive intervals |
| --- | ---: | --- |
| Kita main, including full/low health | 18 (8 full, 10 low) | Exactly 27 ticks |
| Kita secondary stream 1 | 14 | 29-39 ticks |
| Kita secondary stream 2 | 15 | 27-39 ticks |
| Kita idle charged stream 1 | 3 | 147-163 ticks |
| Kita idle charged stream 2 | 3 | 164-174 ticks |
| Vanilla Wither main | 12 | Exactly 40 ticks |
| Vanilla Wither secondary stream 1 | 11 | 40-58 ticks |
| Vanilla Wither secondary stream 2 | 11 | 40-58 ticks |

All observed intervals passed their expected bounds. `verifyKitaAttackTiming` passes exact constant checks and nearest-tick rounding for input delays 1 through 323. Full build, common-entrypoint, module, and refmap checks passed. The new common accessor is packaged with `skullCooldowns` mapped to `field_7091:[I` for the installed runtime.

The existing `-PbossObstacleProof` regression passed all five cases with the new timings: ceiling escape, thick-wall escape, open space, bedrock protection, and mobGriefing protection. The ceiling required three clearing shots; the wall required 21 and she was past it by tick 160. No further clearing shots were recorded after escape. The new constants retain simultaneous three-shot bursts and all protection checks.

`kita-faster-clearing-20260915.mp4` is the matching hidden-client framebuffer recording: 620 frames at 20 fps, 31 seconds, 1280x720 H.264, silent. Ceiling then wall, with ten setup ticks omitted from each case. Decoded frames across both cases were inspected. The opaque wall hides part of the passage; server position verifies crossing. Audio-engine startup remained disabled during both proof runs.
