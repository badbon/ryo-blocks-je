# Pa-san Enderman

Ryo Blocks replaces only the client renderer for `minecraft:enderman`.
Spawning, AI, targeting, teleporting, block carrying, combat, sounds, drops,
hitboxes, networking and saved data remain vanilla.

The mod registers no custom entity or server entrypoint. A Ryo client can join
vanilla or Fabric servers that do not have Ryo Blocks installed. Installing the
JAR on a dedicated server is unnecessary; Fabric Loader skips it because the
mod is declared client-only.
