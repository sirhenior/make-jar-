package com.unstablefarlands;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Rewrites freshly generated chunks. */
public final class ZoneManager {
    private ZoneManager() {}

    private static long cachedSeed;
    private static Noise cachedNoise;

    public static synchronized Noise noise(long seed) {
        if (cachedNoise == null || cachedSeed != seed) {
            cachedSeed = seed;
            cachedNoise = new Noise(seed);
        }
        return cachedNoise;
    }

    /** Overworld: only chunks that lie in a +Z zone. */
    public static void apply(ServerWorld world, WorldChunk chunk) {
        int z0 = chunk.getPos().getStartZ();
        if (Config.index(z0) < 0 && Config.index(z0 + 15) < 0) return;
        rewrite(world, chunk, null);
    }

    /** Underworld: every chunk. */
    public static void applyWhole(ServerWorld world, WorldChunk chunk, Zone zone) {
        rewrite(world, chunk, zone);
    }

    private static void rewrite(ServerWorld world, WorldChunk chunk, Zone fixed) {
        int x0 = chunk.getPos().getStartX(), z0 = chunk.getPos().getStartZ();
        Noise n = noise(world.getSeed());
        int minY = chunk.getBottomY();
        Col col = new Col(minY, chunk.getHeight());
        int sections = chunk.countVerticalSections();
        boolean any = false;
        List<BlockPos> lights = new ArrayList<>();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                Zone z = fixed != null ? fixed : Config.zoneAt(z0 + lz);
                if (z == null) continue;
                col.clear();
                z.column(x0 + lx, z0 + lz, col, n);
                for (int si = 0; si < sections; si++) {
                    ChunkSection sec = chunk.getSection(si);
                    for (int y = 0; y < 16; y++) {
                        BlockState st = col.s[si * 16 + y];
                        sec.setBlockState(lx, y, lz, st, false);
                        if (st.getLuminance() > 0) lights.add(new BlockPos(x0 + lx, minY + si * 16 + y, z0 + lz));
                    }
                }
                any = true;
            }
        }

        Zone mid = fixed != null ? fixed : Config.zoneAt(z0 + 8);
        if (mid != null) mid.decorate(new Deco(chunk, mid, n));

        if (any) {
            Heightmap.populateHeightmaps(chunk, EnumSet.of(
                    Heightmap.Type.WORLD_SURFACE, Heightmap.Type.OCEAN_FLOOR,
                    Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES));
            // Ask the light engine to re-check glowing blocks we placed after lighting ran.
            try {
                var lighting = world.getChunkManager().getLightingProvider();
                for (BlockPos p : lights) lighting.checkBlock(p);
            } catch (RuntimeException e) {
                UnstableFarlands.LOG.debug("Light update skipped", e);
            }
        }
    }
}
