package com.unstablefarlands;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

/** Lets zones place blocks inside one chunk. Anything outside the chunk is silently clipped. */
public final class Deco {
    public final int x0, z0;
    public final Noise n;
    private final Chunk chunk;
    private final Zone zone;

    public Deco(Chunk chunk, Zone zone, Noise n) {
        this.chunk = chunk;
        this.zone = zone;
        this.n = n;
        this.x0 = chunk.getPos().getStartX();
        this.z0 = chunk.getPos().getStartZ();
    }

    /** Surface Y for any world column, computed from the zone (works outside this chunk). */
    public int surface(int wx, int wz) {
        Col c = new Col(chunk.getBottomY(), chunk.getHeight());
        zone.column(wx, wz, c, n);
        return c.top();
    }

    /** Places the block only where there is currently air. */
    public void set(int wx, int y, int wz, BlockState state) {
        int lx = wx - x0, lz = wz - z0;
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) return;
        if (y < chunk.getBottomY() || y >= chunk.getBottomY() + chunk.getHeight()) return;
        ChunkSection sec = chunk.getSection(chunk.getSectionIndex(y));
        if (sec.getBlockState(lx, y & 15, lz).isAir()) {
            sec.setBlockState(lx, y & 15, lz, state, false);
        }
    }
}
