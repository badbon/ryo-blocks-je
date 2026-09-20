# Angel Boss Variants

All three are separately registered, separately spawnable bosses. Their existing entity IDs are stable save identities.

| Boss | Entity ID | Projectile |
| --- | --- | --- |
| Ikuyo Kita | `ryo-blocks:kita_angel_boss` | Existing golden feather |
| Nijika Ijichi | `ryo-blocks:nijika_angel_boss` | Single orange triangular chip, authored at 16x16 |
| Kikuri Hiroi | `ryo-blocks:kikuri_angel_boss` | Vanilla splash potion |

Each has a corresponding `<entity_id>_spawn_egg` in the Spawn Eggs creative tab. Commands also work, e.g. `/summon ryo-blocks:nijika_angel_boss ~ ~ ~`.

## Shared Behavior

`AngelBossEntity` owns the existing Kita AI: 300 health, regeneration, Wither targeting, half-health behavior, slow vertical approach, main firing every 27 ticks, secondary firing every 27-39 ticks, and accelerated idle checks. Obstacle clearing starts after 13 blocked movement ticks, fires three projectiles, and repeats every 20 ticks while blocked. Protected blocks and `mobGriefing=false` remain respected. Drops and underlying boss abilities are unchanged.

All use the existing animated cuboid white wings (Minecraft snow texture, no Ryo imagery), gold halo, slow visual hover, and matching slim-arm player model. Nijika uses the villager skin directly. Kikuri uses the user's exact NameMC PNG. Kita's skin is unchanged. Default display names use full names without nicknames; existing custom-named entities retain their saved custom names.

## Projectile Differences

- Nijika's spinning chip uses a vanilla generated item model with pixel-thick edges. Its underlying skull physics, damage, Wither status, charged shots and explosions match Kita. The visible projectile is a chip, not a skull or feather. Its trail uses gold particles.
  The model inherits Minecraft's normal item display transforms. Flight uses the vanilla thrown-item scale and camera orientation; charged chips are 1.35 times larger. Inventory, held and dropped forms keep vanilla transforms too.
- Kikuri's normal combat uses actual `PotionEntity`, vanilla gravity, Witch throw speed/spread/sound, and offensive potion selection: slowness at long range, poison on healthy unaffected targets, occasional close-range weakness, otherwise harming. It does not add Witch drinking or raid AI. Idle charged-shot opportunities throw harming potions. Normal combat bottles do not explode or break terrain.
- Only Kikuri's obstacle-clearing bottles use `KikuriClearingPotionEntity`: direct flight toward the obstructing blocks, vanilla splash effects plus a radius-1 MOB explosion, with charged-Wither block-resistance limits. This preserves escape behavior without making every combat potion explosive. They are not vanilla Witch projectiles in this special case.
- Ordinary Withers and Witches are not reconfigured. Kita still takes the original skull/feather path.

## Extension Points

Each boss has its own entity subclass and registration. Override `shootVariantProjectile` for distinct normal/secondary/idle projectiles and `shootObstacleProjectile` for obstacle-clearing attacks. The common mixin routes the private Wither firing method through that hook without replacing its target-selection or cooldown scheduling. Rendering accepts a per-boss skin and arm layout; the wings and halo remain shared.

`tools/generate_dorito.py` is the sprite source. `verifyAngelVariantAssets` checks packaged skins, slim-arm layout, chip dimensions, spawn-egg models, names and projectile mixin mapping. `-PbossAbilityProof -PbossVariantsProof` runs the opt-in hidden, speaker-disabled staging/observation suite; never use it with an actual player world.

## Visual Conventions Audit

References checked on 2026-09-20:

- [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/): readable simple geometry, restrained pixel palettes, intentional edges and recognizable proportions.
- [Fabric item guide](https://docs.fabricmc.net/develop/items/first-item): 16x16 item texture and generated item model. This current guide targets newer Minecraft; version-specific API and model behavior were checked against the local 1.20.1 sources instead of copying newer registration code.
- [Minecraft texture artist interview](https://www.minecraft.net/en-us/article/try-new-minecraft-textures): visual consistency and avoiding excessive smoothing at Minecraft's low resolution.

The chip is native 16x16 RGBA with five opaque colors and fully transparent empty pixels, not a downsampled illustration. The standard item model provides its edge thickness and item-display transforms. Both skins remain byte-for-byte unchanged; the boss enlargement is intentional. The requested shared wing silhouette and animation are preserved, rather than redesigning the previously accepted wings. Kikuri's potion uses the vanilla model, tint and splash renderer.

Build checks reject partial-alpha chip edges, oversized palettes and custom item-display overrides. The proof suite includes front, three-quarter, side and back views of both bosses, projectile close-ups, and live combat/obstacle checks. These are project art decisions informed by the guides, not a claim of Mojang certification or exhaustive shader-pack compatibility.
