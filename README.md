# Unstable (Fabric 1.21.11) – v0.4, UNTESTED

Adds Unstable-SMP-style far-lands zones along +Z in the Overworld. Existing chunks are untouched; only
newly generated chunks past `startZ` are rewritten, so use a NEW world (or explore fresh chunks).

## Build the jar with GitHub (no installs)
1. Make a free GitHub account, click New repository (name it anything), create it.
2. Click "uploading an existing file", unzip `unstable.zip`, and drag in EVERYTHING inside it (including the hidden `.github` folder).
   If the .github folder won't upload by drag-drop, use "Add file > Create new file", name it `.github/workflows/build.yml`, and paste the file's text.
3. Open the Actions tab. The "Build jar" workflow runs on its own (or press Run workflow). When it's green, open the run and download the
   `unstable-jar` artifact. Unzip it: the jar without `-sources` goes in your `mods` folder next to Fabric API (0.141.1+1.21.11).
4. If it fails (red X), open the failed step, copy the error text, and send it to me.

## Build locally instead
Easiest and safest:
1. Go to https://fabricmc.net/develop, pick Minecraft 1.21.11, download the template, unzip it.
2. Copy `src/main/java/com/unstablefarlands/` from this project into the template's `src/main/java/`.
3. Copy this project's `fabric.mod.json` over the template's (or just fix its entrypoint to `com.unstablefarlands.UnstableFarlands`).
4. `./gradlew build` -> jar is in `build/libs/`. Drop it in `mods/` with Fabric API.

The built jar is named `unstable-<version>.jar`.
(Or use this project's build files directly after fixing the version numbers in gradle.properties.)

## Use
- `/unstablefarlands list`        – zone order
- `/unstablefarlands tp <zone>`   – teleport to a zone (anyone can run it; add a permissions mod if that's a problem)
- Config: `config/unstable_farlands.json` -> `startZ`, `zoneLength`, `order` (reorder/remove zones)

Default: zones start at z=12,550,821 and are 50,000 blocks long each, in this order:
red_plains, wavy_lands, dark_forest, fabric, skyscrapers, rainbow_forest, ravager_desert,
mythical_forest, decayed_labyrinth, natural_maze.
(Stripelands was removed: it's the Wavy Lands.)

## Lines most likely to need a tweak (Minecraft changes names often)
- `UnstableFarlands.java`: `p.getEntityWorld()` (older name: `getWorld()`).
- `ServerChunkEvents.CHUNK_GENERATE` (Fabric API) – if missing, update Fabric API.
- `Heightmap.populateHeightmaps(...)` signature.
- `ChunkSection.setBlockState(x,y,z,state,false)` and `chunk.getSection(i)`.
- Compile errors on block names: Yarn names, e.g. `Blocks.CUT_RED_SANDSTONE`.

## Known limits
- Biomes aren't changed (only blocks), so sky/grass colour stay as vanilla; lighting inside rewritten chunks may look off until re-lit.
- Dark Forest / Fabric darkness is the Darkness status effect (no hidden nametags).
- Not built: Beanstalk, floating mineshaft, ravagers, hand-built structures (Forge, Temple, Civilization).

## Mazes (new in v0.3)
Both mazes use an infinite, always-solvable layout (binary-tree algorithm, 4-block cells, 3-wide corridors), the same
for every chunk and every seed-consistent.
- `decayed_labyrinth`: mossy cobblestone/stone-brick walls topped with sculk.
- `natural_maze`: overgrown version, grass/moss floor with flowers and ferns, hedge walls (oak/azalea leaves) or
  ruined mossy stone, oak-log corner pillars. Walls are 6 high with open sky.
Note: an old `config/unstable_farlands.json` won't list the new zone. Delete it (it regenerates) or add "natural_maze" to "order".

## Underworld mazes (new in v0.4)
About 15-20% of the Underworld land is a massive maze: 24-block cells, 4-thick walls, 60 blocks tall, built from random mixes of
blackstone, deepslate bricks/tiles, polished blackstone bricks and cracked/cobbled deepslate, with the odd green froglight.
Same always-solvable maze algorithm as the overworld ones (corridors are 20 wide).

## The Underworld (new in v0.2)
A separate dimension `unstable_farlands:underworld` (data/unstable_farlands/dimension/underworld.json). It uses the
Nether dimension type (Y 0-255, no skylight, permanently dark) and the Soul Sand Valley biome for the teal fog.
Getting there:
- `/unstablefarlands underworld`            – standard land at spawn
- `/unstablefarlands underworld fracture`   – where the land breaks into void
- `/unstablefarlands underworld void`       – first void castle
- Fall below Y=-20 in the Nether and you drop into the Underworld at the same X/Z.

Contents: soul sand/soul soil/blackstone terrain over deepslate, deep chasms, green crystal spires, lantern posts,
Fire areas (lava lakes, red crystals), Snow areas (massive spruces), fracturing floating islands, then pure void
with gothic deepslate castles linked by glass bridges (Darkness effect once you're past the fracture point).
Config keys: `underworldFractureStart` (default 6000), `underworldFractureLength` (default 2000).

Not in: Citadel library, graveyard, wither castle, the 20-portal room, Soul Keepers' no-build rule, escape-room
puzzles inside castles, or the custom sky colour. Soul Sand Valley mobs (skeletons, ghasts, endermen, striders) can spawn.
Extra files to check if it fails to compile: `Warp.java` (teleport API changes a lot).
If the dimension doesn't show up, create a NEW world with the mod installed.
