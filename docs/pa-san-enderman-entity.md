# Pa-san Enderman

`ryo-blocks:pa_san_enderman` is a direct `EndermanEntity` subclass. It keeps
vanilla AI, targeting, teleporting, block carrying, combat, sounds, drops, and
NBT behavior without reimplementing any of those systems. It also registers the
same attributes and dark ground-spawn restriction as `minecraft:enderman`, and
uses the vanilla Enderman loot table.

At biome load time, every natural `minecraft:enderman` spawn entry is replaced
with the Pa-san type using the original entry's weight, group sizes, and spawn
cost. This keeps normal spawning behavior intact across the Overworld, Nether,
and End.

Existing Endermen and explicit `minecraft:enderman` summons are not rewritten,
which keeps old saves safe. They still use the Pa-san renderer. New Pa-san
entities can be summoned with `/summon ryo-blocks:pa_san_enderman`.

Because this adds a real entity type, multiplayer servers and their clients
must both install the same Ryo Blocks version.
