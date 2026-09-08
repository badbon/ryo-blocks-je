# Kita flight and impact verification

## Changes

- Visible mirrored wing sweeps: 4.83-second cycle, shoulder roll +/-16 degrees and yaw +/-17 degrees, with delayed outer-feather follow-through. Existing geometry, face texture, and golden halo retained.
- Decorative hover and leg sway slowed to a 12.57-second cycle (hover previously 3.93 seconds).
- Actual combat altitude now approaches the existing Wither target height gently. Vanilla's abrupt lift threshold produced recurring height changes from roughly 184.92 to 185.42 over a stationary target at Y=180. Kita's vertical speed now tapers near the destination, capped at 0.15 blocks/tick ascending and 0.10 descending. Horizontal pursuit and firing remain inherited.
- The half-health target altitude remains the target's ground height, rather than five blocks above it. Idle, invulnerability, and fluid movement remain on the inherited path.
- No change to projectile explosions: they already execute vanilla `WitherSkullEntity.onCollision`, with power 1, MOB source, charged resistance reduction, and game-rule protection.

## Impact Checks

Hidden, speaker-muted integrated client. Paired Kita-owned feather and vanilla-owned skull shots against fresh 42-block walls. Shots were manually staged, but collision, explosion, and block destruction used real server behavior. Neither owner had AI enabled for these isolated checks. Random explosion shapes can differ.

| Scenario | Kita blocks removed | Vanilla blocks removed | Result |
| --- | ---: | ---: | --- |
| Normal shot, dirt | 8 | 9 | Pass |
| Charged shot, stone | 9 | 9 | Pass |
| Normal shot, stone | 0 | 0 | Pass |
| Normal shot, dirt, mobGriefing=false | 0 | 0 | Pass |
| Charged shot, bedrock | 0 | 0 | Pass |

All ten projectiles collided and were removed. The old stone combat floor did not demonstrate normal-shot destruction because stone resists those shots, for both owners.

Command: Java 17, `gradlew.bat --no-daemon runClient -PbossAbilityProof -PbossBlockImpactProof`.

Original screenshots: `kita-dirt-impact-before-20260909.png`, `kita-dirt-impact-after-20260909.png`, `kita-charged-stone-impact-20260909.png`.

## Combat Video

`kita-slow-flight-combat-20260909.mp4`: 15 seconds, 1280x720, 20 fps, silent, 300 consecutive client-tick framebuffer captures. Normal boss AI against an AI-disabled 1000-health golem on the disposable test platform. No generated frames or gameplay speed changes. The recording covers full-health combat; the proof continues afterward to check the half-health descent separately.

Command: Java 17, `gradlew.bat --no-daemon build runClient -PbossAbilityProof -PbossCombatVideo`.

Final run passed the full build and package/common-entrypoint/refmap checks. Live hover check at tick 340: altitude error 0.000013 blocks. After setting health to 120 at tick 360, the half-health descent check at tick 500 had error 0.0071 blocks. Both passed the 0.1-block tolerance. Normal attacks continued and the target retained the Wither effect.

The encoded video was decoded for inspection across all 15 seconds: wing strokes are distinct, the body settles after its initial ascent, and impacts remain visible. FFprobe confirmed 300 frames, exactly 15 seconds, H.264, and no audio stream.
