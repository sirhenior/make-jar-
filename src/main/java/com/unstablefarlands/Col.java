package com.unstablefarlands;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import java.util.Arrays;

/** One vertical column of blocks that a zone fills in. */
public final class Col {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    public final int minY;
    public final BlockState[] s;

    public Col(int minY, int height) {
        this.minY = minY;
        this.s = new BlockState[height];
        clear();
    }

    public void clear() { Arrays.fill(s, AIR); }
    public int maxY() { return minY + s.length - 1; }

    public void fill(int y0, int y1, BlockState b) {
        y0 = Math.max(y0, minY);
        y1 = Math.min(y1, maxY());
        for (int y = y0; y <= y1; y++) s[y - minY] = b;
    }

    public void set(int y, BlockState b) {
        if (y >= minY && y <= maxY()) s[y - minY] = b;
    }

    /** Highest non-air Y, or minY-1 if the column is empty (void). */
    public int top() {
        for (int i = s.length - 1; i >= 0; i--) if (!s[i].isAir()) return minY + i;
        return minY - 1;
    }
}
