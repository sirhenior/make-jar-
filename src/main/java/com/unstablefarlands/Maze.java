package com.unstablefarlands;

/**
 * Infinite, always-solvable maze (binary-tree algorithm): every cell opens either east (+X) or
 * north (+Z), decided by a hash, so there is no global state and every chunk agrees on the layout.
 * A cell is `cell` blocks wide; its first `thick` rows/columns are its wall lines, the rest is corridor.
 */
public final class Maze {
    private Maze() {}

    private static boolean east(Noise n, int cx, int cz, int salt) {
        return n.hash(cx, cz, salt) < 0.5;
    }

    /** Small maze: 4-block cells, 1-block walls, 3-wide corridors. */
    public static boolean wall(Noise n, int x, int z, int salt) {
        return wall(n, x, z, salt, 4, 1);
    }

    public static boolean wall(Noise n, int x, int z, int salt, int cell, int thick) {
        int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
        boolean wx = Math.floorMod(x, cell) < thick;
        boolean wz = Math.floorMod(z, cell) < thick;
        if (wx && wz) return true;                       // corner pillar
        if (wx) return !east(n, cx - 1, cz, salt);       // wall between west neighbour and this cell
        if (wz) return east(n, cx, cz - 1, salt);        // wall between south neighbour and this cell
        return false;
    }

    public static boolean pillar(int x, int z) {
        return Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0;
    }
}
